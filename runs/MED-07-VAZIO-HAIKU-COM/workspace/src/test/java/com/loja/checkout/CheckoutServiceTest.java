package com.loja.checkout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {
    private CheckoutService service;

    @BeforeEach
    void setup() {
        service = new CheckoutService();
    }

    @Test
    void testExemplo1() throws ErroCheckout {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Camiseta", 79.90, 2, 0.30),
            new ItemPedido("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);

        ResumoResponse response = service.calcularResumo(request);

        assertEquals(409.70, response.getSubtotalProdutos());
        assertEquals(40.97, response.getDescontoCupom());
        assertEquals(33.10, response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(-20.09, response.getAjustePagamento());
        assertEquals(381.74, response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(381.74, response.getValorParcela());
    }

    @Test
    void testExemplo2() throws ErroCheckout {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Camiseta", 79.90, 2, 0.30),
            new ItemPedido("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom(null);
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);

        ResumoResponse response = service.calcularResumo(request);

        assertEquals(409.70, response.getSubtotalProdutos());
        assertEquals(0.00, response.getDescontoCupom());
        assertEquals(15.60, response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(6, response.getParcelas());
        assertTrue(response.getValorParcela() > 0);
        assertTrue(response.getAjustePagamento() > 0);
        assertTrue(response.getTotalFinal() > 425.30);
    }

    @Test
    void testExemplo3() throws ErroCheckout {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Collections.singletonList(
            new ItemPedido("Fone", 199.90, 2, 0.25)
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);

        ResumoResponse response = service.calcularResumo(request);

        assertEquals(399.80, response.getSubtotalProdutos());
        assertEquals(50.00, response.getDescontoCupom());
        assertEquals(18.00, response.getFrete());
        assertEquals(0, response.getPrazoEntregaDias());
        assertEquals(3.49, response.getAjustePagamento());
        assertEquals(371.29, response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(371.29, response.getValorParcela());
    }

    @Test
    void testExemplo4() throws ErroCheckout {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
            new ItemPedido("Meia", 19.90, 7, 0.10),
            new ItemPedido("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);

        ResumoResponse response = service.calcularResumo(request);

        assertEquals(299.10, response.getSubtotalProdutos());
        assertEquals(39.80, response.getDescontoCupom());
        assertEquals(0.00, response.getFrete());
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(0.00, response.getAjustePagamento());
        assertEquals(259.30, response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(86.43, response.getValorParcela());
    }

    @Test
    void testPedidoInvalido() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Collections.emptyList());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> service.calcularResumo(request));
        assertEquals("PEDIDO_INVALIDO", e.getCodigo());
    }

    @Test
    void testModalidadeInvalida() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Collections.singletonList(
            new ItemPedido("Produto", 100, 1, 0.5)
        ));
        request.setModalidadeEntrega("INVALIDA");
        request.setFormaPagamento("PIX");

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> service.calcularResumo(request));
        assertEquals("MODALIDADE_INVALIDA", e.getCodigo());
    }

    @Test
    void testModalidadeIndisponivel() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Collections.singletonList(
            new ItemPedido("Produto", 100, 1, 6.0)
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> service.calcularResumo(request));
        assertEquals("MODALIDADE_INDISPONIVEL", e.getCodigo());
    }

    @Test
    void testCupomInvalido() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Collections.singletonList(
            new ItemPedido("Produto", 100, 1, 0.5)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("INVALIDO");
        request.setFormaPagamento("PIX");

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> service.calcularResumo(request));
        assertEquals("CUPOM_INVALIDO", e.getCodigo());
    }

    @Test
    void testCupomNaoAplicavel() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Collections.singletonList(
            new ItemPedido("Produto", 100, 1, 0.5)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> service.calcularResumo(request));
        assertEquals("CUPOM_NAO_APLICAVEL", e.getCodigo());
    }

    @Test
    void testFormaPagamentoInvalida() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Collections.singletonList(
            new ItemPedido("Produto", 100, 1, 0.5)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("INVALIDA");

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> service.calcularResumo(request));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", e.getCodigo());
    }

    @Test
    void testParcelamentoInvalidoPix() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Collections.singletonList(
            new ItemPedido("Produto", 100, 1, 0.5)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(2);

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> service.calcularResumo(request));
        assertEquals("PARCELAMENTO_INVALIDO", e.getCodigo());
    }

    @Test
    void testFormaPagamentoIndisponivel() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Collections.singletonList(
            new ItemPedido("Produto", 1001, 1, 0.5)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("BOLETO");

        ErroCheckout e = assertThrows(ErroCheckout.class, () -> service.calcularResumo(request));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", e.getCodigo());
    }

    @Test
    void testFretegratisCupom() throws ErroCheckout {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Collections.singletonList(
            new ItemPedido("Produto", 100, 1, 1.0)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("FRETEGRATIS");
        request.setFormaPagamento("PIX");

        ResumoResponse response = service.calcularResumo(request);

        assertEquals(100.00, response.getSubtotalProdutos());
        double freteEsperado = 25.00 + 4.50 * 1.0;
        assertEquals(freteEsperado, response.getFrete(), 0.01);
        assertEquals(freteEsperado, response.getDescontoCupom(), 0.01);
        double desconto_pix = 100.0 * 0.05;
        assertEquals(100.0 - desconto_pix, response.getTotalFinal(), 0.01);
    }

    @Test
    void testParcelasDefault() throws ErroCheckout {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Collections.singletonList(
            new ItemPedido("Produto", 100, 1, 0.5)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(null);

        ResumoResponse response = service.calcularResumo(request);

        assertEquals(1, response.getParcelas());
    }
}
