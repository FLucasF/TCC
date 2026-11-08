package br.com.marealta;

import br.com.marealta.domain.ApiException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.Message;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.service.NewReservation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationFlowTest {
    private final InMemory m = new InMemory();

    private NewReservation req(int room, String in, String out, int guests, RatePlan plan) {
        return new NewReservation(room, "Carla Dias", "carla@example.com", "111.222.333-44",
                LocalDate.parse(in), LocalDate.parse(out), guests, plan);
    }

    private ErrorCode codeOf(Runnable r) {
        return assertThrows(ApiException.class, r::run).code();
    }

    @Test
    void createValidations() {
        assertEquals(ErrorCode.ROOM_NOT_FOUND, codeOf(() -> m.service.create(req(999, "2027-03-10", "2027-03-12", 1, RatePlan.STANDARD))));
        assertEquals(ErrorCode.CAPACITY_EXCEEDED, codeOf(() -> m.service.create(req(101, "2027-03-10", "2027-03-12", 3, RatePlan.STANDARD))));
        assertEquals(ErrorCode.INVALID_REQUEST, codeOf(() -> m.service.create(req(101, "2027-03-10", "2027-03-10", 1, RatePlan.STANDARD))));
        assertEquals(ErrorCode.INVALID_REQUEST, codeOf(() -> m.service.create(req(101, "2027-03-10", "2027-03-12", 0, RatePlan.STANDARD))));
    }

    @Test
    void overlapIgnoresCancelledAndAllowsBackToBack() {
        Reservation a = m.service.create(req(201, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD));
        m.service.create(req(201, "2027-03-12", "2027-03-14", 2, RatePlan.STANDARD)); // sai e entra no mesmo dia
        assertEquals(ErrorCode.ROOM_UNAVAILABLE, codeOf(() -> m.service.create(req(201, "2027-03-11", "2027-03-13", 2, RatePlan.STANDARD))));
        m.service.cancel(a.id());
        m.service.create(req(201, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD));
    }

    @Test
    void confirmationNotifiesGuestHousekeepingAndFinance() {
        Reservation r = m.service.create(req(201, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD));
        assertEquals(ReservationStatus.PENDING, r.status());
        assertTrue(m.sent.isEmpty());

        Reservation c = m.service.confirm(r.id());
        assertEquals(new BigDecimal("779.80"), c.amountPaid());
        assertEquals(3, m.sent.size());

        String receipt = """
                Comprovante de reserva nº 1
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""";
        Message guest = byTo("carla@example.com").get(0);
        assertEquals("Reserva confirmada - Pousada Maré Alta", guest.subject());
        assertEquals("Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt + "\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!", guest.body());

        Message house = byTo("governanca@pousadamarealta.com.br").get(0);
        assertEquals("Reserva confirmada - quarto 201", house.subject());
        assertTrue(house.body().contains("10/03/2027") && house.body().contains("12/03/2027"));

        Message fin = byTo("financeiro@pousadamarealta.com.br").get(0);
        assertEquals("Pagamento recebido - reserva 1", fin.subject());
        assertTrue(fin.body().contains("R$ 779,80"));
        assertEquals(Message.Channel.EMAIL, fin.channel());
    }

    @Test
    void receiptShowsDiscountAndSingularNight() {
        Reservation r = m.service.create(req(101, "2027-03-10", "2027-03-11", 1, RatePlan.NON_REFUNDABLE));
        m.service.confirm(r.id());
        String text = Receipt.of(m.service.get(r.id()), m.rooms.find(101).get()).toText();
        assertTrue(text.contains("| 1 diária\n"));
        assertTrue(text.contains("Diárias: R$ 249,90\nDesconto: R$ 37,48\nTotal: R$ 212,42"));
    }

    @Test
    void cancelRefundRules() {
        // pendente: sem avisos, nada a devolver
        Reservation p = m.service.create(req(101, "2027-03-10", "2027-03-12", 1, RatePlan.STANDARD));
        Reservation cp = m.service.cancel(p.id());
        assertEquals(new BigDecimal("0.00"), cp.refund());
        assertTrue(m.sent.isEmpty());

        // confirmada, plano normal: avisa governança e financeiro
        Reservation n = m.service.create(req(101, "2027-03-10", "2027-03-12", 1, RatePlan.STANDARD));
        m.service.confirm(n.id());
        m.sent.clear();
        Reservation cn = m.service.cancel(n.id());
        assertEquals(cn.amountPaid(), cn.refund());
        assertEquals(2, m.sent.size());
        assertEquals("Reserva cancelada - quarto 101", byTo("governanca@pousadamarealta.com.br").get(0).subject());
        Message fin = byTo("financeiro@pousadamarealta.com.br").get(0);
        assertEquals("Reembolso - reserva " + n.id(), fin.subject());
        assertTrue(fin.body().contains("R$ 499,80"));

        // confirmada, não reembolsável: só governança
        Reservation x = m.service.create(req(102, "2027-03-10", "2027-03-12", 1, RatePlan.NON_REFUNDABLE));
        m.service.confirm(x.id());
        m.sent.clear();
        Reservation cx = m.service.cancel(x.id());
        assertEquals(new BigDecimal("0.00"), cx.refund());
        assertEquals(1, m.sent.size());
        assertEquals("governanca@pousadamarealta.com.br", m.sent.get(0).to());
    }

    @Test
    void stateMachine() {
        Reservation r = m.service.create(req(101, "2027-03-10", "2027-03-12", 1, RatePlan.STANDARD));
        assertNull(r.refund());
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, codeOf(() -> m.service.checkIn(r.id())));
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, codeOf(() -> m.service.checkOut(r.id())));
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, codeOf(() -> m.service.document(r.id())));
        m.service.confirm(r.id());
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, codeOf(() -> m.service.confirm(r.id())));
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, codeOf(() -> m.service.checkOut(r.id())));
        m.service.document(r.id());
        m.service.checkIn(r.id());
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, codeOf(() -> m.service.cancel(r.id())));
        Receipt out = m.service.checkOut(r.id());
        assertEquals("Comprovante de reserva", out.title());
        m.service.document(r.id());
        assertEquals(ErrorCode.RESERVATION_NOT_FOUND, codeOf(() -> m.service.get(99)));
    }

    @Test
    void reports() {
        Reservation a = m.service.create(req(102, "2027-07-10", "2027-07-12", 2, RatePlan.STANDARD));
        Reservation b = m.service.create(req(101, "2027-07-11", "2027-07-13", 2, RatePlan.NON_REFUNDABLE));
        Reservation pending = m.service.create(req(201, "2027-07-10", "2027-07-12", 2, RatePlan.STANDARD));
        m.service.confirm(a.id());
        m.service.confirm(b.id());
        m.service.cancel(b.id()); // não reembolsável: o dinheiro fica

        assertEquals("room,type,reservation,check_in,check_out\n102,STANDARD,1,2027-07-10,2027-07-12\ntotal,1/5\n",
                m.reports.occupancy(LocalDate.of(2027, 7, 11)));
        assertEquals("room,type,reservation,check_in,check_out\ntotal,0/5\n",
                m.reports.occupancy(LocalDate.of(2027, 7, 12)));

        // b: 2 noites de 299.88 = 599.76, desconto 89.96 (89.964), total 509.80
        assertEquals("reservation,room,check_in,check_out,status,amount\n"
                + "1,102,2027-07-10,2027-07-12,CONFIRMED,599.76\n"
                + "2,101,2027-07-11,2027-07-13,CANCELLED,509.80\n"
                + "total,1109.56\n", m.reports.revenue(java.time.YearMonth.of(2027, 7)));
        assertEquals("reservation,room,check_in,check_out,status,amount\ntotal,0.00\n",
                m.reports.revenue(java.time.YearMonth.of(2027, 8)));
        assertEquals(ReservationStatus.PENDING, m.service.get(pending.id()).status());
    }

    private List<Message> byTo(String to) {
        return m.sent.stream().filter(x -> x.to().equals(to)).toList();
    }
}
