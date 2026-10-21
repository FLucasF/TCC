package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.marealta.dominio.ApiException;
import br.com.marealta.dominio.Cobranca;
import br.com.marealta.dominio.Dinheiro;
import br.com.marealta.dominio.Estadia;
import br.com.marealta.dominio.PlanosDeTarifa;
import br.com.marealta.dominio.Quarto;
import br.com.marealta.dominio.TipoQuarto;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PlanosDeTarifaTest {

    private static final Quarto STANDARD = new Quarto(101, TipoQuarto.STANDARD);
    private static final Quarto DELUXE = new Quarto(201, TipoQuarto.DELUXE);

    private static Cobranca cobrar(String plano, Quarto q, String entrada, String saida, String empresa) {
        return PlanosDeTarifa.porCodigo(plano)
                .cobrar(new Estadia(q, LocalDate.parse(entrada), LocalDate.parse(saida), empresa));
    }

    @Test
    void altaTemporada() {
        Cobranca c = cobrar("STANDARD", STANDARD, "2027-07-10", "2027-07-12", null);
        assertEquals(Dinheiro.valor("599.76"), c.total());
    }

    @Test
    void naoReembolsavel() {
        Cobranca c = cobrar("NON_REFUNDABLE", DELUXE, "2027-03-10", "2027-03-12", null);
        assertEquals(Dinheiro.valor("116.97"), c.desconto());
    }

    @Test
    void convenio() {
        Cobranca c = cobrar("CORPORATE", DELUXE, "2027-03-10", "2027-03-12", "TECNOSUL");
        assertEquals(Dinheiro.valor("779.80"), c.diarias());
        assertEquals(Dinheiro.valor("93.58"), c.desconto());
        assertEquals(Dinheiro.valor("686.22"), c.total());
    }

    @Test
    void convenioExigeEmpresaConhecida() {
        assertThrows(ApiException.class, () -> cobrar("CORPORATE", DELUXE, "2027-03-10", "2027-03-12", null));
        assertThrows(ApiException.class, () -> cobrar("CORPORATE", DELUXE, "2027-03-10", "2027-03-12", "tecnosul"));
        assertThrows(ApiException.class, () -> cobrar("CORPORATE", DELUXE, "2027-03-10", "2027-03-12", "OUTRA"));
    }

    @Test
    void pacoteDeFimDeSemana() {
        Cobranca c = cobrar("WEEKEND_PACKAGE", DELUXE, "2027-03-12", "2027-03-14", null);
        assertEquals(Dinheiro.valor("820.00"), c.total());
        assertEquals(Dinheiro.valor("410.00"),
                PlanosDeTarifa.porCodigo("WEEKEND_PACKAGE").devolucao(c.total()));
        // preço fechado também na alta temporada
        assertEquals(Dinheiro.valor("520.00"),
                cobrar("WEEKEND_PACKAGE", STANDARD, "2027-07-09", "2027-07-11", null).total());
    }

    @Test
    void pacoteSoEmSextaADomingo() {
        assertThrows(ApiException.class, () -> cobrar("WEEKEND_PACKAGE", DELUXE, "2027-03-13", "2027-03-15", null));
        assertThrows(ApiException.class, () -> cobrar("WEEKEND_PACKAGE", DELUXE, "2027-03-12", "2027-03-15", null));
        assertThrows(ApiException.class, () -> cobrar("WEEKEND_PACKAGE", DELUXE, "2027-03-12", "2027-03-13", null));
    }

    @Test
    void estadiaLonga() {
        Cobranca c = cobrar("LONG_STAY", STANDARD, "2027-03-01", "2027-03-08", null);
        assertEquals(Dinheiro.valor("1749.30"), c.diarias());
        assertEquals(Dinheiro.valor("174.93"), c.desconto());
        assertEquals(Dinheiro.valor("1574.37"), c.total());
        assertThrows(ApiException.class, () -> cobrar("LONG_STAY", STANDARD, "2027-03-01", "2027-03-07", null));
    }
}
