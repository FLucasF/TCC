package br.com.marealta;

import br.com.marealta.domain.AppException;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.event.Message;
import br.com.marealta.service.ReportService;
import br.com.marealta.service.ReservationService;
import br.com.marealta.service.ReservationService.NewReservation;
import br.com.marealta.store.MemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReservationFlowTest {
    MemoryStore store;
    ReservationService svc;
    ReportService reports;

    @BeforeEach
    void setUp() {
        store = new MemoryStore();
        svc = new ReservationService(store, Main.subscribers());
        reports = new ReportService(store);
    }

    NewReservation req(int room, String in, String out, int guests, RatePlan plan) {
        return new NewReservation(room, "Carla Dias", "carla@example.com", "111.222.333-44",
                LocalDate.parse(in), LocalDate.parse(out), guests, plan);
    }

    static void assertCode(String code, Runnable r) {
        assertEquals(code, assertThrows(AppException.class, r::run).code());
    }

    @Test
    void highSeasonPricing() {
        Reservation r = svc.create(req(101, "2027-07-10", "2027-07-12", 2, RatePlan.STANDARD));
        assertEquals(new BigDecimal("599.76"), r.total());
        Reservation nr = svc.create(req(102, "2027-03-10", "2027-03-12", 1, RatePlan.NON_REFUNDABLE));
        assertEquals(new BigDecimal("499.80"), nr.subtotal());
        assertEquals(new BigDecimal("74.97"), nr.discount());
        assertEquals(new BigDecimal("424.83"), nr.total());
    }

    @Test
    void seasonBoundaries() {
        // 14/12 normal, 15/12 alta
        Reservation r = svc.create(req(101, "2027-12-14", "2027-12-16", 1, RatePlan.STANDARD));
        assertEquals(new BigDecimal("549.78"), r.subtotal());
    }

    @Test
    void confirmationSendsFourMessagesAndReceipt() {
        Reservation r = svc.create(req(201, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD));
        svc.confirm("1");
        List<Message> ms = svc.messages();
        assertEquals(3, ms.size());
        Message guest = ms.stream().filter(m -> m.to().equals("carla@example.com")).findFirst().orElseThrow();
        assertEquals("Reserva confirmada - Pousada Maré Alta", guest.subject());
        assertEquals("Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + "Comprovante de reserva nº 1\nHóspede: Carla Dias (CPF 111.222.333-44)\nQuarto: 201 (DELUXE)\n"
                + "Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 779,80\nTotal: R$ 779,80\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!", guest.body());
        assertTrue(ms.stream().anyMatch(m -> m.to().equals("governanca@pousadamarealta.com.br")
                && m.subject().equals("Reserva confirmada - quarto 201")));
        assertTrue(ms.stream().anyMatch(m -> m.to().equals("financeiro@pousadamarealta.com.br")
                && m.subject().equals("Pagamento recebido - reserva 1") && m.body().contains("R$ 779,80")));
        assertEquals(r.total(), svc.get("1").amountPaid());
    }

    @Test
    void cancellationRules() {
        svc.create(req(101, "2027-03-10", "2027-03-12", 1, RatePlan.STANDARD));
        svc.cancel("1");
        assertTrue(svc.messages().isEmpty());
        assertEquals(new BigDecimal("0.00"), svc.get("1").refund());

        svc.create(req(101, "2027-03-10", "2027-03-12", 1, RatePlan.STANDARD));
        svc.confirm("2");
        int before = svc.messages().size();
        Reservation c = svc.cancel("2");
        assertEquals(c.total(), c.refund());
        List<Message> after = svc.messages().subList(before, svc.messages().size());
        assertEquals(2, after.size());

        svc.create(req(101, "2027-03-10", "2027-03-12", 1, RatePlan.NON_REFUNDABLE));
        svc.confirm("3");
        before = svc.messages().size();
        assertEquals(new BigDecimal("0.00"), svc.cancel("3").refund());
        after = svc.messages().subList(before, svc.messages().size());
        assertEquals(1, after.size());
        assertEquals("Reserva cancelada - quarto 101", after.get(0).subject());
        assertCode("OPERATION_NOT_ALLOWED", () -> svc.cancel("3"));
    }

    @Test
    void stateMachineAndDocument() {
        svc.create(req(301, "2027-03-10", "2027-03-11", 4, RatePlan.STANDARD));
        assertCode("OPERATION_NOT_ALLOWED", () -> svc.document("1"));
        assertCode("OPERATION_NOT_ALLOWED", () -> svc.checkIn("1"));
        svc.confirm("1");
        assertCode("OPERATION_NOT_ALLOWED", () -> svc.confirm("1"));
        assertCode("OPERATION_NOT_ALLOWED", () -> svc.checkOut("1"));
        svc.checkIn("1");
        assertCode("OPERATION_NOT_ALLOWED", () -> svc.cancel("1"));
        Receipt r = svc.checkOut("1");
        assertEquals("Comprovante de reserva", r.title());
        assertTrue(r.toText().contains("1 diária\n"));
        assertEquals(r, svc.document("1"));
        assertCode("RESERVATION_NOT_FOUND", () -> svc.get("99"));
        assertCode("RESERVATION_NOT_FOUND", () -> svc.get("abc"));
    }

    @Test
    void validationAndAvailability() {
        assertCode("CAPACITY_EXCEEDED", () -> svc.create(req(101, "2027-03-10", "2027-03-12", 3, RatePlan.STANDARD)));
        assertCode("INVALID_REQUEST", () -> svc.create(req(101, "2027-03-10", "2027-03-10", 1, RatePlan.STANDARD)));
        assertCode("INVALID_REQUEST", () -> svc.create(req(101, "2027-03-10", "2027-03-12", 0, RatePlan.STANDARD)));
        assertCode("ROOM_NOT_FOUND", () -> svc.create(req(999, "2027-03-10", "2027-03-12", 1, RatePlan.STANDARD)));
        svc.create(req(101, "2027-03-10", "2027-03-12", 1, RatePlan.STANDARD));
        assertCode("ROOM_UNAVAILABLE", () -> svc.create(req(101, "2027-03-11", "2027-03-13", 1, RatePlan.STANDARD)));
        svc.create(req(101, "2027-03-12", "2027-03-13", 1, RatePlan.STANDARD)); // sai de manhã, entra à tarde
        svc.cancel("1");
        svc.create(req(101, "2027-03-10", "2027-03-12", 1, RatePlan.STANDARD)); // cancelada libera
    }

    @Test
    void reports() {
        svc.create(req(102, "2027-03-10", "2027-03-12", 1, RatePlan.STANDARD)); // 1
        svc.create(req(101, "2027-03-11", "2027-03-13", 1, RatePlan.NON_REFUNDABLE)); // 2
        svc.create(req(201, "2027-03-11", "2027-03-13", 1, RatePlan.STANDARD)); // 3 pendente
        svc.confirm("1");
        svc.confirm("2");
        assertEquals("room,type,reservation,check_in,check_out\n101,STANDARD,2,2027-03-11,2027-03-13\n"
                + "102,STANDARD,1,2027-03-10,2027-03-12\ntotal,2/5\n", reports.occupancy(LocalDate.parse("2027-03-11")));
        assertEquals("room,type,reservation,check_in,check_out\n101,STANDARD,2,2027-03-11,2027-03-13\ntotal,1/5\n",
                reports.occupancy(LocalDate.parse("2027-03-12")));
        svc.cancel("2"); // não reembolsável: continua na receita
        assertEquals("reservation,room,check_in,check_out,status,amount\n"
                + "1,102,2027-03-10,2027-03-12,CONFIRMED,499.80\n"
                + "2,101,2027-03-11,2027-03-13,CANCELLED,424.83\n"
                + "total,924.63\n", reports.revenue(YearMonth.parse("2027-03")));
    }
}
