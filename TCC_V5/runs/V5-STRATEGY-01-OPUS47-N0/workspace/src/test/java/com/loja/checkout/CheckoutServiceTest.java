package com.loja.checkout;

import com.loja.checkout.service.CheckoutService;
import com.loja.checkout.web.CheckoutException;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.CheckoutResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static CheckoutRequest.Item item(String nome, String preco, int qtd, String peso) {
        CheckoutRequest.Item i = new CheckoutRequest.Item();
        i.nome = nome;
        i.precoUnitario = new BigDecimal(preco);
        i.quantidade = qtd;
        i.pesoKg = new BigDecimal(peso);
        return i;
    }

    private static void assertVal(String esperado, BigDecimal real) {
        assertEquals(new BigDecimal(esperado), real);
    }

    @Test
    void exemplo1() {
        CheckoutRequest r = new CheckoutRequest();
        r.itens = List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20"));
        r.modalidadeEntrega = "EXPRESSA";
        r.cupom = "BEMVINDO10";
        r.formaPagamento = "PIX";
        r.nivelClube = "BRONZE";
        r.regiao = "NORTE";
        CheckoutResponse res = service.calcular(r);
        assertVal("409.70", res.subtotalProdutos);
        assertVal("40.97", res.descontoCupom);
        assertVal("33.10", res.frete);
        assertEquals(2, res.prazoEntregaDias);
        assertVal("10.24", res.seguro);
        assertVal("-20.60", res.ajustePagamento);
        assertVal("391.47", res.totalFinal);
        assertEquals(1, res.parcelas);
        assertVal("391.47", res.valorParcela);
        assertVal("0.00", res.creditoProximaCompra);
        assertFalse(res.brinde);
    }

    @Test
    void exemplo2() {
        CheckoutRequest r = new CheckoutRequest();
        r.itens = List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20"));
        r.modalidadeEntrega = "ECONOMICA";
        r.formaPagamento = "CARTAO";
        r.parcelas = 6;
        r.nivelClube = "PRATA";
        r.regiao = "CENTRO_OESTE";
        CheckoutResponse res = service.calcular(r);
        assertVal("409.70", res.subtotalProdutos);
        assertVal("0.00", res.descontoCupom);
        assertVal("15.60", res.frete);
        assertEquals(7, res.prazoEntregaDias);
        assertVal("6.15", res.seguro);
        assertVal("30.55", res.ajustePagamento);
        assertVal("462.00", res.totalFinal);
        assertEquals(6, res.parcelas);
        assertVal("77.00", res.valorParcela);
        assertVal("8.19", res.creditoProximaCompra);
        assertFalse(res.brinde);
    }

    @Test
    void exemplo3() {
        CheckoutRequest r = new CheckoutRequest();
        r.itens = List.of(item("Fone", "199.90", 2, "0.25"));
        r.modalidadeEntrega = "MOTOBOY";
        r.cupom = "MENOS50";
        r.formaPagamento = "BOLETO";
        r.nivelClube = "BRONZE";
        r.regiao = "NORDESTE";
        CheckoutResponse res = service.calcular(r);
        assertVal("399.80", res.subtotalProdutos);
        assertVal("50.00", res.descontoCupom);
        assertVal("18.00", res.frete);
        assertEquals(0, res.prazoEntregaDias);
        assertVal("8.00", res.seguro);
        assertVal("3.49", res.ajustePagamento);
        assertVal("379.29", res.totalFinal);
        assertEquals(1, res.parcelas);
        assertVal("379.29", res.valorParcela);
        assertVal("0.00", res.creditoProximaCompra);
        assertFalse(res.brinde);
    }

    @Test
    void exemplo4() {
        CheckoutRequest r = new CheckoutRequest();
        r.itens = List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30"));
        r.modalidadeEntrega = "RETIRADA_LOJA";
        r.cupom = "LEVE3PAGUE2";
        r.formaPagamento = "CARTAO";
        r.parcelas = 3;
        r.nivelClube = "PRATA";
        r.regiao = "SUL";
        CheckoutResponse res = service.calcular(r);
        assertVal("299.10", res.subtotalProdutos);
        assertVal("39.80", res.descontoCupom);
        assertVal("0.00", res.frete);
        assertEquals(1, res.prazoEntregaDias);
        assertVal("2.99", res.seguro);
        assertVal("0.00", res.ajustePagamento);
        assertVal("262.29", res.totalFinal);
        assertEquals(3, res.parcelas);
        assertVal("87.43", res.valorParcela);
        assertVal("5.98", res.creditoProximaCompra);
        assertFalse(res.brinde);
    }

    @Test
    void exemplo5() {
        CheckoutRequest r = new CheckoutRequest();
        r.itens = List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20"));
        r.modalidadeEntrega = "EXPRESSA";
        r.formaPagamento = "PIX";
        r.nivelClube = "OURO";
        r.regiao = "SUDESTE";
        CheckoutResponse res = service.calcular(r);
        assertVal("409.70", res.subtotalProdutos);
        assertVal("0.00", res.descontoCupom);
        assertVal("0.00", res.frete);
        assertEquals(2, res.prazoEntregaDias);
        assertVal("4.10", res.seguro);
        assertVal("-20.69", res.ajustePagamento);
        assertVal("393.11", res.totalFinal);
        assertEquals(1, res.parcelas);
        assertVal("393.11", res.valorParcela);
        assertVal("20.48", res.creditoProximaCompra);
        assertFalse(res.brinde);
    }

    private CheckoutRequest base() {
        CheckoutRequest r = new CheckoutRequest();
        r.itens = List.of(item("X", "100.00", 1, "0.50"));
        r.modalidadeEntrega = "ECONOMICA";
        r.formaPagamento = "PIX";
        r.nivelClube = "BRONZE";
        r.regiao = "SUDESTE";
        return r;
    }

    @Test
    void erroCarrinhoVazio() {
        CheckoutRequest r = base();
        r.itens = List.of();
        assertEquals("PEDIDO_INVALIDO", assertThrows(CheckoutException.class, () -> service.calcular(r)).getMessage());
    }

    @Test
    void erroItemInvalido() {
        CheckoutRequest r = base();
        r.itens = List.of(item("X", "0", 1, "1"));
        assertEquals("PEDIDO_INVALIDO", assertThrows(CheckoutException.class, () -> service.calcular(r)).getMessage());
    }

    @Test
    void erroNivel() {
        CheckoutRequest r = base();
        r.nivelClube = "DIAMANTE";
        assertEquals("NIVEL_CLUBE_INVALIDO", assertThrows(CheckoutException.class, () -> service.calcular(r)).getMessage());
    }

    @Test
    void erroRegiao() {
        CheckoutRequest r = base();
        r.regiao = "ANTARTIDA";
        assertEquals("REGIAO_INVALIDA", assertThrows(CheckoutException.class, () -> service.calcular(r)).getMessage());
    }

    @Test
    void erroModalidadeInvalida() {
        CheckoutRequest r = base();
        r.modalidadeEntrega = "DRONE";
        assertEquals("MODALIDADE_INVALIDA", assertThrows(CheckoutException.class, () -> service.calcular(r)).getMessage());
    }

    @Test
    void erroMotoboyPesado() {
        CheckoutRequest r = base();
        r.itens = List.of(item("X", "10.00", 1, "6.0"));
        r.modalidadeEntrega = "MOTOBOY";
        assertEquals("MODALIDADE_INDISPONIVEL", assertThrows(CheckoutException.class, () -> service.calcular(r)).getMessage());
    }

    @Test
    void erroCupomInvalido() {
        CheckoutRequest r = base();
        r.cupom = "NAOEXISTE";
        assertEquals("CUPOM_INVALIDO", assertThrows(CheckoutException.class, () -> service.calcular(r)).getMessage());
    }

    @Test
    void erroCupomNaoAplicavel() {
        CheckoutRequest r = base();
        r.cupom = "MENOS50";
        assertEquals("CUPOM_NAO_APLICAVEL", assertThrows(CheckoutException.class, () -> service.calcular(r)).getMessage());
    }

    @Test
    void erroFormaInvalida() {
        CheckoutRequest r = base();
        r.formaPagamento = "CRIPTO";
        assertEquals("FORMA_PAGAMENTO_INVALIDA", assertThrows(CheckoutException.class, () -> service.calcular(r)).getMessage());
    }

    @Test
    void erroParcelasPixInvalido() {
        CheckoutRequest r = base();
        r.parcelas = 2;
        assertEquals("PARCELAMENTO_INVALIDO", assertThrows(CheckoutException.class, () -> service.calcular(r)).getMessage());
    }

    @Test
    void erroParcelasCartaoInvalido() {
        CheckoutRequest r = base();
        r.formaPagamento = "CARTAO";
        r.parcelas = 13;
        assertEquals("PARCELAMENTO_INVALIDO", assertThrows(CheckoutException.class, () -> service.calcular(r)).getMessage());
    }

    @Test
    void erroBoletoAcimaLimite() {
        CheckoutRequest r = base();
        r.itens = List.of(item("Y", "1500.00", 1, "0.1"));
        r.formaPagamento = "BOLETO";
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", assertThrows(CheckoutException.class, () -> service.calcular(r)).getMessage());
    }
}
