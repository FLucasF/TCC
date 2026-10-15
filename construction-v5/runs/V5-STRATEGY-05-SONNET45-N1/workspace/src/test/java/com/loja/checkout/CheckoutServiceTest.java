package com.loja.checkout;

import com.loja.checkout.model.*;
import com.loja.checkout.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {
    private CheckoutService service;

    @BeforeEach
    void setUp() {
        service = new CheckoutService(
            new ModalidadeEntregaFactory(),
            new CupomFactory(),
            new NivelClubeFactory(),
            new FormaPagamentoFactory()
        );
    }

    @Test
    void exemplo1() {
        PedidoRequest request = new PedidoRequest(
            List.of(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
            ),
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            null,
            "BRONZE",
            "NORTE"
        );

        Object resultado = service.calcularResumo(request);
        assertTrue(resultado instanceof ResumoResponse);

        ResumoResponse resumo = (ResumoResponse) resultado;
        assertEquals(new BigDecimal("409.70"), resumo.subtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resumo.descontoCupom());
        assertEquals(new BigDecimal("33.10"), resumo.frete());
        assertEquals(2, resumo.prazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), resumo.seguro());
        assertEquals(new BigDecimal("-20.60"), resumo.ajustePagamento());
        assertEquals(new BigDecimal("391.47"), resumo.totalFinal());
        assertEquals(1, resumo.parcelas());
        assertEquals(new BigDecimal("391.47"), resumo.valorParcela());
        assertEquals(new BigDecimal("0.00"), resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }

    @Test
    void exemplo2() {
        PedidoRequest request = new PedidoRequest(
            List.of(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
            ),
            "ECONOMICA",
            null,
            "CARTAO",
            6,
            "PRATA",
            "CENTRO_OESTE"
        );

        Object resultado = service.calcularResumo(request);
        assertTrue(resultado instanceof ResumoResponse);

        ResumoResponse resumo = (ResumoResponse) resultado;
        assertEquals(new BigDecimal("409.70"), resumo.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resumo.descontoCupom());
        assertEquals(new BigDecimal("15.60"), resumo.frete());
        assertEquals(7, resumo.prazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), resumo.seguro());
        assertEquals(new BigDecimal("30.55"), resumo.ajustePagamento());
        assertEquals(new BigDecimal("462.00"), resumo.totalFinal());
        assertEquals(6, resumo.parcelas());
        assertEquals(new BigDecimal("77.00"), resumo.valorParcela());
        assertEquals(new BigDecimal("8.19"), resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }

    @Test
    void exemplo3() {
        PedidoRequest request = new PedidoRequest(
            List.of(
                new ItemCarrinho("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
            ),
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            null,
            "BRONZE",
            "NORDESTE"
        );

        Object resultado = service.calcularResumo(request);
        assertTrue(resultado instanceof ResumoResponse);

        ResumoResponse resumo = (ResumoResponse) resultado;
        assertEquals(new BigDecimal("399.80"), resumo.subtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resumo.descontoCupom());
        assertEquals(new BigDecimal("18.00"), resumo.frete());
        assertEquals(0, resumo.prazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), resumo.seguro());
        assertEquals(new BigDecimal("3.49"), resumo.ajustePagamento());
        assertEquals(new BigDecimal("379.29"), resumo.totalFinal());
        assertEquals(1, resumo.parcelas());
        assertEquals(new BigDecimal("379.29"), resumo.valorParcela());
        assertEquals(new BigDecimal("0.00"), resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }

    @Test
    void exemplo4() {
        PedidoRequest request = new PedidoRequest(
            List.of(
                new ItemCarrinho("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
            ),
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3,
            "PRATA",
            "SUL"
        );

        Object resultado = service.calcularResumo(request);
        assertTrue(resultado instanceof ResumoResponse);

        ResumoResponse resumo = (ResumoResponse) resultado;
        assertEquals(new BigDecimal("299.10"), resumo.subtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resumo.descontoCupom());
        assertEquals(new BigDecimal("0.00"), resumo.frete());
        assertEquals(1, resumo.prazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), resumo.seguro());
        assertEquals(new BigDecimal("0.00"), resumo.ajustePagamento());
        assertEquals(new BigDecimal("262.29"), resumo.totalFinal());
        assertEquals(3, resumo.parcelas());
        assertEquals(new BigDecimal("87.43"), resumo.valorParcela());
        assertEquals(new BigDecimal("5.98"), resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }

    @Test
    void exemplo5() {
        PedidoRequest request = new PedidoRequest(
            List.of(
                new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
            ),
            "EXPRESSA",
            null,
            "PIX",
            null,
            "OURO",
            "SUDESTE"
        );

        Object resultado = service.calcularResumo(request);
        assertTrue(resultado instanceof ResumoResponse);

        ResumoResponse resumo = (ResumoResponse) resultado;
        assertEquals(new BigDecimal("409.70"), resumo.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resumo.descontoCupom());
        assertEquals(new BigDecimal("0.00"), resumo.frete());
        assertEquals(2, resumo.prazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), resumo.seguro());
        assertEquals(new BigDecimal("-20.69"), resumo.ajustePagamento());
        assertEquals(new BigDecimal("393.11"), resumo.totalFinal());
        assertEquals(1, resumo.parcelas());
        assertEquals(new BigDecimal("393.11"), resumo.valorParcela());
        assertEquals(new BigDecimal("20.48"), resumo.creditoProximaCompra());
        assertFalse(resumo.brinde());
    }

    @Test
    void carrinhoVazio() {
        PedidoRequest request = new PedidoRequest(
            List.of(),
            "EXPRESSA",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        Object resultado = service.calcularResumo(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("PEDIDO_INVALIDO", ((ErroResponse) resultado).erro());
    }

    @Test
    void nivelClubeInvalido() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))),
            "EXPRESSA",
            null,
            "PIX",
            null,
            "PLATINA",
            "SUDESTE"
        );

        Object resultado = service.calcularResumo(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("NIVEL_CLUBE_INVALIDO", ((ErroResponse) resultado).erro());
    }

    @Test
    void regiaoInvalida() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))),
            "EXPRESSA",
            null,
            "PIX",
            null,
            "BRONZE",
            "EXTERIOR"
        );

        Object resultado = service.calcularResumo(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("REGIAO_INVALIDA", ((ErroResponse) resultado).erro());
    }

    @Test
    void modalidadeInvalida() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))),
            "DRONE",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        Object resultado = service.calcularResumo(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("MODALIDADE_INVALIDA", ((ErroResponse) resultado).erro());
    }

    @Test
    void modalidadeIndisponivel() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemCarrinho("Produto Pesado", new BigDecimal("100.00"), 10, new BigDecimal("1.0"))),
            "MOTOBOY",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        Object resultado = service.calcularResumo(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("MODALIDADE_INDISPONIVEL", ((ErroResponse) resultado).erro());
    }

    @Test
    void cupomInvalido() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))),
            "EXPRESSA",
            "DESCONTO50",
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        Object resultado = service.calcularResumo(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("CUPOM_INVALIDO", ((ErroResponse) resultado).erro());
    }

    @Test
    void cupomNaoAplicavel() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))),
            "EXPRESSA",
            "MENOS50",
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        Object resultado = service.calcularResumo(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("CUPOM_NAO_APLICAVEL", ((ErroResponse) resultado).erro());
    }

    @Test
    void formaPagamentoInvalida() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))),
            "EXPRESSA",
            null,
            "CHEQUE",
            null,
            "BRONZE",
            "SUDESTE"
        );

        Object resultado = service.calcularResumo(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("FORMA_PAGAMENTO_INVALIDA", ((ErroResponse) resultado).erro());
    }

    @Test
    void parcelamentoInvalido() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))),
            "EXPRESSA",
            null,
            "PIX",
            3,
            "BRONZE",
            "SUDESTE"
        );

        Object resultado = service.calcularResumo(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("PARCELAMENTO_INVALIDO", ((ErroResponse) resultado).erro());
    }

    @Test
    void formaPagamentoIndisponivel() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemCarrinho("Produto Caro", new BigDecimal("1500.00"), 1, new BigDecimal("1.0"))),
            "EXPRESSA",
            null,
            "BOLETO",
            null,
            "BRONZE",
            "SUDESTE"
        );

        Object resultado = service.calcularResumo(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ((ErroResponse) resultado).erro());
    }
}
