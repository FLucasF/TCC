package com.loja.checkout;

import com.loja.checkout.model.PedidoRequest;
import com.loja.checkout.model.PedidoRequest.Item;
import com.loja.checkout.model.RespostaResumo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    private static List<Item> itensExemploBase() {
        return List.of(
                new Item("Camiseta", bd("79.90"), 2, bd("0.30")),
                new Item("Tenis", bd("249.90"), 1, bd("1.20"))
        );
    }

    @Test
    void exemplo1_bemvindo10_pix_bronze_norte_expressa() {
        var req = new PedidoRequest(itensExemploBase(), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");
        RespostaResumo r = service.calcular(req);
        assertEquals(bd("409.70"), r.subtotalProdutos());
        assertEquals(bd("40.97"), r.descontoCupom());
        assertEquals(bd("33.10"), r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(bd("10.24"), r.seguro());
        assertEquals(bd("-20.60"), r.ajustePagamento());
        assertEquals(bd("391.47"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(bd("391.47"), r.valorParcela());
        assertEquals(bd("0.00"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo2_economica_cartao6x_prata_centro_oeste() {
        var req = new PedidoRequest(itensExemploBase(), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");
        RespostaResumo r = service.calcular(req);
        assertEquals(bd("409.70"), r.subtotalProdutos());
        assertEquals(bd("0.00"), r.descontoCupom());
        assertEquals(bd("15.60"), r.frete());
        assertEquals(7, r.prazoEntregaDias());
        assertEquals(bd("6.15"), r.seguro());
        assertEquals(bd("30.55"), r.ajustePagamento());
        assertEquals(bd("462.00"), r.totalFinal());
        assertEquals(6, r.parcelas());
        assertEquals(bd("77.00"), r.valorParcela());
        assertEquals(bd("8.19"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        var itens = List.of(new Item("Fone", bd("199.90"), 2, bd("0.25")));
        var req = new PedidoRequest(itens, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");
        RespostaResumo r = service.calcular(req);
        assertEquals(bd("399.80"), r.subtotalProdutos());
        assertEquals(bd("50.00"), r.descontoCupom());
        assertEquals(bd("18.00"), r.frete());
        assertEquals(0, r.prazoEntregaDias());
        assertEquals(bd("8.00"), r.seguro());
        assertEquals(bd("3.49"), r.ajustePagamento());
        assertEquals(bd("379.29"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(bd("379.29"), r.valorParcela());
        assertEquals(bd("0.00"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao3x_prata_sul() {
        var itens = List.of(
                new Item("Meia", bd("19.90"), 7, bd("0.10")),
                new Item("Camiseta", bd("79.90"), 2, bd("0.30"))
        );
        var req = new PedidoRequest(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");
        RespostaResumo r = service.calcular(req);
        assertEquals(bd("299.10"), r.subtotalProdutos());
        assertEquals(bd("39.80"), r.descontoCupom());
        assertEquals(bd("0.00"), r.frete());
        assertEquals(1, r.prazoEntregaDias());
        assertEquals(bd("2.99"), r.seguro());
        assertEquals(bd("0.00"), r.ajustePagamento());
        assertEquals(bd("262.29"), r.totalFinal());
        assertEquals(3, r.parcelas());
        assertEquals(bd("87.43"), r.valorParcela());
        assertEquals(bd("5.98"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo5_expressa_ouro_sudeste_pix() {
        var req = new PedidoRequest(itensExemploBase(), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");
        RespostaResumo r = service.calcular(req);
        assertEquals(bd("409.70"), r.subtotalProdutos());
        assertEquals(bd("0.00"), r.descontoCupom());
        assertEquals(bd("0.00"), r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(bd("4.10"), r.seguro());
        assertEquals(bd("-20.69"), r.ajustePagamento());
        assertEquals(bd("393.11"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(bd("393.11"), r.valorParcela());
        assertEquals(bd("20.48"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void erro_carrinhoVazio_pedidoInvalido() {
        var req = new PedidoRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("PEDIDO_INVALIDO", assertThrows(CheckoutException.class, () -> service.calcular(req)).codigo);
    }

    @Test
    void erro_nivelClubeInvalido() {
        var req = new PedidoRequest(itensExemploBase(), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE");
        assertEquals("NIVEL_CLUBE_INVALIDO", assertThrows(CheckoutException.class, () -> service.calcular(req)).codigo);
    }

    @Test
    void erro_motoboyAcimaDe5kg() {
        var itens = List.of(new Item("Pesado", bd("100"), 1, bd("6")));
        var req = new PedidoRequest(itens, "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("MODALIDADE_INDISPONIVEL", assertThrows(CheckoutException.class, () -> service.calcular(req)).codigo);
    }

    @Test
    void erro_menos50_abaixoDe300() {
        var itens = List.of(new Item("X", bd("100"), 2, bd("0.1")));
        var req = new PedidoRequest(itens, "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("CUPOM_NAO_APLICAVEL", assertThrows(CheckoutException.class, () -> service.calcular(req)).codigo);
    }

    @Test
    void erro_boletoAcimaDe1000() {
        var itens = List.of(new Item("Caro", bd("500"), 3, bd("0.5")));
        var req = new PedidoRequest(itens, "EXPRESSA", null, "BOLETO", 1, "BRONZE", "SUDESTE");
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", assertThrows(CheckoutException.class, () -> service.calcular(req)).codigo);
    }

    @Test
    void erro_cupomInvalido() {
        var req = new PedidoRequest(itensExemploBase(), "EXPRESSA", "NAOEXISTE", "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("CUPOM_INVALIDO", assertThrows(CheckoutException.class, () -> service.calcular(req)).codigo);
    }

    @Test
    void erro_parcelamentoInvalidoPix() {
        var req = new PedidoRequest(itensExemploBase(), "EXPRESSA", null, "PIX", 3, "BRONZE", "SUDESTE");
        assertEquals("PARCELAMENTO_INVALIDO", assertThrows(CheckoutException.class, () -> service.calcular(req)).codigo);
    }
}
