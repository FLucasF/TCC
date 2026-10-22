package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.marealta.Documents.Document;
import br.com.marealta.Model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DomainTest {
    private static final Room DELUXE = new Room(201, "DELUXE", 3, Money.of("389.90"));

    private static Reservation res(Identification id, String in, String out, RatePlan plan) {
        Pricing.Quote q = Pricing.quote(DELUXE.nightlyRate(), LocalDate.parse(in), LocalDate.parse(out), plan);
        return new Reservation(7, 201, "Carla Dias", "c@x.com", id, LocalDate.parse(in), LocalDate.parse(out), 2, plan,
                q.nights(), q.subtotal(), q.discount(), q.total(), Status.CONFIRMED, q.total(), null);
    }

    @Test
    void highSeasonExample() {
        Pricing.Quote q = Pricing.quote(Money.of("249.90"), LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"), RatePlan.STANDARD);
        assertEquals(Money.of("599.76"), q.total());
        assertEquals(2, q.nights());
    }

    @Test
    void seasonBoundaries() {
        assertEquals(false, Pricing.isHighSeason(LocalDate.parse("2027-12-14")));
        assertEquals(true, Pricing.isHighSeason(LocalDate.parse("2027-12-15")));
        assertEquals(true, Pricing.isHighSeason(LocalDate.parse("2028-02-29")));
        assertEquals(false, Pricing.isHighSeason(LocalDate.parse("2028-03-01")));
        assertEquals(false, Pricing.isHighSeason(LocalDate.parse("2027-06-30")));
        assertEquals(false, Pricing.isHighSeason(LocalDate.parse("2027-08-01")));
    }

    @Test
    void nonRefundableDiscountRoundsHalfEven() {
        // 249.90 * 1 noite = 249.90; 15% = 37.485 -> 37.48 (meio para o par)
        Pricing.Quote q = Pricing.quote(Money.of("249.90"), LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("37.48"), q.discount());
        assertEquals(Money.of("212.42"), q.total());
    }

    @Test
    void moneyFormats() {
        assertEquals("1.234,56", Money.pt(new BigDecimal("1234.56")));
        assertEquals("1,234.56", Money.en(new BigDecimal("1234.56")));
        assertEquals("0,00", Money.pt(BigDecimal.ZERO));
    }

    @Test
    void receipt() {
        Reservation r = res(new Identification.Cpf("111.222.333-44"), "2027-03-10", "2027-03-12", RatePlan.STANDARD);
        Document d = Documents.render(r, DELUXE);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", d.text());
        assertEquals("""
                Olá, Carla Dias!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                %s

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!""".formatted(d.text()), Documents.confirmationEmailBody(r, d));
    }

    @Test
    void invoiceWithDiscount() {
        Reservation r = res(new Identification.Company("12.345.678/0001-90", "Tecnosul Ltda"), "2027-03-10", "2027-03-12", RatePlan.NON_REFUNDABLE);
        assertEquals("""
                Fatura de hospedagem nº 7
                Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
                Hóspede: Carla Dias
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Desconto: R$ 116,97
                Total: R$ 662,83
                ISS retido (5%): R$ 33,14""", Documents.render(r, DELUXE).text());
    }

    @Test
    void bookingReceiptOneNight() {
        Reservation r = res(new Identification.Passport("X1234567", "United Kingdom"), "2027-03-10", "2027-03-11", RatePlan.STANDARD);
        assertEquals("""
                Booking receipt no. 7
                Guest: Carla Dias (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-11 | 1 night
                Nights: BRL 389.90
                Total: BRL 389.90""", Documents.render(r, DELUXE).text());
    }
}
