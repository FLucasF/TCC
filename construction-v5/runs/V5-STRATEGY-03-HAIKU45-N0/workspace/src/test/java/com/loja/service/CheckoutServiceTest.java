package com.loja.service;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.Item;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CheckoutServiceTest {

    private CheckoutService checkoutService;

    @BeforeEach
    public void setup() {
        checkoutService = new CheckoutService();
    }

    @Test
    public void testExemplo1() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

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
        assertEquals(false, response.getBrinde());
    }

    @Test
    public void testExemplo2() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

        request.setItens(itens);
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom(null);
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);
        request.setNivelClube("PRATA");
        request.setRegiao("CENTRO_OESTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), response.getSeguro());
        assertEquals(new BigDecimal("30.55"), response.getAjustePagamento());
        assertEquals(new BigDecimal("462.00"), response.getTotalFinal());
        assertEquals(6, response.getParcelas());
        assertEquals(new BigDecimal("77.00"), response.getValorParcela());
        assertEquals(new BigDecimal("8.19"), response.getCreditoProximaCompra());
        assertEquals(false, response.getBrinde());
    }

    @Test
    public void testExemplo3() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));

        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORDESTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

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
        assertEquals(false, response.getBrinde());
    }

    @Test
    public void testExemplo4() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")));
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));

        request.setItens(itens);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);
        request.setNivelClube("PRATA");
        request.setRegiao("SUL");

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("299.10"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), response.getFrete());
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), response.getSeguro());
        assertEquals(new BigDecimal("0.00"), response.getAjustePagamento());
        assertEquals(new BigDecimal("262.29"), response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(new BigDecimal("87.43"), response.getValorParcela());
        assertEquals(new BigDecimal("5.98"), response.getCreditoProximaCompra());
        assertEquals(false, response.getBrinde());
    }

    @Test
    public void testExemplo5() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom(null);
        request.setFormaPagamento("PIX");
        request.setNivelClube("OURO");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), response.getSeguro());
        assertEquals(new BigDecimal("-20.69"), response.getAjustePagamento());
        assertEquals(new BigDecimal("393.11"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("393.11"), response.getValorParcela());
        assertEquals(new BigDecimal("20.48"), response.getCreditoProximaCompra());
        assertEquals(false, response.getBrinde());
    }

    @Test
    public void testErroCarrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(new ArrayList<>());
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals("PEDIDO_INVALIDO", response.getErro());
    }

    @Test
    public void testErroNivelClubeInvalido() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("100"), 1, new BigDecimal("1")));
        request.setItens(itens);

        request.setNivelClube("INVALIDO");
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("PIX");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals("NIVEL_CLUBE_INVALIDO", response.getErro());
    }

    @Test
    public void testErroModalidadeIndisponivel() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("100"), 10, new BigDecimal("1")));
        request.setItens(itens);

        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals("MODALIDADE_INDISPONIVEL", response.getErro());
    }

    @Test
    public void testErroCupomInvalido() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("100"), 1, new BigDecimal("1")));
        request.setItens(itens);

        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom("CUPOMINVALIDO");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals("CUPOM_INVALIDO", response.getErro());
    }

    @Test
    public void testErroCupomNaoAplicavel() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("100"), 1, new BigDecimal("1")));
        request.setItens(itens);

        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals("CUPOM_NAO_APLICAVEL", response.getErro());
    }

    @Test
    public void testErroParcelamentoInvalido() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("100"), 1, new BigDecimal("1")));
        request.setItens(itens);

        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("PIX");
        request.setParcelas(2);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals("PARCELAMENTO_INVALIDO", response.getErro());
    }

    @Test
    public void testErroFormaPagamentoIndisponivel() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("500"), 3, new BigDecimal("1")));
        request.setItens(itens);

        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("BOLETO");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", response.getErro());
    }

    @Test
    public void testFreteGratis() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("100"), 1, new BigDecimal("1")));
        request.setItens(itens);

        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom("FRETEGRATIS");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("14.00"), response.getFrete());
        assertEquals(response.getFrete(), response.getDescontoCupom());
    }
}
