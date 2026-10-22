package com.marealta.pousada;

import com.marealta.pousada.domain.*;
import com.marealta.pousada.mail.Message;
import com.marealta.pousada.persistence.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class JdbcRepositoryTest {
    @Test
    void persistsAndQueries() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:t1;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        Database.createSchema(ds);
        Database.createSchema(ds); // idempotente

        var repo = new JdbcReservationRepository(ds);
        var in = LocalDate.of(2027, 7, 10);
        var out = LocalDate.of(2027, 7, 12);
        var request = new NewReservation(101, "Ana", "ana@x.com", "1", in, out, 2, RatePlan.NON_REFUNDABLE);
        Reservation r = Reservation.open(request, Quote.of(RoomType.STANDARD, RatePlan.NON_REFUNDABLE, in, out));
        r.assignId(repo.insert(r));
        assertEquals(1, r.id());

        r.confirm();
        r.cancel();
        repo.update(r);
        Reservation loaded = repo.find(1).orElseThrow();
        assertEquals(ReservationStatus.CANCELLED, loaded.status());
        assertEquals(new java.math.BigDecimal("0.00"), loaded.refund());
        assertEquals(new java.math.BigDecimal("509.80"), loaded.kept());
        assertTrue(repo.findActiveOverlapping(101, in, out).isEmpty());
        assertEquals(1, repo.findOverlapping(in, in.plusDays(1)).size());
        assertEquals(1, repo.findByCheckInBetween(in, in).size());

        var messages = new JdbcMessageRepository(ds);
        messages.save(Message.email("a@b.com", "s", "b"));
        assertEquals(1L, messages.findAll().get(0).id());
    }
}
