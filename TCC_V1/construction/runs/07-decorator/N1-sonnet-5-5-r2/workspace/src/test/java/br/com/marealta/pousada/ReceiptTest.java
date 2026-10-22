package br.com.marealta.pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Receipt;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReceiptTest {
    @Test
    void textMatchesSpec() {
        Receipt r = new Receipt("Comprovante de reserva", 7, "Carla Dias", "111.222.333-44", 201, "DELUXE",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, new BigDecimal("779.80"),
                new BigDecimal("0.00"), new BigDecimal("779.80"));
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", r.text());
    }

    @Test
    void moneyFormat() {
        assertEquals("R$ 1.234,56", Money.format(new BigDecimal("1234.56")));
    }
}
