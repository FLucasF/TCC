package pousada;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pousada.document.*;
import pousada.domain.Money;
import pousada.domain.RoomType;

class DocumentTest {
    private DocumentData data(long id, String guest, String subtotal, String discount, String total, int nights) {
        return new DocumentData(id, guest, 201, RoomType.DELUXE, LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 10).plusDays(nights), nights, Money.of(subtotal), Money.of(discount),
                Money.of(total));
    }

    @Test
    void cpfReceipt() {
        var id = Map.of("guestCpf", "111.222.333-44");
        String text = DocumentTypes.byCode("CPF").render(data(7, "Carla Dias", "779.80", "0", "779.80", 2), id);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", text);
    }

    @Test
    void companyInvoiceHasIss() {
        var id = Map.of("companyCnpj", "12.345.678/0001-90", "companyName", "Tecnosul Ltda");
        String text = DocumentTypes.byCode("CNPJ").render(data(3, "Ana Souza", "779.80", "0", "779.80", 2), id);
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
    void passportReceiptWithDiscountAndOneNight() {
        var id = Map.of("guestPassport", "X1234567", "guestCountry", "United Kingdom");
        String text = DocumentTypes.byCode("PASSPORT")
                .render(data(4, "John Smith", "1234.56", "185.18", "1049.38", 1), id);
        assertEquals("""
                Booking receipt no. 4
                Guest: John Smith (passport X1234567, United Kingdom)
                Room: 201 (DELUXE)
                Check-in: 2027-03-10 | Check-out: 2027-03-11 | 1 night
                Nights: BRL 1,234.56
                Discount: BRL 185.18
                Total: BRL 1,049.38""", text);
    }

    private Map<String, String> given(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    @Test
    void identityMustBeExactlyOneKind() {
        assertEquals("CPF", DocumentTypes.resolve(given("guestCpf", "1")).type());
        assertEquals("CNPJ", DocumentTypes.resolve(given("companyCnpj", "1", "companyName", "A")).type());
        assertEquals("PASSPORT", DocumentTypes.resolve(given("guestPassport", "1", "guestCountry", "BR")).type());
        assertThrows(ApiException.class, () -> DocumentTypes.resolve(given()));
        assertThrows(ApiException.class, () -> DocumentTypes.resolve(given("guestCpf", "1", "companyCnpj", "2", "companyName", "A")));
        assertThrows(ApiException.class, () -> DocumentTypes.resolve(given("companyCnpj", "1")));
        assertThrows(ApiException.class, () -> DocumentTypes.resolve(given("guestPassport", "1")));
        assertThrows(ApiException.class, () -> DocumentTypes.resolve(given("guestCpf", "  ")));
    }
}
