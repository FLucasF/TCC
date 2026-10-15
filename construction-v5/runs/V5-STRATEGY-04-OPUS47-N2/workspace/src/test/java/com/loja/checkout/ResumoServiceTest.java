package com.loja.checkout;

import com.loja.checkout.calculo.ErroPedido;
import com.loja.checkout.calculo.ResumoService;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.CheckoutResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumoServiceTest {

    private final ResumoService service = new ResumoService();

    private static CheckoutRequest.Item item(String nome, String preco, int qtd, String peso) {
        CheckoutRequest.Item it = new CheckoutRequest.Item();
        it.nome = nome;
        it.precoUnitario = new BigDecimal(preco);
        it.quantidade = qtd;
        it.pesoKg = new BigDecimal(peso);
        return it;
    }

    private static CheckoutRequest req(List<CheckoutRequest.Item> itens, String modalidade, String cupom,
                                       String pagamento, Integer parcelas, String nivel, String regiao) {
        CheckoutRequest r = new CheckoutRequest();
        r.itens = itens;
        r.modalidadeEntrega = modalidade;
        r.cupom = cupom;
        r.formaPagamento = pagamento;
        r.parcelas = parcelas;
        r.nivelClube = nivel;
        r.regiao = regiao;
        return r;
    }

    private static void assertMoney(String esperado, BigDecimal real) {
        assertEquals(new BigDecimal(esperado), real);
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        var r = req(List.of(
                item("Camiseta", "79.90", 2, "0.30"),
                item("Tenis", "249.90", 1, "1.20")
        ), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        CheckoutResponse resp = service.calcular(r);
        assertMoney("409.70", resp.subtotalProdutos);
        assertMoney("40.97", resp.descontoCupom);
        assertMoney("33.10", resp.frete);
        assertEquals(2, resp.prazoEntregaDias);
        assertMoney("10.24", resp.seguro);
        assertMoney("-20.60", resp.ajustePagamento);
        assertMoney("391.47", resp.totalFinal);
        assertEquals(1, resp.parcelas);
        assertMoney("391.47", resp.valorParcela);
        assertMoney("0.00", resp.creditoProximaCompra);
        assertFalse(resp.brinde);
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao6x_prata_centrooeste() {
        var r = req(List.of(
                item("Camiseta", "79.90", 2, "0.30"),
                item("Tenis", "249.90", 1, "1.20")
        ), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        CheckoutResponse resp = service.calcular(r);
        assertMoney("409.70", resp.subtotalProdutos);
        assertMoney("0.00", resp.descontoCupom);
        assertMoney("15.60", resp.frete);
        assertEquals(7, resp.prazoEntregaDias);
        assertMoney("6.15", resp.seguro);
        assertMoney("30.55", resp.ajustePagamento);
        assertMoney("462.00", resp.totalFinal);
        assertEquals(6, resp.parcelas);
        assertMoney("77.00", resp.valorParcela);
        assertMoney("8.19", resp.creditoProximaCompra);
        assertFalse(resp.brinde);
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        var r = req(List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");

        CheckoutResponse resp = service.calcular(r);
        assertMoney("399.80", resp.subtotalProdutos);
        assertMoney("50.00", resp.descontoCupom);
        assertMoney("18.00", resp.frete);
        assertEquals(0, resp.prazoEntregaDias);
        assertMoney("8.00", resp.seguro);
        assertMoney("3.49", resp.ajustePagamento);
        assertMoney("379.29", resp.totalFinal);
        assertEquals(1, resp.parcelas);
        assertMoney("379.29", resp.valorParcela);
        assertMoney("0.00", resp.creditoProximaCompra);
        assertFalse(resp.brinde);
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao3x_prata_sul() {
        var r = req(List.of(
                item("Meia", "19.90", 7, "0.10"),
                item("Camiseta", "79.90", 2, "0.30")
        ), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        CheckoutResponse resp = service.calcular(r);
        assertMoney("299.10", resp.subtotalProdutos);
        assertMoney("39.80", resp.descontoCupom);
        assertMoney("0.00", resp.frete);
        assertEquals(1, resp.prazoEntregaDias);
        assertMoney("2.99", resp.seguro);
        assertMoney("0.00", resp.ajustePagamento);
        assertMoney("262.29", resp.totalFinal);
        assertEquals(3, resp.parcelas);
        assertMoney("87.43", resp.valorParcela);
        assertMoney("5.98", resp.creditoProximaCompra);
        assertFalse(resp.brinde);
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        var r = req(List.of(
                item("Camiseta", "79.90", 2, "0.30"),
                item("Tenis", "249.90", 1, "1.20")
        ), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        CheckoutResponse resp = service.calcular(r);
        assertMoney("409.70", resp.subtotalProdutos);
        assertMoney("0.00", resp.descontoCupom);
        assertMoney("0.00", resp.frete);
        assertEquals(2, resp.prazoEntregaDias);
        assertMoney("4.10", resp.seguro);
        assertMoney("-20.69", resp.ajustePagamento);
        assertMoney("393.11", resp.totalFinal);
        assertEquals(1, resp.parcelas);
        assertMoney("393.11", resp.valorParcela);
        assertMoney("20.48", resp.creditoProximaCompra);
        assertFalse(resp.brinde);
    }

    @Test
    void pedido_vazio() {
        var r = req(new ArrayList<>(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("PEDIDO_INVALIDO", assertThrows(ErroPedido.class, () -> service.calcular(r)).codigo());
    }

    @Test
    void nivel_invalido_prevalece_sobre_modalidade() {
        var r = req(List.of(item("x", "10", 1, "0.1")), "XPTO", null, "PIX", 1, "DIAMANTE", "SUDESTE");
        assertEquals("NIVEL_CLUBE_INVALIDO", assertThrows(ErroPedido.class, () -> service.calcular(r)).codigo());
    }

    @Test
    void motoboy_acima_de_5kg() {
        var r = req(List.of(item("x", "10", 1, "6")), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("MODALIDADE_INDISPONIVEL", assertThrows(ErroPedido.class, () -> service.calcular(r)).codigo());
    }

    @Test
    void cupom_inexistente() {
        var r = req(List.of(item("x", "10", 1, "0.1")), "EXPRESSA", "NAOEXISTE", "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("CUPOM_INVALIDO", assertThrows(ErroPedido.class, () -> service.calcular(r)).codigo());
    }

    @Test
    void menos50_abaixo_do_minimo() {
        var r = req(List.of(item("x", "10", 1, "0.1")), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("CUPOM_NAO_APLICAVEL", assertThrows(ErroPedido.class, () -> service.calcular(r)).codigo());
    }

    @Test
    void boleto_acima_de_1000() {
        var r = req(List.of(item("x", "1500", 1, "0.1")), "EXPRESSA", null, "BOLETO", 1, "BRONZE", "SUDESTE");
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", assertThrows(ErroPedido.class, () -> service.calcular(r)).codigo());
    }

    @Test
    void pix_com_mais_de_1_parcela() {
        var r = req(List.of(item("x", "10", 1, "0.1")), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE");
        assertEquals("PARCELAMENTO_INVALIDO", assertThrows(ErroPedido.class, () -> service.calcular(r)).codigo());
    }

    @Test
    void ouro_acima_de_500_ganha_brinde() {
        var r = req(List.of(item("x", "600", 1, "0.1")), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");
        CheckoutResponse resp = service.calcular(r);
        assertTrue(resp.brinde);
    }
}
