package com.eventpass.organizer;

import com.eventpass.TestContainersConfiguration;
import com.eventpass.event.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({TestContainersConfiguration.class, AdminOrganizerServiceRollbackTest.FaultInjectionConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AdminOrganizerServiceRollbackTest {

    private static final Long SEED_ORGANIZER_ID = 1L;
    private static final String FAILURE_MESSAGE = "Simulated failure after bulk unpublish";

    @Autowired
    private AdminOrganizerService adminOrganizerService;

    @Autowired
    private OrganizerRepository organizerRepository;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void suspend_whenFailureHappensInsideMethod_rollsBackOrganizerAndEvents() {
        OrganizerStatus statusBefore = organizerRepository.findById(SEED_ORGANIZER_ID).orElseThrow().getStatus();
        long publishedBefore = eventRepository.countPublishedEventsByOrganizerId(SEED_ORGANIZER_ID);

        assertThat(statusBefore).isNotEqualTo(OrganizerStatus.SUSPENDED);
        assertThat(publishedBefore).isPositive();

        assertThatThrownBy(() -> adminOrganizerService.suspend(SEED_ORGANIZER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(FAILURE_MESSAGE);

        assertThat(organizerRepository.findById(SEED_ORGANIZER_ID).orElseThrow().getStatus())
                .isEqualTo(statusBefore);
        assertThat(eventRepository.countPublishedEventsByOrganizerId(SEED_ORGANIZER_ID))
                .isEqualTo(publishedBefore);
    }

    @TestConfiguration
    static class FaultInjectionConfig {

        @Bean
        AdminOrganizerService adminOrganizerService(
                OrganizerRepository organizerRepository,
                EventRepository eventRepository
        ) {
            EventRepository failingAfterBulkUpdate = (EventRepository) Proxy.newProxyInstance(
                    EventRepository.class.getClassLoader(),
                    new Class<?>[]{EventRepository.class},
                    (proxy, method, args) -> {
                        Object result;
                        try {
                            result = method.invoke(eventRepository, args);
                        } catch (InvocationTargetException ex) {
                            throw ex.getTargetException();
                        }
                        if ("updatePublishedEventsStatusByOrganizerId".equals(method.getName())) {
                            throw new IllegalStateException(FAILURE_MESSAGE);
                        }
                        return result;
                    });
            return new AdminOrganizerService(organizerRepository, failingAfterBulkUpdate);
        }
    }
}
