package br.com.marealta;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Rooms;
import br.com.marealta.domain.Stay;
import br.com.marealta.plan.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PlansTest {
    private static Stay stay(int room, String in, String out, String company) {
        return new Stay(Rooms.find(room).orElseThrow(), LocalDate.parse(in), LocalDate.parse(out), company);
    }

    @Test
    void highSeasonStandard() {
        Quote q = new StandardPlan().quote(stay(101, "2027-07-10", "2027-07-12", null));
        assertEquals(Money.of("599.76"), q.total());
    }

    @Test
    void nonRefundable() {
        Quote q = new NonRefundablePlan().quote(stay(201, "2027-03-10", "2027-03-12", null));
        assertEquals(Money.of("116.97"), q.discount());
        assertEquals(Money.ZERO, new NonRefundablePlan().refund(Money.of("10")));
    }

    @Test
    void corporate() {
        Quote q = new CorporatePlan().quote(stay(201, "2027-03-10", "2027-03-12", "TECNOSUL"));
        assertEquals(Money.of("779.80"), q.subtotal());
        assertEquals(Money.of("93.58"), q.discount());
        assertEquals(Money.of("686.22"), q.total());
        assertThrows(DomainException.class, () -> new CorporatePlan().quote(stay(201, "2027-03-10", "2027-03-12", null)));
        assertThrows(DomainException.class, () -> new CorporatePlan().quote(stay(201, "2027-03-10", "2027-03-12", "X")));
    }

    @Test
    void weekend() {
        WeekendPackagePlan p = new WeekendPackagePlan();
        Quote q = p.quote(stay(201, "2027-03-12", "2027-03-14", null));
        assertEquals(Money.of("820.00"), q.total());
        assertEquals(Money.ZERO, q.discount());
        assertEquals(Money.of("410.00"), p.refund(Money.of("820.00")));
        assertThrows(DomainException.class, () -> p.quote(stay(201, "2027-03-11", "2027-03-13", null)));
        assertThrows(DomainException.class, () -> p.quote(stay(201, "2027-03-12", "2027-03-15", null)));
    }

    @Test
    void longStay() {
        Quote q = new LongStayPlan().quote(stay(101, "2027-03-01", "2027-03-08", null));
        assertEquals(Money.of("1749.30"), q.subtotal());
        assertEquals(Money.of("174.93"), q.discount());
        assertEquals(Money.of("1574.37"), q.total());
        assertThrows(DomainException.class, () -> new LongStayPlan().quote(stay(101, "2027-03-01", "2027-03-07", null)));
    }

    @Test
    void receiptText() {
        Reservation r = new Reservation(7, 201, "Carla Dias", "c@x.com", "111.222.333-44",
                LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, "STANDARD", 2,
                Money.of("779.80"), Money.ZERO, Money.of("779.80"), ReservationStatus.CONFIRMED,
                Money.of("779.80"), null);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", Receipt.of(r, Rooms.find(201).orElseThrow()).text());
    }
}
