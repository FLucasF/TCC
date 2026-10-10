package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {
    private CheckoutService service;

    @BeforeEach
    void setUp() {
        service = new CheckoutService();
    }

    @Test
    void testExemplo1() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemPedido("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), response.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), response.getSeguro());
        assertEquals(new BigDecimal("-20.60"), response.getAjustePagamento());
        assertEquals(new BigDecimal("391.47"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("391.47"), response.getValorParcela());
        assertEquals(new BigDecimal("0.00"), response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    void testExemplo2() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemPedido("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom(null);
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);
        request.setNivelClube("PRATA");
        request.setRegiao("CENTRO_OESTE");

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_EVEN), response.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), response.getSeguro());
        assertEquals(new BigDecimal("30.55"), response.getAjustePagamento());
        assertEquals(new BigDecimal("462.00"), response.getTotalFinal());
        assertEquals(6, response.getParcelas());
        assertEquals(new BigDecimal("77.00"), response.getValorParcela());
        assertEquals(new BigDecimal("8.19"), response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    void testExemplo3() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORDESTE");

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("399.80"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), response.getFrete());
        assertEquals(0, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), response.getSeguro());
        assertEquals(new BigDecimal("3.49"), response.getAjustePagamento());
        assertEquals(new BigDecimal("379.29"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("379.29"), response.getValorParcela());
        assertEquals(new BigDecimal("0.00"), response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    void testExemplo4() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
            new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);
        request.setNivelClube("PRATA");
        request.setRegiao("SUL");

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("299.10"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.getDescontoCupom());
        assertEquals(BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_EVEN), response.getFrete());
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), response.getSeguro());
        assertEquals(BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_EVEN), response.getAjustePagamento());
        assertEquals(new BigDecimal("262.29"), response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(new BigDecimal("87.43"), response.getValorParcela());
        assertEquals(new BigDecimal("5.98"), response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    void testExemplo5() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemPedido("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom(null);
        request.setFormaPagamento("PIX");
        request.setNivelClube("OURO");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_EVEN), response.getDescontoCupom());
        assertEquals(BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_EVEN), response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), response.getSeguro());
        assertEquals(new BigDecimal("-20.69"), response.getAjustePagamento());
        assertEquals(new BigDecimal("393.11"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("393.11"), response.getValorParcela());
        assertEquals(new BigDecimal("20.48"), response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    void testPedidoInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        Exception exception = assertThrows(Exception.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("PEDIDO_INVALIDO", exception.getMessage());
    }

    @Test
    void testNivelClubeInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("INVALIDO");
        request.setRegiao("NORTE");

        Exception exception = assertThrows(Exception.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("NIVEL_CLUBE_INVALIDO", exception.getMessage());
    }

    @Test
    void testRegiaoInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("INVALIDA");

        Exception exception = assertThrows(Exception.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("REGIAO_INVALIDA", exception.getMessage());
    }

    @Test
    void testModalidadeInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("INVALIDA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        Exception exception = assertThrows(Exception.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("MODALIDADE_INVALIDA", exception.getMessage());
    }

    @Test
    void testModalidadeIndisponivel() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Produto pesado", new BigDecimal("100"), 6, new BigDecimal("1.00"))
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        Exception exception = assertThrows(Exception.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("MODALIDADE_INDISPONIVEL", exception.getMessage());
    }

    @Test
    void testCupomInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("CUPOMINVALIDO");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        Exception exception = assertThrows(Exception.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("CUPOM_INVALIDO", exception.getMessage());
    }

    @Test
    void testCupomNaoAplicavel() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        Exception exception = assertThrows(Exception.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("CUPOM_NAO_APLICAVEL", exception.getMessage());
    }

    @Test
    void testFormaPagamentoInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("INVALIDA");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        Exception exception = assertThrows(Exception.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getMessage());
    }

    @Test
    void testParcelamentoInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(2);
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        Exception exception = assertThrows(Exception.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("PARCELAMENTO_INVALIDO", exception.getMessage());
    }

    @Test
    void testFormaPagamentoIndisponivel() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Produto caro", new BigDecimal("1001"), 1, new BigDecimal("1.00"))
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setFormaPagamento("BOLETO");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        Exception exception = assertThrows(Exception.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getMessage());
    }

    @Test
    void testFreteGratis() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("FRETEGRATIS");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        CheckoutResponse response = service.calcularResumo(request);

        // Frete aparece normalmente, desconto do cupom é igual ao frete
        assertEquals(new BigDecimal("27.70"), response.getFrete());
        assertEquals(new BigDecimal("27.70"), response.getDescontoCupom());
    }
}
