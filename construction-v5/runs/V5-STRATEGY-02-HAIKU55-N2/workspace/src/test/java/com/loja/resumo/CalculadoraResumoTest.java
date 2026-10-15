package com.loja.resumo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loja.resumo.dominio.CodigoErro;
import com.loja.resumo.dominio.PedidoRecusadoException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private static final List<ItemRequest> CAMISETA_E_TENIS = List.of(
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemRequest("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

    @Test
    void exemploUmPixComCupomBemVindo() {
        ResumoCompraResponse r = calcular(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        assertValor("409.70", r.subtotalProdutos());
        assertValor("40.97", r.descontoCupom());
        assertValor("33.10", r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertValor("10.24", r.seguro());
        assertValor("-20.60", r.ajustePagamento());
        assertValor("391.47", r.totalFinal());
        assertEquals(1, r.parcelas());
        assertValor("391.47", r.valorParcela());
        assertValor("0.00", r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemploDoisCartaoSeisParcelasComJurosEClubePrata() {
        ResumoCompraResponse r = calcular(CAMISETA_E_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        assertValor("409.70", r.subtotalProdutos());
        assertValor("0.00", r.descontoCupom());
        assertValor("15.60", r.frete());
        assertEquals(7, r.prazoEntregaDias());
        assertValor("6.15", r.seguro());
        assertValor("30.55", r.ajustePagamento());
        assertValor("462.00", r.totalFinal());
        assertEquals(6, r.parcelas());
        assertValor("77.00", r.valorParcela());
        assertValor("8.19", r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemploTresMotoboyComMenos50EBoleto() {
        List<ItemRequest> itens = List.of(new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));

        ResumoCompraResponse r = calcular(itens, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        assertValor("399.80", r.subtotalProdutos());
        assertValor("50.00", r.descontoCupom());
        assertValor("18.00", r.frete());
        assertEquals(0, r.prazoEntregaDias());
        assertValor("8.00", r.seguro());
        assertValor("3.49", r.ajustePagamento());
        assertValor("379.29", r.totalFinal());
        assertEquals(1, r.parcelas());
        assertValor("379.29", r.valorParcela());
        assertValor("0.00", r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemploQuatroCartaoTresParcelasComLeve3Pague2() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));

        ResumoCompraResponse r = calcular(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        assertValor("299.10", r.subtotalProdutos());
        assertValor("39.80", r.descontoCupom());
        assertValor("0.00", r.frete());
        assertEquals(1, r.prazoEntregaDias());
        assertValor("2.99", r.seguro());
        assertValor("0.00", r.ajustePagamento());
        assertValor("262.29", r.totalFinal());
        assertEquals(3, r.parcelas());
        assertValor("87.43", r.valorParcela());
        assertValor("5.98", r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemploCincoOuroFreteZeroPixEBrindeAcimaDeQuinhentos() {
        ResumoCompraResponse r = calcular(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        assertValor("409.70", r.subtotalProdutos());
        assertValor("0.00", r.descontoCupom());
        assertValor("0.00", r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertValor("4.10", r.seguro());
        assertValor("-20.69", r.ajustePagamento());
        assertValor("393.11", r.totalFinal());
        assertValor("20.48", r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void ouroComSubtotalAcimaDeQuinhentosGanhaBrinde() {
        List<ItemRequest> itens = List.of(new ItemRequest("Casaco", new BigDecimal("520.00"), 1, new BigDecimal("1.00")));

        ResumoCompraResponse r = calcular(itens, "MOTOBOY", null, "PIX", null, "OURO", "SUDESTE");

        assertTrue(r.brinde());
    }

    @Test
    void cupomFreteGratisTemDescontoIgualAoFrete() {
        ResumoCompraResponse r = calcular(CAMISETA_E_TENIS, "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE");

        assertValor("33.10", r.frete());
        assertValor("33.10", r.descontoCupom());
    }

    @Test
    void pedidoSemItensEhRecusado() {
        assertErro(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUL", CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComQuantidadeZeroEhRecusado() {
        List<ItemRequest> itens = List.of(new ItemRequest("X", new BigDecimal("10.00"), 0, new BigDecimal("0.10")));
        assertErro(itens, "EXPRESSA", null, "PIX", null, "BRONZE", "SUL", CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemSemPesoEhRecusado() {
        List<ItemRequest> itens = List.of(new ItemRequest("X", new BigDecimal("10.00"), 1, null));
        assertErro(itens, "EXPRESSA", null, "PIX", null, "BRONZE", "SUL", CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivelDeClubeInvalidoEhRecusado() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", null, "PLATINA", "SUL", CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoAusenteEhRecusada() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", null, "BRONZE", null, CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInexistenteEhRecusada() {
        assertErro(CAMISETA_E_TENIS, "DRONE", null, "PIX", null, "BRONZE", "SUL", CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDeCincoKgEhIndisponivel() {
        List<ItemRequest> itens = List.of(new ItemRequest("Jaqueta", new BigDecimal("300.00"), 1, new BigDecimal("6.00")));
        assertErro(itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUL", CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInexistenteEhRecusado() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", "bemvindo10", "PIX", null, "BRONZE", "SUL", CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void menos50AbaixoDeTrezentosNaoEhAplicavel() {
        List<ItemRequest> itens = List.of(new ItemRequest("Meia", new BigDecimal("19.90"), 2, new BigDecimal("0.10")));
        assertErro(itens, "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUL", CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaDePagamentoInexistenteEhRecusada() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", null, "DINHEIRO", null, "BRONZE", "SUL", CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void pixEmDuasParcelasEhRecusado() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUL", CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoAcimaDeDozeParcelasEhRecusado() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUL", CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDeMilEhIndisponivel() {
        List<ItemRequest> itens = List.of(new ItemRequest("Casaco", new BigDecimal("990.00"), 1, new BigDecimal("1.00")));
        assertErro(itens, "EXPRESSA", null, "BOLETO", null, "BRONZE", "SUL", CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void primeiroProblemaEncontradoEhDevolvido() {
        List<ItemRequest> itens = List.of(new ItemRequest("Jaqueta", new BigDecimal("300.00"), 1, new BigDecimal("6.00")));
        assertErro(itens, "MOTOBOY", "CUPOM_QUE_NAO_EXISTE", "DINHEIRO", null, "PLATINA", "XX", CodigoErro.NIVEL_CLUBE_INVALIDO);
        assertErro(itens, "MOTOBOY", "CUPOM_QUE_NAO_EXISTE", "DINHEIRO", null, "BRONZE", "SUL", CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    private ResumoCompraResponse calcular(List<ItemRequest> itens, String modalidade, String cupom,
                                          String pagamento, Integer parcelas, String nivel, String regiao) {
        return calculadora.calcular(new ResumoCompraRequest(itens, modalidade, cupom, pagamento, parcelas, nivel, regiao));
    }

    private void assertErro(List<ItemRequest> itens, String modalidade, String cupom, String pagamento,
                            Integer parcelas, String nivel, String regiao, CodigoErro esperado) {
        PedidoRecusadoException excecao = assertThrows(PedidoRecusadoException.class,
                () -> calcular(itens, modalidade, cupom, pagamento, parcelas, nivel, regiao));
        assertEquals(esperado, excecao.getCodigo());
    }

    private static void assertValor(String esperado, BigDecimal real) {
        assertEquals(new BigDecimal(esperado), real);
    }
}
