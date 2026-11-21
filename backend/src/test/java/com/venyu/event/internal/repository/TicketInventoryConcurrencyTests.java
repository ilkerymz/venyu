package com.venyu.event.internal.repository;

import com.venyu.TestcontainersConfiguration;
import com.venyu.event.internal.domain.Event;
import com.venyu.event.internal.domain.EventDetails;
import com.venyu.event.internal.domain.EventVisibility;
import com.venyu.event.internal.domain.TicketDetails;
import com.venyu.event.internal.domain.TicketType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.venyu.venue.VenueBookings;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// Bilinçli olarak @Transactional değil: her ayırma kendi transaction'ında, gerçek eşzamanlılıkla.
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class TicketInventoryConcurrencyTests {

    @Autowired
    EventRepository events;

    @Autowired
    TicketTypeRepository tickets;

    @Autowired
    PlatformTransactionManager transactionManager;

    @MockitoBean
    VenueBookings venueBookings;

    TransactionTemplate tx;
    UUID eventId;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        Instant start = Instant.now().plus(Duration.ofDays(7));
        eventId = events.save(Event.atAddress(UUID.randomUUID(), "slug-" + UUID.randomUUID(),
            new EventDetails("Test", null, EventVisibility.PUBLIC), "Adres", start,
            start.plus(Duration.ofHours(2)))).getId();
    }

    @AfterEach
    void cleanUp() {
        events.deleteAll(); // ticket_types FK cascade ile silinir
    }

    @Test
    void twoHundredThreadsCompeteForTheLastSeatAndExactlyOneWins() throws Exception {
        UUID ticketId = ticket(1).getId();
        int threads = 200;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            results.add(executor.submit(() -> {
                start.await();
                return tx.execute(status -> tickets.reserveSeat(ticketId));
            }));
        }
        start.countDown();

        int winners = 0;
        for (Future<Integer> result : results) {
            winners += result.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(winners).isEqualTo(1);
        assertThat(tickets.findById(ticketId).orElseThrow().getSold()).isEqualTo(1);
    }

    @Test
    void unlimitedTicketAcceptsEveryone() {
        UUID ticketId = ticket(null).getId();

        for (int i = 0; i < 5; i++) {
            Integer reserved = tx.execute(status -> tickets.reserveSeat(ticketId));
            assertThat(reserved).isEqualTo(1);
        }
        assertThat(tickets.findById(ticketId).orElseThrow().getSold()).isEqualTo(5);
    }

    @Test
    void editingTicketDoesNotOverwriteConcurrentSales() {
        UUID ticketId = ticket(10).getId();
        TransactionTemplate newTx = new TransactionTemplate(transactionManager);
        newTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        tx.executeWithoutResult(status -> {
            TicketType stale = tickets.findById(ticketId).orElseThrow(); // bellekte sold = 0

            // Bu arada başka transaction'larda 3 bilet satılır (DB'de sold = 3).
            for (int i = 0; i < 3; i++) {
                newTx.execute(inner -> tickets.reserveSeat(ticketId));
            }

            stale.update(new TicketDetails("Yeni Ad", null, BigDecimal.ZERO, 10), Instant.now());
        }); // commit: UPDATE name, capacity... ama sold YOK (updatable = false)

        TicketType reloaded = tickets.findById(ticketId).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Yeni Ad");
        assertThat(reloaded.getSold()).isEqualTo(3); // satışlar ezilmedi
    }

    private TicketType ticket(Integer capacity) {
        return tickets.save(TicketType.define(eventId, new TicketDetails("Bilet", null, BigDecimal.ZERO, capacity)));
    }
}
