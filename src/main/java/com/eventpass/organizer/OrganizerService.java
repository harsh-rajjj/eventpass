package com.eventpass.organizer;

import com.eventpass.common.BusinessRuleException;
import com.eventpass.common.CacheConfig;
import com.eventpass.common.ResourceNotFoundException;
import com.eventpass.logging.Audited;
import com.eventpass.user.User;
import com.eventpass.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizerService {

    private final UserRepository userRepository;
    private final OrganizerRepository organizerRepository;

    public OrganizerResponse getMyProfile(String userEmail) {
        Organizer organizer = getOrganizerForUser(userEmail);
        return OrganizerResponse.from(organizer);
    }

    @Audited(action = "UPDATE_ORGANIZER_PROFILE")
    @CacheEvict(cacheNames = CacheConfig.EVENT_DETAILS_CACHE, allEntries = true)
    @Transactional
    public OrganizerResponse updateMyProfile(String userEmail, UpdateOrganizerProfileRequest request) {
        Organizer organizer = getOrganizerForUser(userEmail);
        String trimmedName = request.name().trim();

        if (organizerRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, organizer.getId())) {
            throw new BusinessRuleException("Organizer name '" + trimmedName + "' is already in use");
        }

        organizer.setName(trimmedName);
        organizer.setDescription(request.description());
        organizer.setWebsite(request.website());
        organizer.setContactEmail(request.contactEmail().trim().toLowerCase());

        log.info("Updated organizer profile id={} name={}", organizer.getId(), organizer.getName());
        return OrganizerResponse.from(organizer);
    }

    /**
     * Resolves the Organizer entity linked to the authenticated ORGANIZER user.
     */
    public Organizer getOrganizerForUser(String userEmail) {
        User user = userRepository.findWithOrganizerByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Organizer organizer = user.getOrganizer();
        if (organizer == null) {
            throw new BusinessRuleException("User " + userEmail + " is not linked to an organization");
        }
        return organizer;
    }
}
