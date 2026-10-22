package br.com.maralta.pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DocumentTest {
    private static Document doc(Identification id, String name, long number, String total, String discount,
                                String subtotal, int nights) {
        return new Document(number, name, id, 201, RoomType.DELUXE, LocalDate.parse("2027-03-10"),
                LocalDate.parse("2027-03-12"), nights, Money.of(subtotal), Money.of(discount), Money.of(total));
    }

    @Test
    void comprovante() {
        String text = doc(new CpfIdentification("111.222.333-44"), "Carla Dias", 7, "779.80", "0", "779.80", 2).text();
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", text);
    }

    @Test
    void fatura() {
        String text = doc(new CnpjIdentification("12.345.678/0001-90", "Tecnosul Ltda"), "Ana Souza", 3,
                "779.80", "0", "779.80", 2).text();
        assertEquals("""
                Fatura de hospedagem nº 3
                Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
                Hóspede: Ana Souza
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                ISS retido (5%): R$ 38,99""", text);
    }

    @Test
    void bookingReceiptWithDiscountAndThousands() {
        String text = doc(new PassportIdentification("X1234567", "United Kingdom"), "John Smith", 4,
                "1105.00", "195.00", "1300.00", 1).text();
        assertEquals("""
                Booking receipt no. 4
                Guest: John Smith (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-12 | 1 night
                Nights: BRL 1,300.00
                Discount: BRL 195.00
                Total: BRL 1,105.00""", text);
    }

    @Test
    void pricing() {
        Pricing.Quote q = Pricing.quote(Rooms.get(101), LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"),
                RatePlan.STANDARD);
        assertEquals(new BigDecimal("599.76"), q.total());
        Pricing.Quote n = Pricing.quote(Rooms.get(101), LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"),
                RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("89.96"), n.discount());
        assertEquals(new BigDecimal("509.80"), n.total());
    }
}
