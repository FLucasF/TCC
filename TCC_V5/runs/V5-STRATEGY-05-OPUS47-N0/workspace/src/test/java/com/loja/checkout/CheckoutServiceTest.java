package com.loja.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static PedidoRequest.Item it(String nome, String preco, int qtd, String peso) {
        return new PedidoRequest.Item(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> run(PedidoRequest req) {
        ResponseEntity<Object> r = service.calcular(req);
        return (Map<String, Object>) r.getBody();
    }

    private static void assertMoney(String expected, Object actual) {
        assertEquals(new BigDecimal(expected), ((BigDecimal) actual).setScale(2));
    }

    @Test
    void exemplo1() {
        var req = new PedidoRequest(
                List.of(it("Camiseta", "79.90", 2, "0.30"), it("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");
        var r = run(req);
        assertMoney("409.70", r.get("subtotalProdutos"));
        assertMoney("40.97", r.get("descontoCupom"));
        assertMoney("33.10", r.get("frete"));
        assertEquals(2, r.get("prazoEntregaDias"));
        assertMoney("10.24", r.get("seguro"));
        assertMoney("-20.60", r.get("ajustePagamento"));
        assertMoney("391.47", r.get("totalFinal"));
        assertEquals(1, r.get("parcelas"));
        assertMoney("391.47", r.get("valorParcela"));
        assertMoney("0.00", r.get("creditoProximaCompra"));
        assertEquals(false, r.get("brinde"));
    }

    @Test
    void exemplo2() {
        var req = new PedidoRequest(
                List.of(it("Camiseta", "79.90", 2, "0.30"), it("Tenis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");
        var r = run(req);
        assertMoney("409.70", r.get("subtotalProdutos"));
        assertMoney("0.00", r.get("descontoCupom"));
        assertMoney("15.60", r.get("frete"));
        assertEquals(7, r.get("prazoEntregaDias"));
        assertMoney("6.15", r.get("seguro"));
        assertMoney("30.55", r.get("ajustePagamento"));
        assertMoney("462.00", r.get("totalFinal"));
        assertEquals(6, r.get("parcelas"));
        assertMoney("77.00", r.get("valorParcela"));
        assertMoney("8.19", r.get("creditoProximaCompra"));
        assertEquals(false, r.get("brinde"));
    }

    @Test
    void exemplo3() {
        var req = new PedidoRequest(
                List.of(it("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");
        var r = run(req);
        assertMoney("399.80", r.get("subtotalProdutos"));
        assertMoney("50.00", r.get("descontoCupom"));
        assertMoney("18.00", r.get("frete"));
        assertEquals(0, r.get("prazoEntregaDias"));
        assertMoney("8.00", r.get("seguro"));
        assertMoney("3.49", r.get("ajustePagamento"));
        assertMoney("379.29", r.get("totalFinal"));
        assertMoney("379.29", r.get("valorParcela"));
        assertMoney("0.00", r.get("creditoProximaCompra"));
        assertEquals(false, r.get("brinde"));
    }

    @Test
    void exemplo4() {
        var req = new PedidoRequest(
                List.of(it("Meia", "19.90", 7, "0.10"), it("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");
        var r = run(req);
        assertMoney("299.10", r.get("subtotalProdutos"));
        assertMoney("39.80", r.get("descontoCupom"));
        assertMoney("0.00", r.get("frete"));
        assertEquals(1, r.get("prazoEntregaDias"));
        assertMoney("2.99", r.get("seguro"));
        assertMoney("0.00", r.get("ajustePagamento"));
        assertMoney("262.29", r.get("totalFinal"));
        assertEquals(3, r.get("parcelas"));
        assertMoney("87.43", r.get("valorParcela"));
        assertMoney("5.98", r.get("creditoProximaCompra"));
        assertEquals(false, r.get("brinde"));
    }

    @Test
    void exemplo5() {
        var req = new PedidoRequest(
                List.of(it("Camiseta", "79.90", 2, "0.30"), it("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");
        var r = run(req);
        assertMoney("409.70", r.get("subtotalProdutos"));
        assertMoney("0.00", r.get("descontoCupom"));
        assertMoney("0.00", r.get("frete"));
        assertEquals(2, r.get("prazoEntregaDias"));
        assertMoney("4.10", r.get("seguro"));
        assertMoney("-20.69", r.get("ajustePagamento"));
        assertMoney("393.11", r.get("totalFinal"));
        assertMoney("20.48", r.get("creditoProximaCompra"));
        assertEquals(false, r.get("brinde"));
    }

    @Test
    void pedidoInvalidoCarrinhoVazio() {
        var req = new PedidoRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("PEDIDO_INVALIDO", run(req).get("erro"));
    }

    @Test
    void motoboyAcimaDe5Kg() {
        var req = new PedidoRequest(
                List.of(it("Caixa", "100.00", 1, "6.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("MODALIDADE_INDISPONIVEL", run(req).get("erro"));
    }

    @Test
    void menos50AbaixoDoMinimo() {
        var req = new PedidoRequest(
                List.of(it("Item", "100.00", 1, "0.10")),
                "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");
        assertEquals("CUPOM_NAO_APLICAVEL", run(req).get("erro"));
    }

    @Test
    void boletoAcimaDe1000() {
        var req = new PedidoRequest(
                List.of(it("Item", "2000.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE");
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", run(req).get("erro"));
    }

    @Test
    void parcelamentoInvalidoPix() {
        var req = new PedidoRequest(
                List.of(it("Item", "100.00", 1, "0.10")),
                "RETIRADA_LOJA", null, "PIX", 3, "BRONZE", "SUDESTE");
        assertEquals("PARCELAMENTO_INVALIDO", run(req).get("erro"));
    }
}
