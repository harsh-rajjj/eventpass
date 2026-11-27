package com.eventpass.booking;

import com.eventpass.TestContainersConfiguration;
import com.eventpass.common.BusinessRuleException;
import com.eventpass.common.JpaAuditingConfig;
import com.eventpass.common.SoldOutException;
import com.eventpass.event.Event;
import com.eventpass.event.EventCategory;
import com.eventpass.event.EventRepository;
import com.eventpass.event.EventStatus;
import com.eventpass.event.OrganizerEventService;
import com.eventpass.event.TicketTier;
import com.eventpass.event.TicketTierRepository;
import com.eventpass.organizer.Organizer;
import com.eventpass.organizer.OrganizerRepository;
import com.eventpass.organizer.OrganizerService;
import com.eventpass.organizer.OrganizerStatus;
import com.eventpass.user.Role;
import com.eventpass.user.User;
import com.eventpass.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({
        TestContainersConfiguration.class,
        JpaAuditingConfig.class,
        BookingService.class,
        OrganizerService.class,
        OrganizerEventService.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BookingServiceTest {

    private static final String EMAIL_DOMAIN = "@booking.test";
    private static final String ORGANIZER_PREFIX = "BookingTest ";

    @Autowired
    private BookingService bookingService;

    @Autowired
    private OrganizerEventService organizerEventService;

    @Autowired
    private OrganizerRepository organizerRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketTierRepository ticketTierRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update(
                "DELETE FROM bookings WHERE user_id IN (SELECT id FROM users WHERE email LIKE ?)",
                "%" + EMAIL_DOMAIN);
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE ?", "%" + EMAIL_DOMAIN);
        jdbcTemplate.update(
                "DELETE FROM events WHERE organizer_id IN (SELECT id FROM organizers WHERE name LIKE ?)",
                ORGANIZER_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM organizers WHERE name LIKE ?", ORGANIZER_PREFIX + "%");
    }

    @Test
    void createBooking_with50ConcurrentBuyersFor10Seats_sellsExactly10AndRejects40() throws Exception {
        Fixture fixture = createPublishedEvent(10, Instant.now().plus(30, ChronoUnit.DAYS));
        List<String> buyers = createAttendees(50);

        ExecutorService pool = Executors.newFixedThreadPool(50);
        CountDownLatch ready = new CountDownLatch(50);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();
        List<Throwable> unexpected = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        for (String email : buyers) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    bookingService.createBooking(email, new CreateBookingRequest(fixture.tierId(), 1));
                    succeeded.incrementAndGet();
                } catch (SoldOutException ex) {
                    soldOut.incrementAndGet();
                } catch (Throwable ex) {
                    unexpected.add(ex);
                }
                return null;
            }));
        }

        ready.await();
        start.countDown();
        for (Future<?> future : futures) {
            future.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(unexpected).isEmpty();
        assertThat(succeeded.get()).isEqualTo(10);
        assertThat(soldOut.get()).isEqualTo(40);
        assertThat(soldCount(fixture.tierId())).isEqualTo(10);
        assertThat(bookingCount(fixture.tierId())).isEqualTo(10);
    }

    @Test
    void createBooking_whenSameUserSendsTwoParallelRequests_cannotExceedSixTicketCap() throws Exception {
        Fixture fixture = createPublishedEvent(100, Instant.now().plus(30, ChronoUnit.DAYS));
        String email = createAttendees(1).getFirst();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger capRejected = new AtomicInteger();
        List<Throwable> unexpected = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    bookingService.createBooking(email, new CreateBookingRequest(fixture.tierId(), 4));
                    succeeded.incrementAndGet();
                } catch (BusinessRuleException ex) {
                    capRejected.incrementAndGet();
                } catch (Throwable ex) {
                    unexpected.add(ex);
                }
                return null;
            }));
        }

        start.countDown();
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(unexpected).isEmpty();
        assertThat(succeeded.get()).isEqualTo(1);
        assertThat(capRejected.get()).isEqualTo(1);
        assertThat(soldCount(fixture.tierId())).isEqualTo(4);
    }

    @Test
    void cancelBooking_beforeCutoff_marksCancelledAndReleasesSeats() {
        Fixture fixture = createPublishedEvent(10, Instant.now().plus(30, ChronoUnit.DAYS));
        String email = createAttendees(1).getFirst();

        BookingResponse booked = bookingService.createBooking(email, new CreateBookingRequest(fixture.tierId(), 2));
        assertThat(soldCount(fixture.tierId())).isEqualTo(2);

        BookingResponse cancelled = bookingService.cancelBooking(email, booked.bookingRef());

        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(cancelled.cancelledAt()).isNotNull();
        assertThat(soldCount(fixture.tierId())).isZero();
    }

    @Test
    void cancelBooking_within24HoursOfStart_isRejectedAndKeepsSeats() {
        Fixture fixture = createPublishedEvent(10, Instant.now().plus(2, ChronoUnit.HOURS));
        String email = createAttendees(1).getFirst();

        BookingResponse booked = bookingService.createBooking(email, new CreateBookingRequest(fixture.tierId(), 1));

        assertThatThrownBy(() -> bookingService.cancelBooking(email, booked.bookingRef()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Bookings can only be cancelled at least 24 hours before event start");
        assertThat(soldCount(fixture.tierId())).isEqualTo(1);
    }

    @Test
    void cancelEvent_cancelsAllConfirmedBookingsForThatEvent() {
        Fixture fixture = createPublishedEvent(10, Instant.now().plus(30, ChronoUnit.DAYS));
        String ownerEmail = createOrganizerUser(fixture.organizer());
        for (String email : createAttendees(3)) {
            bookingService.createBooking(email, new CreateBookingRequest(fixture.tierId(), 1));
        }

        organizerEventService.cancelEvent(ownerEmail, fixture.eventId());

        Integer cancelled = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE event_id = ? AND status = 'CANCELLED'",
                Integer.class, fixture.eventId());
        assertThat(cancelled).isEqualTo(3);
    }

    private Fixture createPublishedEvent(int capacity, Instant startsAt) {
        Organizer organizer = new Organizer();
        organizer.setName(ORGANIZER_PREFIX + UUID.randomUUID());
        organizer.setContactEmail("contact" + EMAIL_DOMAIN);
        organizer.setStatus(OrganizerStatus.APPROVED);
        Organizer savedOrganizer = organizerRepository.save(organizer);

        Event event = new Event();
        event.setOrganizer(savedOrganizer);
        event.setTitle("Concurrency Live");
        event.setCategory(EventCategory.MUSIC);
        event.setVenueName("Test Arena");
        event.setCity("Mumbai");
        event.setStartsAt(startsAt);
        event.setEndsAt(startsAt.plus(4, ChronoUnit.HOURS));
        event.setStatus(EventStatus.PUBLISHED);

        TicketTier tier = new TicketTier();
        tier.setEvent(event);
        tier.setName("General");
        tier.setPrice(new BigDecimal("499.00"));
        tier.setCapacity(capacity);
        event.getTicketTiers().add(tier);

        Event savedEvent = eventRepository.save(event);
        return new Fixture(savedOrganizer, savedEvent.getId(), savedEvent.getTicketTiers().getFirst().getId());
    }

    private List<String> createAttendees(int count) {
        String batch = UUID.randomUUID().toString().substring(0, 8);
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User user = new User();
            user.setFullName("Attendee " + i);
            user.setEmail("attendee-" + batch + "-" + i + EMAIL_DOMAIN);
            user.setPasswordHash("$2a$10$dummyHashForConcurrencyTest");
            user.setRole(Role.ATTENDEE);
            users.add(user);
        }
        return userRepository.saveAll(users).stream().map(User::getEmail).toList();
    }

    private String createOrganizerUser(Organizer organizer) {
        User owner = new User();
        owner.setFullName("Event Owner");
        owner.setEmail("owner-" + UUID.randomUUID().toString().substring(0, 8) + EMAIL_DOMAIN);
        owner.setPasswordHash("$2a$10$dummyHashForConcurrencyTest");
        owner.setRole(Role.ORGANIZER);
        owner.setOrganizer(organizer);
        return userRepository.save(owner).getEmail();
    }

    private int soldCount(Long tierId) {
        return ticketTierRepository.findById(tierId).orElseThrow().getSoldCount();
    }

    private int bookingCount(Long tierId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE ticket_tier_id = ?", Integer.class, tierId);
        return count == null ? 0 : count;
    }

    private record Fixture(Organizer organizer, Long eventId, Long tierId) {
    }
}
