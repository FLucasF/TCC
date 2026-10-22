package br.com.mealta;

import br.com.mealta.domain.CreateReservation;
import br.com.mealta.domain.ErrorCode;
import br.com.mealta.domain.Money;
import br.com.mealta.domain.PousadaException;
import br.com.mealta.domain.Pricing;
import br.com.mealta.domain.RatePlan;
import br.com.mealta.domain.Reservation;
import br.com.mealta.domain.ReservationService;
import br.com.mealta.domain.ReservationStatus;
import br.com.mealta.domain.RoomCatalog;
import br.com.mealta.report.CsvRenderer;
import br.com.mealta.report.ReportCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PousadaTest {
    InMemoryStore store;
    RoomCatalog rooms = new RoomCatalog();
    ReservationService service;
    ReportCatalog reports;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        service = new ReservationService(store, store, store, rooms, new Pricing());
        reports = new ReportCatalog(store, rooms, Money.of("4.50"));
    }

    CreateReservation request(int room, String in, String out, int guests, RatePlan plan) {
        return new CreateReservation(room, "Carla Dias", "carla@example.com", "111.222.333-44",
                LocalDate.parse(in), LocalDate.parse(out), guests, plan);
    }

    String report(String name, String param) {
        return new CsvRenderer().render(reports.definitions().stream()
                .filter(d -> d.name().equals(name)).findFirst().orElseThrow().builder().apply(param));
    }

    @Test
    void highSeasonPricing() {
        Reservation r = service.create(request(101, "2027-07-10", "2027-07-12", 2, RatePlan.STANDARD));
        assertEquals(new BigDecimal("599.76"), r.total());
        assertEquals(2, r.nights());
        Pricing p = new Pricing();
        assertFalse(p.isHighSeason(LocalDate.parse("2027-12-14")));
        assertTrue(p.isHighSeason(LocalDate.parse("2027-12-15")));
        assertTrue(p.isHighSeason(LocalDate.parse("2028-02-29")));
        assertFalse(p.isHighSeason(LocalDate.parse("2027-03-01")));
    }

    @Test
    void nonRefundableDiscount() {
        Reservation r = service.create(request(201, "2027-03-10", "2027-03-12", 2, RatePlan.NON_REFUNDABLE));
        assertEquals(new BigDecimal("779.80"), r.subtotal());
        assertEquals(new BigDecimal("116.97"), r.discount());
        assertEquals(new BigDecimal("662.83"), r.total());
    }

    @Test
    void confirmationSendsEmailWithReceipt() {
        service.create(request(201, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD));
        service.confirm(1);
        var m = store.all().get(0);
        assertEquals("Reserva confirmada - Pousada Maré Alta", m.subject());
        assertEquals("""
                Olá, Carla Dias!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                Comprovante de reserva nº 1
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!""", m.body());
    }

    @Test
    void cancellationRefunds() {
        service.create(request(101, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD));
        service.confirm(1);
        assertEquals(new BigDecimal("499.80"), service.cancel(1).refund());

        service.create(request(101, "2027-03-10", "2027-03-12", 2, RatePlan.NON_REFUNDABLE));
        service.confirm(2);
        assertEquals(Money.ZERO, service.cancel(2).refund());

        service.create(request(101, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD));
        assertEquals(Money.ZERO, service.cancel(3).refund());
    }

    @Test
    void stateRulesAndAvailability() {
        service.create(request(101, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD));
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, assertThrows(PousadaException.class, () -> service.checkIn(1)).code());
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, assertThrows(PousadaException.class, () -> service.document(1)).code());
        assertEquals(ErrorCode.ROOM_UNAVAILABLE, assertThrows(PousadaException.class,
                () -> service.create(request(101, "2027-03-11", "2027-03-13", 1, RatePlan.STANDARD))).code());
        service.create(request(101, "2027-03-12", "2027-03-13", 1, RatePlan.STANDARD));
        assertEquals(ErrorCode.CAPACITY_EXCEEDED, assertThrows(PousadaException.class,
                () -> service.create(request(101, "2027-04-01", "2027-04-02", 3, RatePlan.STANDARD))).code());
        service.confirm(1);
        service.checkIn(1);
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, assertThrows(PousadaException.class, () -> service.cancel(1)).code());
        assertEquals(1, service.checkOut(1).reservationId());
        assertEquals(ReservationStatus.CHECKED_OUT, service.get(1).status());
    }

    @Test
    void reports() {
        service.create(request(101, "2027-07-10", "2027-07-12", 2, RatePlan.STANDARD));
        service.confirm(1);
        service.create(request(201, "2027-07-11", "2027-07-14", 2, RatePlan.NON_REFUNDABLE));
        service.confirm(2);
        service.cancel(2);
        service.create(request(301, "2027-07-11", "2027-07-12", 2, RatePlan.STANDARD));

        assertEquals("""
                Pousada Maré Alta,occupancy,2027-07-11
                room,type,reservation,check_in,check_out
                101,STANDARD,1,2027-07-10,2027-07-12
                total,1/5
                """, report("occupancy", "2027-07-11"));
        assertEquals("""
                Pousada Maré Alta,revenue,2027-07
                reservation,room,check_in,check_out,status,amount
                1,101,2027-07-10,2027-07-12,CONFIRMED,599.76
                2,201,2027-07-11,2027-07-14,CANCELLED,1193.09
                total,1792.85
                """, report("revenue", "2027-07"));
        assertEquals("""
                Pousada Maré Alta,tourism-tax,2027-07
                reservation,room,check_in,check_out,guests,nights,tax
                1,101,2027-07-10,2027-07-12,2,2,18.00
                total,18.00
                """, report("tourism-tax", "2027-07"));
        assertEquals("""
                Pousada Maré Alta,rate-plans,2027-07
                rate_plan,reservations,amount
                STANDARD,1,599.76
                NON_REFUNDABLE,1,1193.09
                total,2,1792.85
                """, report("rate-plans", "2027-07"));
    }

    @Test
    void invalidReportParameter() {
        assertEquals(ErrorCode.INVALID_REQUEST, assertThrows(PousadaException.class, () -> report("revenue", "2027-7")).code());
        assertEquals(ErrorCode.INVALID_REQUEST, assertThrows(PousadaException.class, () -> report("occupancy", null)).code());
    }
}
