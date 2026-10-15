package com.loja.checkout;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static PedidoRequest.Item item(String nome, String preco, int qtd, String peso) {
        return new PedidoRequest.Item(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    // Exemplo 1: EXPRESSA, BEMVINDO10, PIX, BRONZE, NORTE
    @Test
    void exemplo1() {
        var req = new PedidoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"
        );
        ResumoResponse r = service.calcular(req);

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

    // Exemplo 2: ECONOMICA, sem cupom, CARTAO 6x, PRATA, CENTRO_OESTE
    @Test
    void exemplo2() {
        var req = new PedidoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"
        );
        ResumoResponse r = service.calcular(req);

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

    // Exemplo 3: MOTOBOY, MENOS50, BOLETO, BRONZE, NORDESTE
    @Test
    void exemplo3() {
        var req = new PedidoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"
        );
        ResumoResponse r = service.calcular(req);

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

    // Exemplo 4: RETIRADA_LOJA, LEVE3PAGUE2, CARTAO 3x, PRATA, SUL
    @Test
    void exemplo4() {
        var req = new PedidoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"
        );
        ResumoResponse r = service.calcular(req);

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

    // Exemplo 5: EXPRESSA, sem cupom, PIX, OURO, SUDESTE
    @Test
    void exemplo5() {
        var req = new PedidoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"
        );
        ResumoResponse r = service.calcular(req);

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

    // Erros: carrinho vazio
    @Test
    void erroCarrinhoVazio() {
        var req = new PedidoRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("PEDIDO_INVALIDO", ex.getCodigoErro());
    }

    // Erros: item com preço zero
    @Test
    void erroItemPrecoZero() {
        var req = new PedidoRequest(
                List.of(item("X", "0.00", 1, "0.50")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"
        );
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("PEDIDO_INVALIDO", ex.getCodigoErro());
    }

    // Erros: nível de clube inválido
    @Test
    void erroNivelClubeInvalido() {
        var req = new PedidoRequest(
                List.of(item("X", "10.00", 1, "0.50")),
                "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE"
        );
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("NIVEL_CLUBE_INVALIDO", ex.getCodigoErro());
    }

    // Erros: região inválida
    @Test
    void erroRegiaoInvalida() {
        var req = new PedidoRequest(
                List.of(item("X", "10.00", 1, "0.50")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "LESTE"
        );
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("REGIAO_INVALIDA", ex.getCodigoErro());
    }

    // Erros: modalidade inválida
    @Test
    void erroModalidadeInvalida() {
        var req = new PedidoRequest(
                List.of(item("X", "10.00", 1, "0.50")),
                "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE"
        );
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("MODALIDADE_INVALIDA", ex.getCodigoErro());
    }

    // Erros: motoboy acima de 5 kg
    @Test
    void erroModalidadeIndisponivel() {
        var req = new PedidoRequest(
                List.of(item("X", "10.00", 1, "6.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"
        );
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("MODALIDADE_INDISPONIVEL", ex.getCodigoErro());
    }

    // Erros: cupom inexistente
    @Test
    void erroCupomInvalido() {
        var req = new PedidoRequest(
                List.of(item("X", "10.00", 1, "0.50")),
                "EXPRESSA", "SUPEROFERTA", "PIX", 1, "BRONZE", "SUDESTE"
        );
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("CUPOM_INVALIDO", ex.getCodigoErro());
    }

    // Erros: MENOS50 abaixo do mínimo
    @Test
    void erroCupomNaoAplicavel() {
        var req = new PedidoRequest(
                List.of(item("X", "100.00", 1, "0.50")),
                "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"
        );
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("CUPOM_NAO_APLICAVEL", ex.getCodigoErro());
    }

    // Erros: forma de pagamento inválida
    @Test
    void erroFormaPagamentoInvalida() {
        var req = new PedidoRequest(
                List.of(item("X", "10.00", 1, "0.50")),
                "EXPRESSA", null, "CHEQUE", 1, "BRONZE", "SUDESTE"
        );
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", ex.getCodigoErro());
    }

    // Erros: PIX parcelado
    @Test
    void erroParcelamentoInvalido() {
        var req = new PedidoRequest(
                List.of(item("X", "10.00", 1, "0.50")),
                "EXPRESSA", null, "PIX", 3, "BRONZE", "SUDESTE"
        );
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("PARCELAMENTO_INVALIDO", ex.getCodigoErro());
    }

    // Erros: boleto acima de R$ 1.000,00
    @Test
    void erroFormaPagamentoIndisponivel() {
        var req = new PedidoRequest(
                List.of(item("X", "600.00", 2, "0.50")),
                "EXPRESSA", null, "BOLETO", 1, "BRONZE", "SUDESTE"
        );
        var ex = assertThrows(CheckoutException.class, () -> service.calcular(req));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ex.getCodigoErro());
    }
}
