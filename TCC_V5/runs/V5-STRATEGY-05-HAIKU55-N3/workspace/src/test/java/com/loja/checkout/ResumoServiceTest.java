package com.loja.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResumoServiceTest {

    private final ResumoService service = new ResumoService();

    private static final List<ResumoRequest.Item> CAMISETA_E_TENIS = List.of(
            new ResumoRequest.Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ResumoRequest.Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

    @Test
    void exemploUmComCupomPixENorte() {
        ResumoResponse r = service.calcular(new ResumoRequest(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10",
                "PIX", 1, "BRONZE", "NORTE"));

        assertEquals(new BigDecimal("409.70"), r.subtotalProdutos());
        assertEquals(new BigDecimal("40.97"), r.descontoCupom());
        assertEquals(new BigDecimal("33.10"), r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), r.seguro());
        assertEquals(new BigDecimal("-20.60"), r.ajustePagamento());
        assertEquals(new BigDecimal("391.47"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(new BigDecimal("391.47"), r.valorParcela());
        assertEquals(new BigDecimal("0.00"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemploDoisCartaoSeisVezesComJurosEClubePrata() {
        ResumoResponse r = service.calcular(new ResumoRequest(CAMISETA_E_TENIS, "ECONOMICA", null,
                "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertEquals(new BigDecimal("409.70"), r.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), r.descontoCupom());
        assertEquals(new BigDecimal("15.60"), r.frete());
        assertEquals(7, r.prazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), r.seguro());
        assertEquals(new BigDecimal("30.55"), r.ajustePagamento());
        assertEquals(new BigDecimal("462.00"), r.totalFinal());
        assertEquals(6, r.parcelas());
        assertEquals(new BigDecimal("77.00"), r.valorParcela());
        assertEquals(new BigDecimal("8.19"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemploTresBoletoComMotoboyEMenos50() {
        List<ResumoRequest.Item> itens = List.of(
                new ResumoRequest.Item("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));

        ResumoResponse r = service.calcular(new ResumoRequest(itens, "MOTOBOY", "MENOS50",
                "BOLETO", null, "BRONZE", "NORDESTE"));

        assertEquals(new BigDecimal("399.80"), r.subtotalProdutos());
        assertEquals(new BigDecimal("50.00"), r.descontoCupom());
        assertEquals(new BigDecimal("18.00"), r.frete());
        assertEquals(0, r.prazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), r.seguro());
        assertEquals(new BigDecimal("3.49"), r.ajustePagamento());
        assertEquals(new BigDecimal("379.29"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(new BigDecimal("379.29"), r.valorParcela());
    }

    @Test
    void exemploQuatroRetiradaComLeve3Pague2ECartaoEmTresVezes() {
        List<ResumoRequest.Item> itens = List.of(
                new ResumoRequest.Item("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ResumoRequest.Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));

        ResumoResponse r = service.calcular(new ResumoRequest(itens, "RETIRADA_LOJA", "LEVE3PAGUE2",
                "CARTAO", 3, "PRATA", "SUL"));

        assertEquals(new BigDecimal("299.10"), r.subtotalProdutos());
        assertEquals(new BigDecimal("39.80"), r.descontoCupom());
        assertEquals(new BigDecimal("0.00"), r.frete());
        assertEquals(1, r.prazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), r.seguro());
        assertEquals(new BigDecimal("0.00"), r.ajustePagamento());
        assertEquals(new BigDecimal("262.29"), r.totalFinal());
        assertEquals(3, r.parcelas());
        assertEquals(new BigDecimal("87.43"), r.valorParcela());
        assertEquals(new BigDecimal("5.98"), r.creditoProximaCompra());
    }

    @Test
    void exemploCincoOuroSemFreteComBrinde() {
        ResumoResponse r = service.calcular(new ResumoRequest(CAMISETA_E_TENIS, "EXPRESSA", null,
                "PIX", null, "OURO", "SUDESTE"));

        assertEquals(new BigDecimal("409.70"), r.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), r.frete());
        assertEquals(new BigDecimal("4.10"), r.seguro());
        assertEquals(new BigDecimal("-20.69"), r.ajustePagamento());
        assertEquals(new BigDecimal("393.11"), r.totalFinal());
        assertEquals(new BigDecimal("20.48"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void ourooComProdutosAcimaDe500GanhaBrinde() {
        List<ResumoRequest.Item> itens = List.of(
                new ResumoRequest.Item("Bolsa", new BigDecimal("600.00"), 1, new BigDecimal("0.50")));

        ResumoResponse r = service.calcular(new ResumoRequest(itens, "ECONOMICA", null,
                "PIX", null, "OURO", "SUDESTE"));

        assertTrue(r.brinde());
    }

    @Test
    void itemSemPesoEhPedidoInvalido() {
        List<ResumoRequest.Item> itens = List.of(
                new ResumoRequest.Item("Boné", new BigDecimal("50.00"), 1, null));

        assertErro("PEDIDO_INVALIDO", new ResumoRequest(itens, "ECONOMICA", null, "PIX", null, "BRONZE", "SUL"));
    }

    @Test
    void carrinhoVazioEhPedidoInvalido() {
        assertErro("PEDIDO_INVALIDO", new ResumoRequest(List.of(), "ECONOMICA", null, "PIX", null, "BRONZE", "SUL"));
    }

    @Test
    void motoboyAcimaDe5KgEhIndisponivel() {
        List<ResumoRequest.Item> itens = List.of(
                new ResumoRequest.Item("Sofá", new BigDecimal("900.00"), 1, new BigDecimal("6.00")));

        assertErro("MODALIDADE_INDISPONIVEL", new ResumoRequest(itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUL"));
    }

    @Test
    void cupomInexistenteEhInvalido() {
        assertErro("CUPOM_INVALIDO", new ResumoRequest(CAMISETA_E_TENIS, "ECONOMICA", "NAOEXISTE",
                "PIX", null, "BRONZE", "SUL"));
    }

    @Test
    void menos50AbaixoDe300NaoSeAplica() {
        List<ResumoRequest.Item> itens = List.of(
                new ResumoRequest.Item("Meia", new BigDecimal("19.90"), 1, new BigDecimal("0.10")));

        assertErro("CUPOM_NAO_APLICAVEL", new ResumoRequest(itens, "ECONOMICA", "MENOS50",
                "PIX", null, "BRONZE", "SUL"));
    }

    @Test
    void pixNaoParcela() {
        assertErro("PARCELAMENTO_INVALIDO", new ResumoRequest(CAMISETA_E_TENIS, "ECONOMICA", null,
                "PIX", 2, "BRONZE", "SUL"));
    }

    @Test
    void boletoAcimaDe1000NaoEhPermitido() {
        List<ResumoRequest.Item> itens = List.of(
                new ResumoRequest.Item("Bolsa", new BigDecimal("1200.00"), 1, new BigDecimal("0.80")));

        assertErro("FORMA_PAGAMENTO_INDISPONIVEL", new ResumoRequest(itens, "RETIRADA_LOJA", null,
                "BOLETO", null, "BRONZE", "SUDESTE"));
    }

    @Test
    void motoboyComExatamente5KgEhPermitido() {
        List<ResumoRequest.Item> itens = List.of(
                new ResumoRequest.Item("Caixa", new BigDecimal("100.00"), 1, new BigDecimal("5.00")));

        ResumoResponse r = service.calcular(new ResumoRequest(itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUL"));

        assertEquals(new BigDecimal("18.00"), r.frete());
    }

    @Test
    void ourooComExatamente500NaoGanhaBrinde() {
        List<ResumoRequest.Item> itens = List.of(
                new ResumoRequest.Item("Bolsa", new BigDecimal("500.00"), 1, new BigDecimal("0.50")));

        ResumoResponse r = service.calcular(new ResumoRequest(itens, "ECONOMICA", null, "PIX", null, "OURO", "SUDESTE"));

        assertFalse(r.brinde());
    }

    @Test
    void menos50ComExatamente300EAplicavel() {
        List<ResumoRequest.Item> itens = List.of(
                new ResumoRequest.Item("Bolsa", new BigDecimal("300.00"), 1, new BigDecimal("0.50")));

        ResumoResponse r = service.calcular(new ResumoRequest(itens, "ECONOMICA", "MENOS50", "PIX", null, "BRONZE", "SUL"));

        assertEquals(new BigDecimal("50.00"), r.descontoCupom());
    }

    @Test
    void freteGratisDaDescontoIgualAoFrete() {
        List<ResumoRequest.Item> itens = List.of(
                new ResumoRequest.Item("Cinto", new BigDecimal("100.00"), 1, new BigDecimal("1.00")));

        ResumoResponse r = service.calcular(new ResumoRequest(itens, "ECONOMICA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUL"));

        assertEquals(new BigDecimal("14.00"), r.frete());
        assertEquals(new BigDecimal("14.00"), r.descontoCupom());
    }

    @Test
    void leve3Pague2SemTresUnidadesNaoEAplicavel() {
        List<ResumoRequest.Item> itens = List.of(
                new ResumoRequest.Item("Meia", new BigDecimal("50.00"), 2, new BigDecimal("0.10")));

        assertErro("CUPOM_NAO_APLICAVEL", new ResumoRequest(itens, "ECONOMICA", "LEVE3PAGUE2", "PIX", null, "BRONZE", "SUL"));
    }

    @Test
    void cartaoComTrezeParcelasEhInvalido() {
        assertErro("PARCELAMENTO_INVALIDO", new ResumoRequest(CAMISETA_E_TENIS, "ECONOMICA", null,
                "CARTAO", 13, "BRONZE", "SUL"));
    }

    @Test
    void nivelInvalidoTemPrecedenciaSobreRegiaoInvalida() {
        assertErro("NIVEL_CLUBE_INVALIDO", new ResumoRequest(CAMISETA_E_TENIS, "ECONOMICA", null,
                "PIX", null, "DIAMANTE", "XYZ"));
    }

    @Test
    void regiaoModalidadeEFormaInvalidasSaoDetectadas() {
        assertErro("REGIAO_INVALIDA", new ResumoRequest(CAMISETA_E_TENIS, "ECONOMICA", null,
                "PIX", null, "BRONZE", "XYZ"));
        assertErro("MODALIDADE_INVALIDA", new ResumoRequest(CAMISETA_E_TENIS, "DRONE", null,
                "PIX", null, "BRONZE", "SUL"));
        assertErro("FORMA_PAGAMENTO_INVALIDA", new ResumoRequest(CAMISETA_E_TENIS, "ECONOMICA", null,
                "DINHEIRO", null, "BRONZE", "SUL"));
    }

    private void assertErro(String codigo, ResumoRequest req) {
        ResumoException e = assertThrows(ResumoException.class, () -> service.calcular(req));
        assertEquals(codigo, e.getCodigo());
    }
}
