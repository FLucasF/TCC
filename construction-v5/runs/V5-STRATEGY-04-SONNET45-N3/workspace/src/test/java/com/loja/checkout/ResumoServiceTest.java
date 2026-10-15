package com.loja.checkout;

import com.loja.checkout.model.*;
import com.loja.checkout.service.ResumoService;
import com.loja.checkout.domain.cupom.RegistroCupons;
import com.loja.checkout.domain.entrega.RegistroModalidades;
import com.loja.checkout.domain.clube.RegistroNiveis;
import com.loja.checkout.domain.pagamento.RegistroFormasPagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ResumoServiceTest {
    private ResumoService service;

    @BeforeEach
    void setUp() {
        service = new ResumoService(
            new RegistroCupons(),
            new RegistroModalidades(),
            new RegistroNiveis(),
            new RegistroFormasPagamento()
        );
    }

    @Test
    void exemplo1() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            1,
            "BRONZE",
            "NORTE"
        );

        Object resultado = service.calcular(request);
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
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "ECONOMICA",
            null,
            "CARTAO",
            6,
            "PRATA",
            "CENTRO_OESTE"
        );

        Object resultado = service.calcular(request);
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
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            1,
            "BRONZE",
            "NORDESTE"
        );

        Object resultado = service.calcular(request);
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
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3,
            "PRATA",
            "SUL"
        );

        Object resultado = service.calcular(request);
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
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemCarrinho("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            1,
            "OURO",
            "SUDESTE"
        );

        Object resultado = service.calcular(request);
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
    void erroCarrinhoVazio() {
        PedidoRequest request = new PedidoRequest(
            List.of(),
            "EXPRESSA",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        Object resultado = service.calcular(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("PEDIDO_INVALIDO", ((ErroResponse) resultado).erro());
    }

    @Test
    void erroNivelClubeInvalido() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            1,
            "PLATINA",
            "SUDESTE"
        );

        Object resultado = service.calcular(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("NIVEL_CLUBE_INVALIDO", ((ErroResponse) resultado).erro());
    }

    @Test
    void erroRegiaoInvalida() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            1,
            "BRONZE",
            "EUROPA"
        );

        Object resultado = service.calcular(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("REGIAO_INVALIDA", ((ErroResponse) resultado).erro());
    }

    @Test
    void erroModalidadeInvalida() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "FOGUETE",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        Object resultado = service.calcular(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("MODALIDADE_INVALIDA", ((ErroResponse) resultado).erro());
    }

    @Test
    void erroModalidadeIndisponivel() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Geladeira", new BigDecimal("2000.00"), 1, new BigDecimal("60.00"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "MOTOBOY",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        Object resultado = service.calcular(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("MODALIDADE_INDISPONIVEL", ((ErroResponse) resultado).erro());
    }

    @Test
    void erroCupomInvalido() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "EXPRESSA",
            "CUPOMINEXISTENTE",
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        Object resultado = service.calcular(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("CUPOM_INVALIDO", ((ErroResponse) resultado).erro());
    }

    @Test
    void erroCupomNaoAplicavel() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "EXPRESSA",
            "MENOS50",
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        Object resultado = service.calcular(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("CUPOM_NAO_APLICAVEL", ((ErroResponse) resultado).erro());
    }

    @Test
    void erroFormaPagamentoInvalida() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "CRIPTO",
            1,
            "BRONZE",
            "SUDESTE"
        );

        Object resultado = service.calcular(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("FORMA_PAGAMENTO_INVALIDA", ((ErroResponse) resultado).erro());
    }

    @Test
    void erroParcelamentoInvalido() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            3,
            "BRONZE",
            "SUDESTE"
        );

        Object resultado = service.calcular(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("PARCELAMENTO_INVALIDO", ((ErroResponse) resultado).erro());
    }

    @Test
    void erroFormaPagamentoIndisponivel() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Notebook", new BigDecimal("5000.00"), 1, new BigDecimal("2.50"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "BOLETO",
            1,
            "BRONZE",
            "SUDESTE"
        );

        Object resultado = service.calcular(request);
        assertTrue(resultado instanceof ErroResponse);
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ((ErroResponse) resultado).erro());
    }

    @Test
    void cupomFreteGratis() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "EXPRESSA",
            "FRETEGRATIS",
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        Object resultado = service.calcular(request);
        assertTrue(resultado instanceof ResumoResponse);
        ResumoResponse resumo = (ResumoResponse) resultado;

        assertEquals(new BigDecimal("159.80"), resumo.subtotalProdutos());
        assertEquals(new BigDecimal("27.70"), resumo.descontoCupom());
        assertEquals(new BigDecimal("27.70"), resumo.frete());
    }

    @Test
    void clubeOuroComBrinde() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Notebook", new BigDecimal("2500.00"), 1, new BigDecimal("2.50"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            1,
            "OURO",
            "SUDESTE"
        );

        Object resultado = service.calcular(request);
        assertTrue(resultado instanceof ResumoResponse);
        ResumoResponse resumo = (ResumoResponse) resultado;

        assertEquals(new BigDecimal("2500.00"), resumo.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resumo.frete());
        assertEquals(new BigDecimal("125.00"), resumo.creditoProximaCompra());
        assertTrue(resumo.brinde());
    }

    @Test
    void clubeOuroComCupomFreteGratis() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "EXPRESSA",
            "FRETEGRATIS",
            "PIX",
            1,
            "OURO",
            "SUDESTE"
        );

        Object resultado = service.calcular(request);
        assertTrue(resultado instanceof ResumoResponse);
        ResumoResponse resumo = (ResumoResponse) resultado;

        assertEquals(new BigDecimal("0.00"), resumo.descontoCupom());
        assertEquals(new BigDecimal("0.00"), resumo.frete());
    }

    @Test
    void cupomFreteGratisComRetiradaLoja() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "RETIRADA_LOJA",
            "FRETEGRATIS",
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        Object resultado = service.calcular(request);
        assertTrue(resultado instanceof ResumoResponse);
        ResumoResponse resumo = (ResumoResponse) resultado;

        assertEquals(new BigDecimal("0.00"), resumo.frete());
        assertEquals(new BigDecimal("0.00"), resumo.descontoCupom());
    }

    @Test
    void clubeOuroSemBrindeNoLimite() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto", new BigDecimal("500.00"), 1, new BigDecimal("1.00"))
        );

        PedidoRequest request = new PedidoRequest(
            itens,
            "EXPRESSA",
            null,
            "PIX",
            1,
            "OURO",
            "SUDESTE"
        );

        Object resultado = service.calcular(request);
        assertTrue(resultado instanceof ResumoResponse);
        ResumoResponse resumo = (ResumoResponse) resultado;

        assertEquals(new BigDecimal("500.00"), resumo.subtotalProdutos());
        assertFalse(resumo.brinde());
    }
}
