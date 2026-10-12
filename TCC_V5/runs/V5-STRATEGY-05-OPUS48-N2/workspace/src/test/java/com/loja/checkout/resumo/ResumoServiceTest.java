package com.loja.checkout.resumo;

import com.loja.checkout.comum.CheckoutException;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.PedidoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ResumoServiceTest {

    @Autowired
    private ResumoService service;

    private static ItemRequest item(String nome, String preco, int qtd, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static void dinheiro(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real),
                "esperado " + esperado + " mas veio " + real);
    }

    @Test
    void exemplo1() {
        var req = new PedidoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        ResumoResponse r = service.calcular(req);

        dinheiro("409.70", r.subtotalProdutos());
        dinheiro("40.97", r.descontoCupom());
        dinheiro("33.10", r.frete());
        assertEquals(2, r.prazoEntregaDias());
        dinheiro("10.24", r.seguro());
        dinheiro("-20.60", r.ajustePagamento());
        dinheiro("391.47", r.totalFinal());
        assertEquals(1, r.parcelas());
        dinheiro("391.47", r.valorParcela());
        dinheiro("0.00", r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo2() {
        var req = new PedidoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse r = service.calcular(req);

        dinheiro("409.70", r.subtotalProdutos());
        dinheiro("0.00", r.descontoCupom());
        dinheiro("15.60", r.frete());
        assertEquals(7, r.prazoEntregaDias());
        dinheiro("6.15", r.seguro());
        dinheiro("30.55", r.ajustePagamento());
        dinheiro("462.00", r.totalFinal());
        assertEquals(6, r.parcelas());
        dinheiro("77.00", r.valorParcela());
        dinheiro("8.19", r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo3() {
        var req = new PedidoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");

        ResumoResponse r = service.calcular(req);

        dinheiro("399.80", r.subtotalProdutos());
        dinheiro("50.00", r.descontoCupom());
        dinheiro("18.00", r.frete());
        assertEquals(0, r.prazoEntregaDias());
        dinheiro("8.00", r.seguro());
        dinheiro("3.49", r.ajustePagamento());
        dinheiro("379.29", r.totalFinal());
        assertEquals(1, r.parcelas());
        dinheiro("379.29", r.valorParcela());
        dinheiro("0.00", r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo4() {
        var req = new PedidoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse r = service.calcular(req);

        dinheiro("299.10", r.subtotalProdutos());
        dinheiro("39.80", r.descontoCupom());
        dinheiro("0.00", r.frete());
        assertEquals(1, r.prazoEntregaDias());
        dinheiro("2.99", r.seguro());
        dinheiro("0.00", r.ajustePagamento());
        dinheiro("262.29", r.totalFinal());
        assertEquals(3, r.parcelas());
        dinheiro("87.43", r.valorParcela());
        dinheiro("5.98", r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo5() {
        var req = new PedidoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse r = service.calcular(req);

        dinheiro("409.70", r.subtotalProdutos());
        dinheiro("0.00", r.descontoCupom());
        dinheiro("0.00", r.frete());
        assertEquals(2, r.prazoEntregaDias());
        dinheiro("4.10", r.seguro());
        dinheiro("-20.69", r.ajustePagamento());
        dinheiro("393.11", r.totalFinal());
        assertEquals(1, r.parcelas());
        dinheiro("393.11", r.valorParcela());
        dinheiro("20.48", r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void brindeOuroAcimaDe500() {
        var req = new PedidoRequest(
                List.of(item("Jaqueta", "600.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE");

        assertTrue(service.calcular(req).brinde());
    }

    private PedidoRequest base(String modalidade, String cupom, String pagamento, Integer parcelas,
                               String nivel, String regiao) {
        return new PedidoRequest(List.of(item("X", "100.00", 1, "0.50")),
                modalidade, cupom, pagamento, parcelas, nivel, regiao);
    }

    private String erro(PedidoRequest req) {
        return assertThrows(CheckoutException.class, () -> service.calcular(req)).getCodigo();
    }

    @Test
    void carrinhoVazio() {
        var req = new PedidoRequest(List.of(), "ECONOMICA", null, "PIX", 1, "BRONZE", "SUL");
        assertEquals("PEDIDO_INVALIDO", erro(req));
    }

    @Test
    void itemComPesoZero() {
        var req = new PedidoRequest(List.of(item("X", "10.00", 1, "0")),
                "ECONOMICA", null, "PIX", 1, "BRONZE", "SUL");
        assertEquals("PEDIDO_INVALIDO", erro(req));
    }

    @Test
    void nivelInvalido() {
        assertEquals("NIVEL_CLUBE_INVALIDO", erro(base("ECONOMICA", null, "PIX", 1, "PLATINA", "SUL")));
    }

    @Test
    void regiaoInvalida() {
        assertEquals("REGIAO_INVALIDA", erro(base("ECONOMICA", null, "PIX", 1, "BRONZE", "LESTE")));
    }

    @Test
    void modalidadeInvalida() {
        assertEquals("MODALIDADE_INVALIDA", erro(base("DRONE", null, "PIX", 1, "BRONZE", "SUL")));
    }

    @Test
    void motoboyAcimaDoPeso() {
        var req = new PedidoRequest(List.of(item("Peso", "10.00", 1, "6.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUL");
        assertEquals("MODALIDADE_INDISPONIVEL", erro(req));
    }

    @Test
    void cupomInexistente() {
        assertEquals("CUPOM_INVALIDO", erro(base("ECONOMICA", "NATAL99", "PIX", 1, "BRONZE", "SUL")));
    }

    @Test
    void menos50AbaixoDoMinimo() {
        assertEquals("CUPOM_NAO_APLICAVEL", erro(base("ECONOMICA", "MENOS50", "PIX", 1, "BRONZE", "SUL")));
    }

    @Test
    void pagamentoInvalido() {
        assertEquals("FORMA_PAGAMENTO_INVALIDA", erro(base("ECONOMICA", null, "CHEQUE", 1, "BRONZE", "SUL")));
    }

    @Test
    void pixParcelado() {
        assertEquals("PARCELAMENTO_INVALIDO", erro(base("ECONOMICA", null, "PIX", 2, "BRONZE", "SUL")));
    }

    @Test
    void cartaoAlemDe12() {
        assertEquals("PARCELAMENTO_INVALIDO", erro(base("ECONOMICA", null, "CARTAO", 13, "BRONZE", "SUL")));
    }

    @Test
    void boletoAcimaDeMil() {
        var req = new PedidoRequest(List.of(item("Caro", "2000.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUL");
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", erro(req));
    }

    @Test
    void parcelasAusentesViram1() {
        var req = base("ECONOMICA", null, "CARTAO", null, "BRONZE", "SUL");
        assertEquals(1, service.calcular(req).parcelas());
    }
}
