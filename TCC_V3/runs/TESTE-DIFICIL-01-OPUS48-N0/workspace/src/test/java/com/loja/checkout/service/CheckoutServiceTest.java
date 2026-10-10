package com.loja.checkout.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loja.checkout.domain.CheckoutException;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.CheckoutResponse;
import com.loja.checkout.web.ItemRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckoutServiceTest {

    private final CheckoutService servico = new CheckoutService();

    private static ItemRequest item(String nome, String preco, int qtd, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static void igual(String esperado, BigDecimal real) {
        assertEquals(new BigDecimal(esperado), real);
    }

    private CheckoutException erroDe(CheckoutRequest req) {
        try {
            servico.calcular(req);
        } catch (CheckoutException e) {
            return e;
        }
        throw new AssertionError("esperava CheckoutException, mas o calculo foi concluido");
    }

    private List<ItemRequest> camisetaMaisTenis() {
        return List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20"));
    }

    @Test
    void exemplo1_expressaBemvindo10PixBronzeNorte() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaMaisTenis(), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        CheckoutResponse r = servico.calcular(req);

        igual("409.70", r.subtotalProdutos());
        igual("40.97", r.descontoCupom());
        igual("33.10", r.frete());
        assertEquals(2, r.prazoEntregaDias());
        igual("10.24", r.seguro());
        igual("-20.60", r.ajustePagamento());
        igual("391.47", r.totalFinal());
        assertEquals(1, r.parcelas());
        igual("391.47", r.valorParcela());
        igual("0.00", r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo2_economicaSemCupomCartao6xPrataCentroOeste() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaMaisTenis(), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        CheckoutResponse r = servico.calcular(req);

        igual("409.70", r.subtotalProdutos());
        igual("0.00", r.descontoCupom());
        igual("15.60", r.frete());
        assertEquals(7, r.prazoEntregaDias());
        igual("6.15", r.seguro());
        igual("30.55", r.ajustePagamento());
        igual("462.00", r.totalFinal());
        assertEquals(6, r.parcelas());
        igual("77.00", r.valorParcela());
        igual("8.19", r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo3_motoboyMenos50BoletoBronzeNordeste() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        CheckoutResponse r = servico.calcular(req);

        igual("399.80", r.subtotalProdutos());
        igual("50.00", r.descontoCupom());
        igual("18.00", r.frete());
        assertEquals(2, r.prazoEntregaDias());
        igual("8.00", r.seguro());
        igual("3.49", r.ajustePagamento());
        igual("379.29", r.totalFinal());
        assertEquals(1, r.parcelas());
        igual("379.29", r.valorParcela());
        igual("0.00", r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo4_retiradaLeve3Pague2Cartao3xPrataSul() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        CheckoutResponse r = servico.calcular(req);

        igual("299.10", r.subtotalProdutos());
        igual("39.80", r.descontoCupom());
        igual("0.00", r.frete());
        assertEquals(1, r.prazoEntregaDias());
        igual("0.00", r.seguro());
        igual("0.00", r.ajustePagamento());
        igual("259.30", r.totalFinal());
        assertEquals(3, r.parcelas());
        igual("86.43", r.valorParcela());
        igual("5.98", r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo5_expressaSemCupomPixOuroSudeste() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaMaisTenis(), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        CheckoutResponse r = servico.calcular(req);

        igual("409.70", r.subtotalProdutos());
        igual("0.00", r.descontoCupom());
        igual("0.00", r.frete());
        assertEquals(2, r.prazoEntregaDias());
        igual("4.10", r.seguro());
        igual("-20.69", r.ajustePagamento());
        igual("393.11", r.totalFinal());
        assertEquals(1, r.parcelas());
        igual("393.11", r.valorParcela());
        igual("20.48", r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void anexo_expressaBemvindo10PixOuroSudeste() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaMaisTenis(), "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE");

        CheckoutResponse r = servico.calcular(req);

        igual("409.70", r.subtotalProdutos());
        igual("40.97", r.descontoCupom());
        igual("0.00", r.frete());
        assertEquals(2, r.prazoEntregaDias());
        igual("4.10", r.seguro());
        igual("-18.64", r.ajustePagamento());
        igual("354.19", r.totalFinal());
        assertEquals(1, r.parcelas());
        igual("354.19", r.valorParcela());
        igual("20.48", r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void brinde_ouroComProdutosAcimaDe500() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Tenis", "249.90", 3, "1.20")), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        CheckoutResponse r = servico.calcular(req);

        igual("749.70", r.subtotalProdutos());
        assertTrue(r.brinde());
        igual("37.48", r.creditoProximaCompra());
    }

    @Test
    void fretegratis_descontoIgualAoFrete() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaMaisTenis(), "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE");

        CheckoutResponse r = servico.calcular(req);

        igual("33.10", r.frete());
        igual("33.10", r.descontoCupom());
    }

    @Test
    void cartao12x_comJuros() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaMaisTenis(), "RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE");

        CheckoutResponse r = servico.calcular(req);

        // retirada na loja nao tem seguro, entao o total do pedido e 409.70;
        // parcela com juros de 1,99% a.m. (tabela Price).
        igual("0.00", r.seguro());
        igual("38.72", r.valorParcela());
        igual("464.64", r.totalFinal());
        igual("54.94", r.ajustePagamento());
    }

    @Test
    void erro_carrinhoVazio() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("PEDIDO_INVALIDO", erroDe(req).getCodigo());
    }

    @Test
    void erro_itemComPrecoZero() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("X", "0.00", 1, "0.10")), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("PEDIDO_INVALIDO", erroDe(req).getCodigo());
    }

    @Test
    void erro_itemComQuantidadeNegativa() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("X", "10.00", -1, "0.10")), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("PEDIDO_INVALIDO", erroDe(req).getCodigo());
    }

    @Test
    void erro_nivelClubeInvalido() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaMaisTenis(), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE");
        assertEquals("NIVEL_CLUBE_INVALIDO", erroDe(req).getCodigo());
    }

    @Test
    void erro_regiaoInvalida() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaMaisTenis(), "EXPRESSA", null, "PIX", 1, "BRONZE", "LESTE");
        assertEquals("REGIAO_INVALIDA", erroDe(req).getCodigo());
    }

    @Test
    void erro_modalidadeInvalida() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaMaisTenis(), "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("MODALIDADE_INVALIDA", erroDe(req).getCodigo());
    }

    @Test
    void erro_motoboyAcimaDe5kg() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Halter", "100.00", 1, "6.00")), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("MODALIDADE_INDISPONIVEL", erroDe(req).getCodigo());
    }

    @Test
    void erro_cupomInexistente() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaMaisTenis(), "EXPRESSA", "NAOEXISTE", "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("CUPOM_INVALIDO", erroDe(req).getCodigo());
    }

    @Test
    void erro_menos50AbaixoDoMinimo() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Meia", "19.90", 2, "0.10")), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("CUPOM_NAO_APLICAVEL", erroDe(req).getCodigo());
    }

    @Test
    void erro_formaPagamentoInvalida() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaMaisTenis(), "EXPRESSA", null, "CRIPTO", 1, "BRONZE", "SUDESTE");
        assertEquals("FORMA_PAGAMENTO_INVALIDA", erroDe(req).getCodigo());
    }

    @Test
    void erro_pixParceladoNaoPermitido() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaMaisTenis(), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE");
        assertEquals("PARCELAMENTO_INVALIDO", erroDe(req).getCodigo());
    }

    @Test
    void erro_cartaoAcimaDe12x() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaMaisTenis(), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE");
        assertEquals("PARCELAMENTO_INVALIDO", erroDe(req).getCodigo());
    }

    @Test
    void erro_boletoAcimaDe1000() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Tenis", "249.90", 5, "1.20")), "ECONOMICA", null, "BOLETO", 1, "BRONZE", "SUDESTE");
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", erroDe(req).getCodigo());
    }

    @Test
    void ordemDosErros_pedidoInvalidoAntesDeNivel() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "LESTE");
        assertEquals("PEDIDO_INVALIDO", erroDe(req).getCodigo());
    }
}
