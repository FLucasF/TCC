package com.loja.checkout.service;

import com.loja.checkout.dto.ItemDTO;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResumoServiceTest {

    private final ResumoService service = new ResumoService();

    private ItemDTO item(String nome, String preco, int quantidade, String peso) {
        return new ItemDTO(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    @Test
    void exemplo1() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"
        );
        ResumoResponse resp = service.calcular(request);

        assertEquals(new BigDecimal("409.70"), resp.subtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resp.descontoCupom());
        assertEquals(new BigDecimal("33.10"), resp.frete());
        assertEquals(2, resp.prazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), resp.seguro());
        assertEquals(new BigDecimal("-20.60"), resp.ajustePagamento());
        assertEquals(new BigDecimal("391.47"), resp.totalFinal());
        assertEquals(1, resp.parcelas());
        assertEquals(new BigDecimal("391.47"), resp.valorParcela());
        assertEquals(new BigDecimal("0.00"), resp.creditoProximaCompra());
        assertFalse(resp.brinde());
    }

    @Test
    void exemplo2() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"
        );
        ResumoResponse resp = service.calcular(request);

        assertEquals(new BigDecimal("409.70"), resp.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resp.descontoCupom());
        assertEquals(new BigDecimal("15.60"), resp.frete());
        assertEquals(7, resp.prazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), resp.seguro());
        assertEquals(new BigDecimal("30.55"), resp.ajustePagamento());
        assertEquals(new BigDecimal("462.00"), resp.totalFinal());
        assertEquals(6, resp.parcelas());
        assertEquals(new BigDecimal("77.00"), resp.valorParcela());
        assertEquals(new BigDecimal("8.19"), resp.creditoProximaCompra());
        assertFalse(resp.brinde());
    }

    @Test
    void exemplo3() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"
        );
        ResumoResponse resp = service.calcular(request);

        assertEquals(new BigDecimal("399.80"), resp.subtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resp.descontoCupom());
        assertEquals(new BigDecimal("18.00"), resp.frete());
        assertEquals(0, resp.prazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), resp.seguro());
        assertEquals(new BigDecimal("3.49"), resp.ajustePagamento());
        assertEquals(new BigDecimal("379.29"), resp.totalFinal());
        assertEquals(1, resp.parcelas());
        assertEquals(new BigDecimal("379.29"), resp.valorParcela());
        assertEquals(new BigDecimal("0.00"), resp.creditoProximaCompra());
        assertFalse(resp.brinde());
    }

    @Test
    void exemplo4() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"
        );
        ResumoResponse resp = service.calcular(request);

        assertEquals(new BigDecimal("299.10"), resp.subtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resp.descontoCupom());
        assertEquals(new BigDecimal("0.00"), resp.frete());
        assertEquals(1, resp.prazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), resp.seguro());
        assertEquals(new BigDecimal("0.00"), resp.ajustePagamento());
        assertEquals(new BigDecimal("262.29"), resp.totalFinal());
        assertEquals(3, resp.parcelas());
        assertEquals(new BigDecimal("87.43"), resp.valorParcela());
        assertEquals(new BigDecimal("5.98"), resp.creditoProximaCompra());
        assertFalse(resp.brinde());
    }

    @Test
    void exemplo5() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"
        );
        ResumoResponse resp = service.calcular(request);

        assertEquals(new BigDecimal("409.70"), resp.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resp.descontoCupom());
        assertEquals(new BigDecimal("0.00"), resp.frete());
        assertEquals(2, resp.prazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), resp.seguro());
        assertEquals(new BigDecimal("-20.69"), resp.ajustePagamento());
        assertEquals(new BigDecimal("393.11"), resp.totalFinal());
        assertEquals(1, resp.parcelas());
        assertEquals(new BigDecimal("393.11"), resp.valorParcela());
        assertEquals(new BigDecimal("20.48"), resp.creditoProximaCompra());
        assertFalse(resp.brinde());
    }

    @Test
    void carrinhoVazioRetornaPedidoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE"
        );
        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("PEDIDO_INVALIDO", ex.getCodigo());
    }

    @Test
    void motoboyAcimaDoLimiteDePesoRetornaModalidadeIndisponivel() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Caixa", "10.00", 1, "6.00")),
                "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE"
        );
        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("MODALIDADE_INDISPONIVEL", ex.getCodigo());
    }

    @Test
    void boletoAcimaDoLimiteRetornaFormaPagamentoIndisponivel() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Jaqueta", "1200.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE"
        );
        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ex.getCodigo());
    }

    @Test
    void cupomDesconhecidoRetornaCupomInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Caixa", "10.00", 1, "1.00")),
                "RETIRADA_LOJA", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE"
        );
        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("CUPOM_INVALIDO", ex.getCodigo());
    }

    @Test
    void pixComParcelamentoRetornaParcelamentoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Caixa", "10.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "PIX", 2, "BRONZE", "SUDESTE"
        );
        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("PARCELAMENTO_INVALIDO", ex.getCodigo());
    }
}
