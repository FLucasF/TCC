package com.loja.checkout.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loja.checkout.dominio.ErroPedidoException;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoRequest.ItemRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    @Test
    void exemplo1PixComCupomBemVindoERegiaoNorte() {
        var req = new ResumoRequest(
                List.of(item("79.90", 2, "0.30"), item("249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        assertEquals(new ResumoResponse(
                b("409.70"), b("40.97"), b("33.10"), 2, b("10.24"), b("-20.60"),
                b("391.47"), 1, b("391.47"), b("0.00"), false), calculadora.calcular(req));
    }

    @Test
    void exemplo2CartaoSeisParcelasComClubePrata() {
        var req = new ResumoRequest(
                List.of(item("79.90", 2, "0.30"), item("249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        assertEquals(new ResumoResponse(
                b("409.70"), b("0.00"), b("15.60"), 7, b("6.15"), b("30.55"),
                b("462.00"), 6, b("77.00"), b("8.19"), false), calculadora.calcular(req));
    }

    @Test
    void exemplo3MotoboyComMenos50EBoleto() {
        var req = new ResumoRequest(
                List.of(item("199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        assertEquals(new ResumoResponse(
                b("399.80"), b("50.00"), b("18.00"), 0, b("8.00"), b("3.49"),
                b("379.29"), 1, b("379.29"), b("0.00"), false), calculadora.calcular(req));
    }

    @Test
    void exemplo4RetiradaComLeve3Pague2ECartaoTresVezes() {
        var req = new ResumoRequest(
                List.of(item("19.90", 7, "0.10"), item("79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        assertEquals(new ResumoResponse(
                b("299.10"), b("39.80"), b("0.00"), 1, b("2.99"), b("0.00"),
                b("262.29"), 3, b("87.43"), b("5.98"), false), calculadora.calcular(req));
    }

    @Test
    void exemplo5OuroNaoPagaFreteEGanhaCredito() {
        var req = new ResumoRequest(
                List.of(item("79.90", 2, "0.30"), item("249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        assertEquals(new ResumoResponse(
                b("409.70"), b("0.00"), b("0.00"), 2, b("4.10"), b("-20.69"),
                b("393.11"), 1, b("393.11"), b("20.48"), false), calculadora.calcular(req));
    }

    @Test
    void ouroComMaisDeQuinhentosGanhaBrinde() {
        var req = new ResumoRequest(
                List.of(item("300.00", 2, "0.50")),
                "ECONOMICA", null, "PIX", null, "OURO", "SUL");

        assertTrue(calculadora.calcular(req).brinde());
    }

    @Test
    void freteGratisDaCupomIgualAoFrete() {
        var req = new ResumoRequest(
                List.of(item("100.00", 1, "1.00")),
                "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUL");

        var resumo = calculadora.calcular(req);

        assertEquals(b("29.50"), resumo.frete());
        assertEquals(b("29.50"), resumo.descontoCupom());
    }

    @Test
    void itensVaziosSaoPedidoInvalido() {
        assertErro("PEDIDO_INVALIDO", new ResumoRequest(
                List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUL"));
    }

    @Test
    void precoZeroEhPedidoInvalido() {
        assertErro("PEDIDO_INVALIDO", new ResumoRequest(
                List.of(item("0", 1, "1.00")), "EXPRESSA", null, "PIX", null, "BRONZE", "SUL"));
    }

    @Test
    void nivelDesconhecidoEhNivelInvalido() {
        assertErro("NIVEL_CLUBE_INVALIDO", new ResumoRequest(
                List.of(item("10.00", 1, "1.00")), "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUL"));
    }

    @Test
    void regiaoAusenteEhRegiaoInvalida() {
        assertErro("REGIAO_INVALIDA", new ResumoRequest(
                List.of(item("10.00", 1, "1.00")), "EXPRESSA", null, "PIX", null, "BRONZE", null));
    }

    @Test
    void motoboyAcimaDeCincoKgEhIndisponivel() {
        assertErro("MODALIDADE_INDISPONIVEL", new ResumoRequest(
                List.of(item("10.00", 1, "5.01")), "MOTOBOY", null, "PIX", null, "BRONZE", "SUL"));
    }

    @Test
    void motoboyComExatamenteCincoKgEhPermitido() {
        var req = new ResumoRequest(
                List.of(item("10.00", 1, "5.00")), "MOTOBOY", null, "PIX", null, "BRONZE", "SUL");

        assertEquals(0, calculadora.calcular(req).prazoEntregaDias());
    }

    @Test
    void cupomInexistenteEhCupomInvalido() {
        assertErro("CUPOM_INVALIDO", new ResumoRequest(
                List.of(item("10.00", 1, "1.00")), "EXPRESSA", "XYZ", "PIX", null, "BRONZE", "SUL"));
    }

    @Test
    void menos50AbaixoDeTrezentosEhCupomNaoAplicavel() {
        assertErro("CUPOM_NAO_APLICAVEL", new ResumoRequest(
                List.of(item("299.90", 1, "1.00")), "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUL"));
    }

    @Test
    void formaPagamentoDesconhecidaEhFormaInvalida() {
        assertErro("FORMA_PAGAMENTO_INVALIDA", new ResumoRequest(
                List.of(item("10.00", 1, "1.00")), "EXPRESSA", null, "DINHEIRO", null, "BRONZE", "SUL"));
    }

    @Test
    void pixComDuasParcelasEhParcelamentoInvalido() {
        assertErro("PARCELAMENTO_INVALIDO", new ResumoRequest(
                List.of(item("10.00", 1, "1.00")), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUL"));
    }

    @Test
    void cartaoComTrezeParcelasEhParcelamentoInvalido() {
        assertErro("PARCELAMENTO_INVALIDO", new ResumoRequest(
                List.of(item("10.00", 1, "1.00")), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUL"));
    }

    @Test
    void boletoAcimaDeMilEhFormaIndisponivel() {
        assertErro("FORMA_PAGAMENTO_INDISPONIVEL", new ResumoRequest(
                List.of(item("1000.00", 1, "1.00")), "EXPRESSA", null, "BOLETO", null, "BRONZE", "SUL"));
    }

    @Test
    void cupomVazioEhSemCupom() {
        var req = new ResumoRequest(
                List.of(item("10.00", 1, "1.00")), "EXPRESSA", "", "PIX", null, "BRONZE", "SUL");

        assertEquals(b("0.00"), calculadora.calcular(req).descontoCupom());
    }

    @Test
    void menos50ComExatamenteTrezentosEhAplicavel() {
        var req = new ResumoRequest(
                List.of(item("300.00", 1, "1.00")), "RETIRADA_LOJA", "MENOS50", "PIX", null, "BRONZE", "SUL");

        assertEquals(b("50.00"), calculadora.calcular(req).descontoCupom());
    }

    @Test
    void boletoComExatamenteMilEhAceito() {
        var req = new ResumoRequest(
                List.of(item("990.10", 1, "1.00")), "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        assertEquals(b("1003.49"), calculadora.calcular(req).totalFinal());
    }

    @Test
    void ouroComExatamenteQuinhentosNaoGanhaBrinde() {
        var req = new ResumoRequest(
                List.of(item("250.00", 2, "1.00")), "ECONOMICA", null, "PIX", null, "OURO", "SUL");

        assertFalse(calculadora.calcular(req).brinde());
    }

    @Test
    void leve3Pague2ComSeisUnidadesDaDoisGratis() {
        var req = new ResumoRequest(
                List.of(item("10.00", 6, "0.10")), "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", null, "BRONZE", "SUL");

        assertEquals(b("20.00"), calculadora.calcular(req).descontoCupom());
    }

    @Test
    void leve3Pague2ComDuasUnidadesNaoDaDesconto() {
        var req = new ResumoRequest(
                List.of(item("10.00", 2, "0.10")), "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", null, "BRONZE", "SUL");

        assertEquals(b("0.00"), calculadora.calcular(req).descontoCupom());
    }

    @Test
    void cartaoEmDozeParcelasTemJurosDaTabelaPrice() {
        var req = new ResumoRequest(
                List.of(item("100.00", 1, "1.00")), "RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUL");

        var resumo = calculadora.calcular(req);

        assertEquals(12, resumo.parcelas());
        assertEquals(resumo.totalFinal(), resumo.valorParcela().multiply(b("12")));
        assertTrue(resumo.totalFinal().compareTo(b("100.00")) > 0);
    }

    @Test
    void cartaoEmTresParcelasNaoTemJuros() {
        var req = new ResumoRequest(
                List.of(item("100.00", 1, "1.00")), "RETIRADA_LOJA", null, "CARTAO", 3, "BRONZE", "SUL");

        assertEquals(b("101.00"), calculadora.calcular(req).totalFinal());
    }

    @Test
    void cartaoComZeroParcelasEhParcelamentoInvalido() {
        assertErro("PARCELAMENTO_INVALIDO", new ResumoRequest(
                List.of(item("10.00", 1, "1.00")), "EXPRESSA", null, "CARTAO", 0, "BRONZE", "SUL"));
    }

    @Test
    void modalidadeDesconhecidaEhModalidadeInvalida() {
        assertErro("MODALIDADE_INVALIDA", new ResumoRequest(
                List.of(item("10.00", 1, "1.00")), "DRONE", null, "PIX", null, "BRONZE", "SUL"));
    }

    @Test
    void quantidadeZeroEhPedidoInvalido() {
        assertErro("PEDIDO_INVALIDO", new ResumoRequest(
                List.of(item("10.00", 0, "1.00")), "EXPRESSA", null, "PIX", null, "BRONZE", "SUL"));
    }

    @Test
    void pesoZeroEhPedidoInvalido() {
        assertErro("PEDIDO_INVALIDO", new ResumoRequest(
                List.of(item("10.00", 1, "0")), "EXPRESSA", null, "PIX", null, "BRONZE", "SUL"));
    }

    @Test
    void itemNuloEhPedidoInvalido() {
        assertErro("PEDIDO_INVALIDO", new ResumoRequest(
                Arrays.asList((ItemRequest) null), "EXPRESSA", null, "PIX", null, "BRONZE", "SUL"));
    }

    @Test
    void pedidoInvalidoTemPrecedenciaSobreNivelInvalido() {
        assertErro("PEDIDO_INVALIDO", new ResumoRequest(
                List.of(item("10.00", 0, "1.00")), "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUL"));
    }

    @Test
    void boletoAcimaDeMilComDuasParcelasEhParcelamentoInvalido() {
        assertErro("PARCELAMENTO_INVALIDO", new ResumoRequest(
                List.of(item("1000.00", 1, "1.00")), "EXPRESSA", null, "BOLETO", 2, "BRONZE", "SUL"));
    }

    private void assertErro(String codigo, ResumoRequest req) {
        var erro = assertThrows(ErroPedidoException.class, () -> calculadora.calcular(req));
        assertEquals(codigo, erro.getCodigo());
    }

    private static ItemRequest item(String preco, int quantidade, String peso) {
        return new ItemRequest("produto", b(preco), quantidade, b(peso));
    }

    private static BigDecimal b(String valor) {
        return new BigDecimal(valor);
    }
}
