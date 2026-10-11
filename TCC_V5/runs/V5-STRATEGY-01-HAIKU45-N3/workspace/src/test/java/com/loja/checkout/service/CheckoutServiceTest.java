package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.Item;
import com.loja.checkout.exception.CheckoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private CheckoutService service;

    @BeforeEach
    void setUp() {
        service = new CheckoutService();
    }

    @Test
    void testExemplo1() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);
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
    void testExemplo2() {
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

        CheckoutResponse response = service.calcularResumo(request);

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
        assertFalse(response.getBrinde());
    }

    @Test
    void testExemplo3() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));

        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);
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
    void testExemplo4() {
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

        CheckoutResponse response = service.calcularResumo(request);

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
        assertFalse(response.getBrinde());
    }

    @Test
    void testExemplo5() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom(null);
        request.setFormaPagamento("PIX");
        request.setParcelas(1);
        request.setNivelClube("OURO");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = service.calcularResumo(request);

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
        assertFalse(response.getBrinde());
    }

    @Test
    void testCarrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(new ArrayList<>());
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void testItemComPrecoZero() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", BigDecimal.ZERO, 1, new BigDecimal("0.5")));

        request.setItens(itens);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void testMotoboySobreLimit() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("5.50")));

        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigoErro());
    }

    @Test
    void testCupomMenos50Invalido() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5")));

        request.setItens(itens);
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom("MENOS50");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigoErro());
    }

    @Test
    void testCupomInexistente() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5")));

        request.setItens(itens);
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom("CUPOM_FAKE");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("CUPOM_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void testBoletoAcimaDoLimite() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("999.00"), 2, new BigDecimal("1.0")));

        request.setItens(itens);
        request.setModalidadeEntrega("ECONOMICA");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigoErro());
    }

    @Test
    void testParcelamentoInvalido() {
        CheckoutRequest request = new CheckoutRequest();

        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5")));

        request.setItens(itens);
        request.setModalidadeEntrega("ECONOMICA");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");
        request.setFormaPagamento("PIX");
        request.setParcelas(2);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });

        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigoErro());
    }
}
