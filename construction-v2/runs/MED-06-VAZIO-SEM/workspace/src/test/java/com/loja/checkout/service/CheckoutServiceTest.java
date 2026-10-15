package com.loja.checkout.service;

import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.model.CheckoutResponse;
import com.loja.checkout.model.Item;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CheckoutServiceTest {
    private CheckoutService service = new CheckoutService();

    @Test
    public void testExemplo1() throws CheckoutException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Camiseta", 79.90, 2, 0.30));
        itens.add(new Item("Tênis", 249.90, 1, 1.20));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), response.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("-20.09"), response.getAjustePagamento());
        assertEquals(new BigDecimal("381.74"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("381.74"), response.getValorParcela());
    }

    @Test
    public void testCartaoSemJurosAte3Parcelas() throws CheckoutException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", 300.0, 1, 1.0));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom(null);
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("300.00"), response.getSubtotalProdutos());
        assertEquals(0, response.getDescontoCupom().compareTo(new BigDecimal("0.00")));
        assertEquals(0, response.getFrete().compareTo(new BigDecimal("0.00")));
        assertEquals(new BigDecimal("300.00"), response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(new BigDecimal("100.00"), response.getValorParcela());
    }

    @Test
    public void testExemplo3() throws CheckoutException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Fone", 199.90, 2, 0.25));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("399.80"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), response.getFrete());
        assertEquals(0, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("3.49"), response.getAjustePagamento());
        assertEquals(new BigDecimal("371.29"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("371.29"), response.getValorParcela());
    }

    @Test
    public void testExemplo4() throws CheckoutException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Meia", 19.90, 7, 0.10));
        itens.add(new Item("Camiseta", 79.90, 2, 0.30));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(0, response.getSubtotalProdutos().compareTo(new BigDecimal("299.10")));
        assertEquals(0, response.getDescontoCupom().compareTo(new BigDecimal("39.80")));
        assertEquals(0, response.getFrete().compareTo(new BigDecimal("0.00")));
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(0, response.getAjustePagamento().compareTo(new BigDecimal("0.00")));
        assertEquals(0, response.getTotalFinal().compareTo(new BigDecimal("259.30")));
        assertEquals(3, response.getParcelas());
        assertEquals(0, response.getValorParcela().compareTo(new BigDecimal("86.43")));
    }

    @Test
    public void testCarrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(new ArrayList<>());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("PEDIDO_INVALIDO", exception.getErrorCode());
    }

    @Test
    public void testPrecoZero() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", 0.0, 1, 0.5));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("PEDIDO_INVALIDO", exception.getErrorCode());
    }

    @Test
    public void testQuantidadeZero() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", 10.0, 0, 0.5));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("PEDIDO_INVALIDO", exception.getErrorCode());
    }

    @Test
    public void testModalidadeInvalida() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", 10.0, 1, 0.5));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("INVALIDA");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("MODALIDADE_INVALIDA", exception.getErrorCode());
    }

    @Test
    public void testMotoboyAcimaDe5kg() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", 100.0, 1, 5.5));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("MODALIDADE_INDISPONIVEL", exception.getErrorCode());
    }

    @Test
    public void testCupomInvalido() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", 10.0, 1, 0.5));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("CUPOMINVALIDO");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("CUPOM_INVALIDO", exception.getErrorCode());
    }

    @Test
    public void testMenos50AbaixoDeMinimo() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", 100.0, 1, 0.5));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("CUPOM_NAO_APLICAVEL", exception.getErrorCode());
    }

    @Test
    public void testFormaPagamentoInvalida() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", 10.0, 1, 0.5));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("INVALIDA");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getErrorCode());
    }

    @Test
    public void testPixNaoParcelavelAcima1() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", 10.0, 1, 0.5));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(2);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("PARCELAMENTO_INVALIDO", exception.getErrorCode());
    }

    @Test
    public void testCartaoParcelasInvalidas() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", 10.0, 1, 0.5));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(13);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("PARCELAMENTO_INVALIDO", exception.getErrorCode());
    }

    @Test
    public void testBoletoAcimaDeLimit() {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", 1001.0, 1, 0.5));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setFormaPagamento("BOLETO");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getErrorCode());
    }

    @Test
    public void testParcelasDefault() throws CheckoutException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", 100.0, 1, 0.5));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(null);

        CheckoutResponse response = service.calcularResumo(request);
        assertEquals(1, response.getParcelas());
    }

    @Test
    public void testFretegratis() throws CheckoutException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", 100.0, 1, 1.0));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("FRETEGRATIS");
        request.setFormaPagamento("PIX");

        CheckoutResponse response = service.calcularResumo(request);
        assertEquals(0, response.getDescontoCupom().compareTo(new BigDecimal("0.00")));
    }

    @Test
    public void testCartaoComJuros() throws CheckoutException {
        List<Item> itens = new ArrayList<>();
        itens.add(new Item("Produto", 100.0, 5, 0.5));

        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);

        CheckoutResponse response = service.calcularResumo(request);
        assertNotNull(response.getTotalFinal());
        assertNotNull(response.getValorParcela());
        assertTrue(response.getAjustePagamento().compareTo(BigDecimal.ZERO) > 0);
    }
}
