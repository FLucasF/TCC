package br.com.mare.pousada;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomainTest {
    private static Reservation reservation(int id, int room, GuestIdentity identity, String name, String in,
                                           String out, RatePlan plan) {
        return Reservation.pending(RoomCatalog.find(room).orElseThrow(), name, "x@y.z", identity,
                LocalDate.parse(in), LocalDate.parse(out), 2, plan).withId(id);
    }

    @Test
    void highSeasonPricing() {
        Quote q = PricingPolicy.quote(RoomCatalog.find(101).orElseThrow(), RatePlan.STANDARD,
                LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"));
        assertEquals(new BigDecimal("599.76"), q.total());
        assertEquals(2, q.nights());
    }

    @Test
    void seasonBoundaries() {
        assertEquals(false, PricingPolicy.isHighSeason(LocalDate.parse("2027-12-14")));
        assertEquals(true, PricingPolicy.isHighSeason(LocalDate.parse("2027-12-15")));
        assertEquals(true, PricingPolicy.isHighSeason(LocalDate.parse("2028-02-29")));
        assertEquals(false, PricingPolicy.isHighSeason(LocalDate.parse("2027-03-01")));
    }

    @Test
    void nonRefundableDiscount() {
        Quote q = PricingPolicy.quote(RoomCatalog.find(201).orElseThrow(), RatePlan.NON_REFUNDABLE,
                LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"));
        assertEquals(new BigDecimal("779.80"), q.subtotal());
        assertEquals(new BigDecimal("116.97"), q.discount());
        assertEquals(new BigDecimal("662.83"), q.total());
    }

    @Test
    void receipt() {
        Reservation r = reservation(7, 201, new CpfIdentity("111.222.333-44"), "Carla Dias", "2027-03-10",
                "2027-03-12", RatePlan.STANDARD);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", r.identity().document(r).text());
    }

    @Test
    void invoice() {
        Reservation r = reservation(3, 201, new CompanyIdentity("12.345.678/0001-90", "Tecnosul Ltda"), "Ana Souza",
                "2027-03-10", "2027-03-12", RatePlan.STANDARD);
        assertEquals("""
                Fatura de hospedagem nº 3
                Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
                Hóspede: Ana Souza
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                ISS retido (5%): R$ 38,99""", r.identity().document(r).text());
    }

    @Test
    void bookingReceiptWithDiscountAndGrouping() {
        Reservation r = reservation(4, 301, new PassportIdentity("X1234567", "United Kingdom"), "John Smith",
                "2027-03-10", "2027-03-14", RatePlan.NON_REFUNDABLE);
        assertEquals("""
                Booking receipt no. 4
                Guest: John Smith (passport X1234567, United Kingdom)
                Room: 301 (SUITE)
                Check-in: 2027-03-10 | Check-out: 2027-03-14 | 4 nights
                Nights: BRL 2,319.60
                Discount: BRL 347.94
                Total: BRL 1,971.66""", r.identity().document(r).text());
    }

    @Test
    void cancelRefunds() {
        Reservation r = reservation(1, 101, new CpfIdentity("1"), "A", "2027-03-10", "2027-03-11", RatePlan.STANDARD);
        r.confirm();
        r.cancel();
        assertEquals(r.amountPaid(), r.refund());
        assertEquals(Money.ZERO, r.amountKept());

        Reservation n = reservation(2, 101, new CpfIdentity("1"), "A", "2027-03-10", "2027-03-11",
                RatePlan.NON_REFUNDABLE);
        n.confirm();
        n.cancel();
        assertEquals(Money.ZERO, n.refund());
        assertEquals(n.quote().total(), n.amountKept());
    }
}
