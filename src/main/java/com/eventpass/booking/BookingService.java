package com.eventpass.booking;

import com.eventpass.common.BusinessRuleException;
import com.eventpass.common.CacheConfig;
import com.eventpass.common.PageResponse;
import com.eventpass.common.ResourceNotFoundException;
import com.eventpass.common.SoldOutException;
import com.eventpass.event.Event;
import com.eventpass.event.EventRepository;
import com.eventpass.event.EventStatus;
import com.eventpass.event.TicketTier;
import com.eventpass.event.TicketTierRepository;
import com.eventpass.logging.Audited;
import com.eventpass.user.User;
import com.eventpass.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {

    static final int MAX_TICKETS_PER_EVENT = 6;
    static final long CANCEL_CUTOFF_HOURS = 24;

    private static final String REF_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int REF_RANDOM_LENGTH = 6;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final BookingRepository bookingRepository;
    private final TicketTierRepository ticketTierRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ObjectProvider<CacheManager> cacheManagerProvider;

    @Audited(action = "CREATE_BOOKING")
    @Transactional
    public BookingResponse createBooking(String userEmail, CreateBookingRequest request) {
        if (request.quantity() < 1) {
            throw new BusinessRuleException("Booking quantity must be at least 1");
        }

        // Lock the user's row first so concurrent requests from the same user cannot race past the 6-ticket cap
        User user = userRepository.findByEmailForUpdate(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        TicketTier tier = ticketTierRepository.findWithEventById(request.ticketTierId())
                .orElseThrow(() -> new ResourceNotFoundException("TicketTier", request.ticketTierId()));
        Event event = tier.getEvent();

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException("Tickets can only be booked for PUBLISHED events");
        }
        if (!event.getStartsAt().isAfter(Instant.now())) {
            throw new BusinessRuleException("Cannot book tickets for an event that has already started");
        }

        int alreadyBooked = bookingRepository.sumActiveTicketsForUserAndEvent(user.getId(), event.getId());
        if (alreadyBooked + request.quantity() > MAX_TICKETS_PER_EVENT) {
            throw new BusinessRuleException(
                    "Maximum " + MAX_TICKETS_PER_EVENT + " tickets allowed per event (already booked: " + alreadyBooked + ")"
            );
        }

        // Snapshot fields before reserveSeats() clears the persistence context
        Long userId = user.getId();
        Long eventId = event.getId();
        String eventTitle = event.getTitle();
        Instant eventStartsAt = event.getStartsAt();
        Long tierId = tier.getId();
        String tierName = tier.getName();
        BigDecimal unitPrice = tier.getPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));

        int updated = ticketTierRepository.reserveSeats(tierId, request.quantity());
        if (updated == 0) {
            throw new SoldOutException("Not enough seats available for tier: " + tierName);
        }

        Booking booking = new Booking();
        booking.setBookingRef(generateUniqueBookingRef());
        booking.setUser(userRepository.getReferenceById(userId));
        booking.setTicketTier(ticketTierRepository.getReferenceById(tierId));
        booking.setEvent(eventRepository.getReferenceById(eventId));
        booking.setQuantity(request.quantity());
        booking.setUnitPrice(unitPrice);
        booking.setTotalAmount(totalAmount);
        booking.setStatus(BookingStatus.CONFIRMED);

        Booking saved = bookingRepository.save(booking);
        evictEventCache(eventId);

        log.info("Created booking ref={} userId={} eventId={} tierId={} qty={}",
                saved.getBookingRef(), userId, eventId, tierId, saved.getQuantity());

        return new BookingResponse(
                saved.getBookingRef(),
                eventId,
                eventTitle,
                eventStartsAt,
                tierId,
                tierName,
                saved.getQuantity(),
                unitPrice,
                totalAmount,
                saved.getStatus(),
                saved.getCreatedAt(),
                saved.getCancelledAt(),
                saved.getCheckedInAt()
        );
    }

    public PageResponse<BookingResponse> listMyBookings(String userEmail, Pageable pageable) {
        User user = findUserByEmail(userEmail);
        Page<Booking> page = bookingRepository.findByUserId(user.getId(), pageable);
        return PageResponse.from(page, BookingResponse::from);
    }

    public BookingResponse getMyBooking(String userEmail, String bookingRef) {
        User user = findUserByEmail(userEmail);
        Booking booking = bookingRepository.findByBookingRefAndUserId(bookingRef, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingRef));
        return BookingResponse.from(booking);
    }

    @Audited(action = "CANCEL_BOOKING")
    @Transactional
    public BookingResponse cancelBooking(String userEmail, String bookingRef) {
        User user = findUserByEmail(userEmail);
        Booking booking = bookingRepository.findByBookingRefAndUserId(bookingRef, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingRef));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Booking " + bookingRef + " is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            throw new BusinessRuleException("Checked-in bookings cannot be cancelled");
        }

        Instant cancellationDeadline = booking.getEvent().getStartsAt().minus(CANCEL_CUTOFF_HOURS, ChronoUnit.HOURS);
        if (!Instant.now().isBefore(cancellationDeadline)) {
            throw new BusinessRuleException(
                    "Bookings can only be cancelled at least " + CANCEL_CUTOFF_HOURS + " hours before event start"
            );
        }

        Instant now = Instant.now();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);

        // Snapshot fields before releaseSeats() flushes this booking update and clears the persistence context
        Long eventId = booking.getEvent().getId();
        Long tierId = booking.getTicketTier().getId();
        int quantity = booking.getQuantity();
        BookingResponse response = BookingResponse.from(booking);

        ticketTierRepository.releaseSeats(tierId, quantity);
        evictEventCache(eventId);

        log.info("Cancelled booking ref={} userId={} releasedSeats={}", bookingRef, user.getId(), quantity);
        return response;
    }

    private User findUserByEmail(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));
    }

    private String generateUniqueBookingRef() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder ref = new StringBuilder("EVP-");
            for (int i = 0; i < REF_RANDOM_LENGTH; i++) {
                ref.append(REF_ALPHABET.charAt(SECURE_RANDOM.nextInt(REF_ALPHABET.length())));
            }
            String candidate = ref.toString();
            if (!bookingRepository.existsByBookingRef(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to generate a unique booking reference");
    }

    private void evictEventCache(Long eventId) {
        CacheManager cacheManager = cacheManagerProvider.getIfAvailable();
        if (cacheManager != null) {
            Cache cache = cacheManager.getCache(CacheConfig.EVENT_DETAILS_CACHE);
            if (cache != null) {
                cache.evict(eventId);
            }
        }
    }
}