package com.loja.checkout.service;

import com.loja.checkout.api.CartItem;
import com.loja.checkout.api.CheckoutRequest;
import com.loja.checkout.api.OrderSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        checkoutService = new CheckoutService();
    }

    @Test
    void exemplo1() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of(
                new CartItem("Camiseta", 79.90, 2, 0.30),
                new CartItem("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals(409.70, resultado.getSubtotalProdutos(), 0.01);
        assertEquals(40.97, resultado.getDescontoCupom(), 0.01);
        assertEquals(33.10, resultado.getFrete(), 0.01);
        assertEquals(2, resultado.getPrazoEntregaDias());
        assertEquals(10.24, resultado.getSeguro(), 0.01);
        assertEquals(-20.60, resultado.getAjustePagamento(), 0.01);
        assertEquals(391.47, resultado.getTotalFinal(), 0.01);
        assertEquals(1, resultado.getParcelas());
        assertEquals(391.47, resultado.getValorParcela(), 0.01);
        assertEquals(0.00, resultado.getCreditoProximaCompra(), 0.01);
        assertEquals(false, resultado.getBrinde());
    }

    @Test
    void exemplo2() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of(
                new CartItem("Camiseta", 79.90, 2, 0.30),
                new CartItem("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);
        request.setNivelClube("PRATA");
        request.setRegiao("CENTRO_OESTE");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals(409.70, resultado.getSubtotalProdutos(), 0.01);
        assertEquals(0.00, resultado.getDescontoCupom(), 0.01);
        assertEquals(15.60, resultado.getFrete(), 0.01);
        assertEquals(7, resultado.getPrazoEntregaDias());
        assertEquals(6.15, resultado.getSeguro(), 0.01);
        assertEquals(30.55, resultado.getAjustePagamento(), 0.01);
        assertEquals(462.00, resultado.getTotalFinal(), 0.01);
        assertEquals(6, resultado.getParcelas());
        assertEquals(77.00, resultado.getValorParcela(), 0.01);
        assertEquals(8.19, resultado.getCreditoProximaCompra(), 0.01);
        assertEquals(false, resultado.getBrinde());
    }

    @Test
    void exemplo3() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of(
                new CartItem("Fone", 199.90, 2, 0.25)
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORDESTE");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals(399.80, resultado.getSubtotalProdutos(), 0.01);
        assertEquals(50.00, resultado.getDescontoCupom(), 0.01);
        assertEquals(18.00, resultado.getFrete(), 0.01);
        assertEquals(0, resultado.getPrazoEntregaDias());
        assertEquals(8.00, resultado.getSeguro(), 0.01);
        assertEquals(3.49, resultado.getAjustePagamento(), 0.01);
        assertEquals(379.29, resultado.getTotalFinal(), 0.01);
        assertEquals(1, resultado.getParcelas());
        assertEquals(379.29, resultado.getValorParcela(), 0.01);
        assertEquals(0.00, resultado.getCreditoProximaCompra(), 0.01);
        assertEquals(false, resultado.getBrinde());
    }

    @Test
    void exemplo4() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of(
                new CartItem("Meia", 19.90, 7, 0.10),
                new CartItem("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);
        request.setNivelClube("PRATA");
        request.setRegiao("SUL");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals(299.10, resultado.getSubtotalProdutos(), 0.01);
        assertEquals(39.80, resultado.getDescontoCupom(), 0.01);
        assertEquals(0.00, resultado.getFrete(), 0.01);
        assertEquals(1, resultado.getPrazoEntregaDias());
        assertEquals(2.99, resultado.getSeguro(), 0.01);
        assertEquals(0.00, resultado.getAjustePagamento(), 0.01);
        assertEquals(262.29, resultado.getTotalFinal(), 0.01);
        assertEquals(3, resultado.getParcelas());
        assertEquals(87.43, resultado.getValorParcela(), 0.01);
        assertEquals(5.98, resultado.getCreditoProximaCompra(), 0.01);
        assertEquals(false, resultado.getBrinde());
    }

    @Test
    void exemplo5() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of(
                new CartItem("Camiseta", 79.90, 2, 0.30),
                new CartItem("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("OURO");
        request.setRegiao("SUDESTE");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals(409.70, resultado.getSubtotalProdutos(), 0.01);
        assertEquals(0.00, resultado.getDescontoCupom(), 0.01);
        assertEquals(0.00, resultado.getFrete(), 0.01);
        assertEquals(2, resultado.getPrazoEntregaDias());
        assertEquals(4.10, resultado.getSeguro(), 0.01);
        assertEquals(-20.69, resultado.getAjustePagamento(), 0.01);
        assertEquals(393.11, resultado.getTotalFinal(), 0.01);
        assertEquals(1, resultado.getParcelas());
        assertEquals(393.11, resultado.getValorParcela(), 0.01);
        assertEquals(20.48, resultado.getCreditoProximaCompra(), 0.01);
        assertEquals(false, resultado.getBrinde());
    }

    @Test
    void erroCarrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals("PEDIDO_INVALIDO", resultado.getErro());
    }

    @Test
    void erroNivelClubeInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of(
                new CartItem("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("PLATINUM");
        request.setRegiao("NORTE");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals("NIVEL_CLUBE_INVALIDO", resultado.getErro());
    }

    @Test
    void erroRegiaoInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of(
                new CartItem("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("MARIANA");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals("REGIAO_INVALIDA", resultado.getErro());
    }

    @Test
    void erroModalidadeInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of(
                new CartItem("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("DRONES");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals("MODALIDADE_INVALIDA", resultado.getErro());
    }

    @Test
    void erroModalidadeIndisponivel() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of(
                new CartItem("Camiseta", 79.90, 2, 3.0)
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals("MODALIDADE_INDISPONIVEL", resultado.getErro());
    }

    @Test
    void erroCupomInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of(
                new CartItem("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("PROMOCAO2025");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals("CUPOM_INVALIDO", resultado.getErro());
    }

    @Test
    void erroCupomNaoAplicavel() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of(
                new CartItem("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals("CUPOM_NAO_APLICAVEL", resultado.getErro());
    }

    @Test
    void erroFormaPagamentoInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of(
                new CartItem("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("CHEQUE");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals("FORMA_PAGAMENTO_INVALIDA", resultado.getErro());
    }

    @Test
    void erroParcelamentoInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of(
                new CartItem("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(2);
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals("PARCELAMENTO_INVALIDO", resultado.getErro());
    }

    @Test
    void erroFormaPagamentoIndisponivel() {
        CheckoutRequest request = new CheckoutRequest();
        List<CartItem> itens = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            itens.add(new CartItem("Tênis", 249.90, 1, 1.0));
        }
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("BOLETO");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", resultado.getErro());
    }

    @Test
    void precoNegativo() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of(
                new CartItem("Camiseta", -79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals("PEDIDO_INVALIDO", resultado.getErro());
    }

    @Test
    void quantidadeZero() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of(
                new CartItem("Camiseta", 79.90, 0, 0.30)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals("PEDIDO_INVALIDO", resultado.getErro());
    }

    @Test
    void pesoNegativo() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of(
                new CartItem("Camiseta", 79.90, 2, -0.30)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals("PEDIDO_INVALIDO", resultado.getErro());
    }

    @Test
    void ouroComBrinde() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of(
                new CartItem("Camiseta", 79.90, 2, 0.30),
                new CartItem("Tênis", 249.90, 2, 1.20)
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("OURO");
        request.setRegiao("SUDESTE");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals(true, resultado.getBrinde());
    }

    @Test
    void fretegratisComCupom() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of(
                new CartItem("Camiseta", 79.90, 2, 0.30),
                new CartItem("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("FRETEGRATIS");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals(33.10, resultado.getFrete(), 0.01);
        assertEquals(33.10, resultado.getDescontoCupom(), 0.01);
    }

    @Test
    void cartaoAte3Parcelas() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(List.of(
                new CartItem("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        OrderSummary resultado = checkoutService.calcularResumo(request);

        assertEquals(0.00, resultado.getAjustePagamento(), 0.01);
        assertEquals(3, resultado.getParcelas());
    }
}
