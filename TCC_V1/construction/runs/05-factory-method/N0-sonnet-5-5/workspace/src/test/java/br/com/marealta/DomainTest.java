package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DomainTest {
    private static final Room STANDARD = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));
    private static final Room DELUXE = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));

    private static Reservation reservation(long id, Room room, Identification ident, String in, String out,
                                           RatePlan plan, String guest) {
        Pricing.Quote q = Pricing.quote(room.nightlyRate(), LocalDate.parse(in), LocalDate.parse(out), plan);
        return new Reservation(id, room.number(), guest, "x@y.com", ident, LocalDate.parse(in), LocalDate.parse(out),
                1, plan, q.nights(), q.subtotal(), q.discount(), q.total(), Status.CONFIRMED, q.total(), null);
    }

    @Test
    void highSeasonBoundaries() {
        assertEquals(false, Pricing.isHighSeason(LocalDate.parse("2027-12-14")));
        assertEquals(true, Pricing.isHighSeason(LocalDate.parse("2027-12-15")));
        assertEquals(true, Pricing.isHighSeason(LocalDate.parse("2028-02-29")));
        assertEquals(false, Pricing.isHighSeason(LocalDate.parse("2027-03-01")));
        assertEquals(true, Pricing.isHighSeason(LocalDate.parse("2027-07-31")));
    }

    @Test
    void highSeasonQuote() {
        Pricing.Quote q = Pricing.quote(STANDARD.nightlyRate(), LocalDate.parse("2027-07-10"),
                LocalDate.parse("2027-07-12"), RatePlan.STANDARD);
        assertEquals(new BigDecimal("599.76"), q.total());
        assertEquals(2, q.nights());
    }

    @Test
    void nonRefundableDiscountRoundsHalfEven() {
        // 2 noites de 299,88 = 599,76; 15% = 89,964 -> 89,96
        Pricing.Quote q = Pricing.quote(STANDARD.nightlyRate(), LocalDate.parse("2027-07-10"),
                LocalDate.parse("2027-07-12"), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("89.96"), q.discount());
        assertEquals(new BigDecimal("509.80"), q.total());
    }

    @Test
    void moneyFormats() {
        assertEquals("R$ 1.234,56", Money.brl(new BigDecimal("1234.56")));
        assertEquals("BRL 1,234.56", Money.en(new BigDecimal("1234.56")));
        assertEquals(new BigDecimal("0.12"), Money.round(new BigDecimal("0.125")));
    }

    @Test
    void receiptText() {
        Reservation r = reservation(7, DELUXE, new Identification.Cpf("111.222.333-44"), "2027-03-10", "2027-03-12",
                RatePlan.STANDARD, "Carla Dias");
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", Documents.render(r, DELUXE).text());
    }

    @Test
    void invoiceText() {
        Reservation r = reservation(3, DELUXE, new Identification.Company("12.345.678/0001-90", "Tecnosul Ltda"),
                "2027-03-10", "2027-03-12", RatePlan.STANDARD, "Ana Souza");
        assertEquals("""
                Fatura de hospedagem nº 3
                Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
                Hóspede: Ana Souza
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                ISS retido (5%): R$ 38,99""", Documents.render(r, DELUXE).text());
    }

    @Test
    void bookingReceiptTextWithDiscountAndSingleNight() {
        Reservation r = reservation(4, DELUXE, new Identification.Passport("X1234567", "United Kingdom"),
                "2027-03-10", "2027-03-11", RatePlan.NON_REFUNDABLE, "John Smith");
        assertEquals("""
                Booking receipt no. 4
                Guest: John Smith (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-11 | 1 night
                Nights: BRL 389.90
                Discount: BRL 58.48
                Total: BRL 331.42""", Documents.render(r, DELUXE).text());
    }

    @Test
    void confirmationEmailWrapsDocument() {
        Reservation r = reservation(7, DELUXE, new Identification.Cpf("111.222.333-44"), "2027-03-10", "2027-03-12",
                RatePlan.STANDARD, "Carla Dias");
        String body = Documents.confirmationBody(r, Documents.render(r, DELUXE));
        assertEquals(true, body.startsWith("Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. "
                + "Estes são os dados dela:\n\nComprovante de reserva nº 7\n"));
        assertEquals(true, body.endsWith("Total: R$ 779,80\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!"));
    }
}
