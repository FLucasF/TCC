package br.com.marealta;

import br.com.marealta.domain.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DomainTest {
    private static Reservation book(int room, IdentityFields f, String in, String out, RatePlan plan, long id) {
        Reservation r = Reservation.book(RoomCatalog.find(room).orElseThrow(), "Carla Dias", "c@x.com",
                IdentityKinds.from(f), LocalDate.parse(in), LocalDate.parse(out), 2, plan);
        r.assignId(id);
        return r;
    }

    private static final IdentityFields CPF = new IdentityFields("111.222.333-44", null, null, null, null);

    @Test
    void highSeasonPricing() {
        Pricing.Quote q = Pricing.quote(RoomType.STANDARD, LocalDate.parse("2027-07-10"),
                LocalDate.parse("2027-07-12"), RatePlan.STANDARD);
        assertEquals("599.76", q.total().toPlainString());
        assertEquals(2, q.nights());
        assertTrue(Pricing.isHighSeason(LocalDate.parse("2027-12-15")));
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2027-12-14")));
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2027-03-01")));
    }

    @Test
    void nonRefundableDiscount() {
        Pricing.Quote q = Pricing.quote(RoomType.DELUXE, LocalDate.parse("2027-03-10"),
                LocalDate.parse("2027-03-12"), RatePlan.NON_REFUNDABLE);
        assertEquals("779.80", q.subtotal().toPlainString());
        assertEquals("116.97", q.discount().toPlainString());
        assertEquals("662.83", q.total().toPlainString());
    }

    @Test
    void receipt() {
        Reservation r = book(201, CPF, "2027-03-10", "2027-03-12", RatePlan.STANDARD, 7);
        r.confirm();
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", r.document().text());
    }

    @Test
    void invoice() {
        Reservation r = book(201, new IdentityFields(null, "12.345.678/0001-90", "Tecnosul Ltda", null, null),
                "2027-03-10", "2027-03-12", RatePlan.STANDARD, 3);
        r.confirm();
        assertEquals("""
                Fatura de hospedagem nº 3
                Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
                Hóspede: Carla Dias
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                ISS retido (5%): R$ 38,99""", r.document().text());
    }

    @Test
    void bookingReceiptWithDiscountAndOneNight() {
        Reservation r = book(301, new IdentityFields(null, null, null, "X1234567", "United Kingdom"),
                "2027-03-10", "2027-03-11", RatePlan.NON_REFUNDABLE, 4);
        r.confirm();
        assertEquals("""
                Booking receipt no. 4
                Guest: Carla Dias (passport X1234567, United Kingdom)
                Room: 301 (SUITE)
                Check-in: 2027-03-10 | Check-out: 2027-03-11 | 1 night
                Nights: BRL 579.90
                Discount: BRL 86.98
                Total: BRL 492.92""", r.document().text());
    }

    @Test
    void identityMustBeExactlyOneKind() {
        assertThrows(DomainException.class, () -> IdentityKinds.from(new IdentityFields(null, null, null, null, null)));
        assertThrows(DomainException.class, () -> IdentityKinds.from(new IdentityFields("1", null, null, "P", "BR")));
        assertThrows(DomainException.class, () -> IdentityKinds.from(new IdentityFields(null, "1", null, null, null)));
        assertThrows(DomainException.class, () -> IdentityKinds.from(new IdentityFields(null, null, null, "P", null)));
    }

    @Test
    void lifecycleAndRefund() {
        Reservation r = book(101, CPF, "2027-03-10", "2027-03-12", RatePlan.NON_REFUNDABLE, 1);
        assertThrows(DomainException.class, r::registerCheckIn);
        assertThrows(DomainException.class, r::document);
        r.confirm();
        assertThrows(DomainException.class, r::confirm);
        r.cancel();
        assertEquals("0.00", r.refund().toPlainString());
        assertEquals(r.total(), r.amountKept());
        assertThrows(DomainException.class, r::registerCheckIn);

        Reservation pending = book(101, CPF, "2027-03-10", "2027-03-12", RatePlan.STANDARD, 2);
        pending.cancel();
        assertEquals("0.00", pending.refund().toPlainString());
    }

    @Test
    void moneyFormats() {
        assertEquals("R$ 1.234,56", Money.brl(Money.of("1234.56")));
        assertEquals("BRL 1,234.56", Money.international(Money.of("1234.56")));
        assertEquals("0.02", Money.of("0.025").toPlainString());
    }
}
