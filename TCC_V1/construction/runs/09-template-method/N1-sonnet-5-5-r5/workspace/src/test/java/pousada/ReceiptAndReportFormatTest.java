package pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import pousada.domain.ConfirmationEmail;
import pousada.domain.Receipt;
import pousada.domain.ReceiptText;
import pousada.domain.RoomType;
import pousada.report.CsvReportWriter;
import pousada.report.Report;
import pousada.report.ReportContent;

class ReceiptAndReportFormatTest {
    private final Receipt receipt = new Receipt("Comprovante de reserva", 7, "Carla Dias", "111.222.333-44", 201,
            RoomType.DELUXE, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2,
            new BigDecimal("779.80"), new BigDecimal("0.00"), new BigDecimal("779.80"));

    private static final String TEXT = """
            Comprovante de reserva nº 7
            Hóspede: Carla Dias (CPF 111.222.333-44)
            Quarto: 201 (DELUXE)
            Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
            Diárias: R$ 779,80
            Total: R$ 779,80""";

    @Test
    void receiptText() {
        assertEquals(TEXT, ReceiptText.of(receipt));
    }

    @Test
    void receiptWithDiscountAndOneNight() {
        Receipt r = new Receipt("Comprovante de reserva", 8, "A", "1", 101, RoomType.STANDARD,
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), 1,
                new BigDecimal("1000.00"), new BigDecimal("150.00"), new BigDecimal("850.00"));
        assertEquals("""
                Comprovante de reserva nº 8
                Hóspede: A (CPF 1)
                Quarto: 101 (STANDARD)
                Entrada: 10/03/2027 | Saída: 11/03/2027 | 1 diária
                Diárias: R$ 1.000,00
                Desconto: R$ 150,00
                Total: R$ 850,00""", ReceiptText.of(r));
    }

    @Test
    void confirmationEmailBody() {
        assertEquals("Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + TEXT + "\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!", ConfirmationEmail.body(receipt));
    }

    @Test
    void csvHasTitleHeaderRowsAndTotal() {
        Report report = new Report() {
            public String name() { return "demo"; }
            public String parameter() { return "month"; }
            public ReportContent generate(String period) { return null; }
        };
        var content = new ReportContent("2027-07", List.of("a", "b"), List.of(List.of("1", "2")), List.of("total", "3"));
        assertEquals("Pousada Maré Alta,demo,2027-07\na,b\n1,2\ntotal,3\n", new CsvReportWriter().write(report, content));
    }
}
