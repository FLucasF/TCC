package pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricingAndDocumentTest {
    private static Reservation res(long id, int room, String name, Identification i, String in, String out,
            RatePlan plan) {
        LocalDate a = LocalDate.parse(in), b = LocalDate.parse(out);
        Pricing.Quote q = Pricing.quote(Room.find(room).orElseThrow().type(), a, b, plan);
        return new Reservation(id, room, name, "x@y.z", i, a, b, 2, plan, q.nights(), q.subtotal(), q.discount(),
                q.total(), Status.CONFIRMED, q.total(), null);
    }

    @Test
    void highSeasonPricing() {
        var q = Pricing.quote(RoomType.STANDARD, LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"),
                RatePlan.STANDARD);
        assertEquals(Money.of("599.76"), q.total());
        assertEquals(2, q.nights());
    }

    @Test
    void seasonBoundaries() {
        assertEquals(false, Pricing.isHighSeason(LocalDate.parse("2027-12-14")));
        assertEquals(true, Pricing.isHighSeason(LocalDate.parse("2027-12-15")));
        assertEquals(true, Pricing.isHighSeason(LocalDate.parse("2028-02-29")));
        assertEquals(false, Pricing.isHighSeason(LocalDate.parse("2028-03-01")));
    }

    @Test
    void nonRefundableDiscountRoundsHalfEven() {
        var q = Pricing.quote(RoomType.DELUXE, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"),
                RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("779.80"), q.subtotal());
        assertEquals(Money.of("116.97"), q.discount());
        assertEquals(Money.of("662.83"), q.total());
    }

    @Test
    void moneyFormats() {
        assertEquals("R$ 1.234,56", Money.pt(Money.of("1234.56")));
        assertEquals("BRL 1,234.56", Money.en(Money.of("1234.56")));
    }

    @Test
    void comprovante() {
        var r = res(7, 201, "Carla Dias", new Identification.Cpf("111.222.333-44"), "2027-03-10", "2027-03-12",
                RatePlan.STANDARD);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", r.identification().render(r));
    }

    @Test
    void fatura() {
        var r = res(3, 201, "Ana Souza", new Identification.Cnpj("12.345.678/0001-90", "Tecnosul Ltda"),
                "2027-03-10", "2027-03-12", RatePlan.STANDARD);
        assertEquals("""
                Fatura de hospedagem nº 3
                Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
                Hóspede: Ana Souza
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                ISS retido (5%): R$ 38,99""", r.identification().render(r));
    }

    @Test
    void bookingReceiptWithDiscountOneNight() {
        var r = res(4, 201, "John Smith", new Identification.Passport("X1234567", "United Kingdom"),
                "2027-03-10", "2027-03-11", RatePlan.NON_REFUNDABLE);
        assertEquals("""
                Booking receipt no. 4
                Guest: John Smith (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-11 | 1 night
                Nights: BRL 389.90
                Discount: BRL 58.48
                Total: BRL 331.42""", r.identification().render(r));
    }

    @Test
    void emailBody() {
        var r = res(7, 201, "Carla Dias", new Identification.Cpf("111.222.333-44"), "2027-03-10", "2027-03-12",
                RatePlan.STANDARD);
        String body = ReservationService.confirmationBody(r);
        assertEquals(true, body.startsWith("Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada."
                + " Estes são os dados dela:\n\nComprovante de reserva nº 7\n"));
        assertEquals(true, body.endsWith("Total: R$ 779,80\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!"));
    }
}
