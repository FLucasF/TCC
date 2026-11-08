package br.com.marealta.pousada;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.marealta.pousada.domain.*;
import br.com.marealta.pousada.notification.Message;

import static org.junit.jupiter.api.Assertions.*;

class ReservationServiceTest {
    InMemory.Messages log;
    ReservationService service;

    @BeforeEach
    void setUp() {
        log = new InMemory.Messages();
        service = new ReservationService(new InMemory.Reservations(), new RoomCatalog(), InMemory.listeners(log));
    }

    Reservation book(int room, String in, String out, int guests, RatePlan plan) {
        return service.create(new NewReservation(room, new Guest("Carla Dias", "carla@x.com", "111.222.333-44"),
                LocalDate.parse(in), LocalDate.parse(out), guests, plan));
    }

    DomainException.Kind failure(Runnable r) {
        return assertThrows(DomainException.class, r::run).kind();
    }

    @Test
    void confirmationSendsThreeMessages() {
        Reservation r = book(201, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD);
        assertEquals(new java.math.BigDecimal("0.00"), r.amountPaid());
        service.confirm(r.id());
        assertEquals(3, log.sent.size());
        Message guest = log.sent.stream().filter(m -> m.to().equals("carla@x.com")).findFirst().get();
        assertEquals("Reserva confirmada - Pousada Maré Alta", guest.subject());
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
                Até breve!""", guest.body());
        assertTrue(log.sent.stream().anyMatch(m -> m.to().startsWith("governanca@") && m.subject().equals("Reserva confirmada - quarto 201")));
        assertTrue(log.sent.stream().anyMatch(m -> m.to().startsWith("financeiro@") && m.subject().equals("Pagamento recebido - reserva 1")));
    }

    @Test
    void cancelPendingNotifiesNobody() {
        Reservation r = book(101, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD);
        Reservation c = service.cancel(r.id());
        assertEquals(new java.math.BigDecimal("0.00"), c.refund());
        assertTrue(log.sent.isEmpty());
    }

    @Test
    void cancelConfirmedRefundable() {
        Reservation r = book(101, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD);
        service.confirm(r.id());
        log.sent.clear();
        Reservation c = service.cancel(r.id());
        assertEquals(c.amountPaid(), c.refund());
        assertEquals(2, log.sent.size());
        assertTrue(log.sent.stream().anyMatch(m -> m.subject().equals("Reembolso - reserva 1")));
        assertTrue(log.sent.stream().anyMatch(m -> m.subject().equals("Reserva cancelada - quarto 101")));
    }

    @Test
    void cancelConfirmedNonRefundableOnlyHousekeeping() {
        Reservation r = book(101, "2027-03-10", "2027-03-12", 2, RatePlan.NON_REFUNDABLE);
        service.confirm(r.id());
        log.sent.clear();
        Reservation c = service.cancel(r.id());
        assertEquals(0, c.refund().signum());
        assertEquals(1, log.sent.size());
        assertTrue(log.sent.get(0).to().startsWith("governanca@"));
        assertEquals(c.amountPaid(), c.retainedAmount());
    }

    @Test
    void stateRules() {
        Reservation r = book(101, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD);
        assertEquals(DomainException.Kind.OPERATION_NOT_ALLOWED, failure(() -> service.checkIn(r.id())));
        assertEquals(DomainException.Kind.OPERATION_NOT_ALLOWED, failure(() -> service.document(r.id())));
        service.confirm(r.id());
        assertEquals(DomainException.Kind.OPERATION_NOT_ALLOWED, failure(() -> service.confirm(r.id())));
        service.checkIn(r.id());
        assertEquals(DomainException.Kind.OPERATION_NOT_ALLOWED, failure(() -> service.cancel(r.id())));
        Receipt receipt = service.checkOut(r.id());
        assertEquals("Comprovante de reserva", receipt.title());
        assertEquals(receipt, service.document(r.id()));
    }

    @Test
    void validations() {
        book(101, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD);
        assertEquals(DomainException.Kind.ROOM_UNAVAILABLE, failure(() -> book(101, "2027-03-11", "2027-03-13", 1, RatePlan.STANDARD)));
        book(101, "2027-03-12", "2027-03-13", 1, RatePlan.STANDARD); // a saída libera a noite
        assertEquals(DomainException.Kind.CAPACITY_EXCEEDED, failure(() -> book(102, "2027-03-10", "2027-03-12", 3, RatePlan.STANDARD)));
        assertEquals(DomainException.Kind.INVALID_REQUEST, failure(() -> book(102, "2027-03-10", "2027-03-12", 0, RatePlan.STANDARD)));
        assertEquals(DomainException.Kind.INVALID_REQUEST, failure(() -> book(102, "2027-03-10", "2027-03-10", 1, RatePlan.STANDARD)));
        assertEquals(DomainException.Kind.ROOM_NOT_FOUND, failure(() -> book(999, "2027-03-10", "2027-03-12", 1, RatePlan.STANDARD)));
    }

    @Test
    void cancelledReservationFreesRoom() {
        Reservation r = book(101, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD);
        service.cancel(r.id());
        book(101, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD);
    }
}
