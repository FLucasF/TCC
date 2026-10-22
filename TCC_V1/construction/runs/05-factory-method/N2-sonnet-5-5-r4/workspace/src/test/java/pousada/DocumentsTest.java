package pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DocumentsTest {
    private static final Room DELUXE = new Room(201, "DELUXE", 3, new BigDecimal("389.90"));
    private static final Room STANDARD = new Room(101, "STANDARD", 2, new BigDecimal("249.90"));

    private Reservation reservation(long id, Room room, String in, String out, RatePlan plan, String type,
                                    Map<String, String> values, String name) {
        Pricing pricing = Pricing.quote(room, LocalDate.parse(in), LocalDate.parse(out), plan);
        return new Reservation(id, room, name, "a@b.c", new Identification(IdentificationTypes.byCode(type), values),
                LocalDate.parse(in), LocalDate.parse(out), 2, plan, pricing, Status.CONFIRMED,
                pricing.total(), null);
    }

    @Test
    void highSeasonPricing() {
        Pricing p = Pricing.quote(STANDARD, LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"),
                RatePlan.STANDARD);
        assertEquals(new BigDecimal("599.76"), p.total());
        assertEquals(2, p.nights());
    }

    @Test
    void nonRefundableDiscount() {
        Pricing p = Pricing.quote(DELUXE, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"),
                RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("779.80"), p.subtotal());
        assertEquals(new BigDecimal("116.97"), p.discount());
        assertEquals(new BigDecimal("662.83"), p.total());
    }

    @Test
    void cpfReceipt() {
        Reservation r = reservation(7, DELUXE, "2027-03-10", "2027-03-12", RatePlan.STANDARD, "CPF",
                Map.of("guestCpf", "111.222.333-44"), "Carla Dias");
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", r.identification().type().document().render(r));
    }

    @Test
    void invoice() {
        Reservation r = reservation(3, DELUXE, "2027-03-10", "2027-03-12", RatePlan.STANDARD, "CNPJ",
                Map.of("companyCnpj", "12.345.678/0001-90", "companyName", "Tecnosul Ltda"), "Ana Souza");
        assertEquals("""
                Fatura de hospedagem nº 3
                Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
                Hóspede: Ana Souza
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80
                ISS retido (5%): R$ 38,99""", r.identification().type().document().render(r));
    }

    @Test
    void bookingReceiptWithDiscountAndOneNight() {
        Reservation r = reservation(4, DELUXE, "2027-03-10", "2027-03-11", RatePlan.NON_REFUNDABLE, "PASSPORT",
                Map.of("guestPassport", "X1234567", "guestCountry", "United Kingdom"), "John Smith");
        assertEquals("""
                Booking receipt no. 4
                Guest: John Smith (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-11 | 1 night
                Nights: BRL 389.90
                Discount: BRL 58.48
                Total: BRL 331.42""", r.identification().type().document().render(r));
    }

    @Test
    void identificationMustBeExactlyOneKind() throws Exception {
        ObjectMapper m = new ObjectMapper();
        assertEquals("CNPJ", IdentificationTypes.fromRequest(
                m.readTree("{\"companyCnpj\":\"1\",\"companyName\":\"X\"}")).type().code());
        assertThrows(ApiException.class, () -> IdentificationTypes.fromRequest(m.readTree("{}")));
        assertThrows(ApiException.class, () -> IdentificationTypes.fromRequest(m.readTree("{\"companyCnpj\":\"1\"}")));
        assertThrows(ApiException.class, () -> IdentificationTypes.fromRequest(
                m.readTree("{\"guestCpf\":\"1\",\"guestPassport\":\"2\",\"guestCountry\":\"BR\"}")));
    }
}
