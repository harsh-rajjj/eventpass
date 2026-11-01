package com.eventpass.event;

import com.eventpass.TestContainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// NOT_SUPPORTED: every thread must run its OWN real transaction, like real concurrent HTTP requests.
@DataJpaTest
@Import(TestContainersConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TicketTierRepositoryTest {

    private static final int SEATS_LEFT = 10;
    private static final int CONCURRENT_BUYERS = 50;

    @Autowired
    private TicketTierRepository ticketTierRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void reserveSeats_under50ConcurrentRequests_sellsExactlyRemainingCapacity() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Long tierId = ticketTierRepository.findAll().getFirst().getId();
        TicketTier original = ticketTierRepository.findById(tierId).orElseThrow();
        int originalCapacity = original.getCapacity();
        int originalSoldCount = original.getSoldCount();

        try {
            // Arrange: leave exactly 10 seats available
            tx.executeWithoutResult(status -> {
                TicketTier tier = ticketTierRepository.findById(tierId).orElseThrow();
                tier.setCapacity(originalSoldCount + SEATS_LEFT);
            });

            // Act: 50 buyers wait at the gate, then race for 1 seat each
            ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_BUYERS);
            CountDownLatch startGate = new CountDownLatch(1);
            List<Future<Integer>> results = new ArrayList<>();

            for (int i = 0; i < CONCURRENT_BUYERS; i++) {
                results.add(pool.submit(() -> {
                    startGate.await();
                    return tx.execute(status -> ticketTierRepository.reserveSeats(tierId, 1));
                }));
            }

            startGate.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

            int succeeded = 0;
            int soldOut = 0;
            for (Future<Integer> result : results) {
                if (result.get() == 1) {
                    succeeded++;
                } else {
                    soldOut++;
                }
            }

            // Assert: exactly 10 winners, 40 sold-out, and the counter never exceeds capacity
            assertThat(succeeded).isEqualTo(SEATS_LEFT);
            assertThat(soldOut).isEqualTo(CONCURRENT_BUYERS - SEATS_LEFT);

            TicketTier after = ticketTierRepository.findById(tierId).orElseThrow();
            assertThat(after.getSoldCount()).isEqualTo(originalSoldCount + SEATS_LEFT);
            assertThat(after.getSoldCount()).isLessThanOrEqualTo(after.getCapacity());
        } finally {
            // Restore seed data so other test classes are unaffected
            tx.executeWithoutResult(status -> {
                TicketTier tier = ticketTierRepository.findById(tierId).orElseThrow();
                tier.setCapacity(originalCapacity);
                tier.setSoldCount(originalSoldCount);
            });
        }
    }

    @Test
    void releaseSeats_neverDropsSoldCountBelowZero() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Long tierId = ticketTierRepository.findAll().getFirst().getId();
        int soldCount = ticketTierRepository.findById(tierId).orElseThrow().getSoldCount();

        Integer updated = tx.execute(status -> ticketTierRepository.releaseSeats(tierId, soldCount + 1));

        assertThat(updated).isZero();
        assertThat(ticketTierRepository.findById(tierId).orElseThrow().getSoldCount()).isEqualTo(soldCount);
    }
}
