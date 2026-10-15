package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemDto;
import com.loja.checkout.exception.CheckoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CheckoutServiceTest {

    private CheckoutService service;

    @BeforeEach
    public void setup() {
        service = new CheckoutService();
    }

    @Test
    public void exemplo1_BemVindo10_Pix() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemDto> itens = new ArrayList<>();
        itens.add(new ItemDto("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(new ItemDto("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

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
    public void exemplo2_SemCupom_Cartao6x() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemDto> itens = new ArrayList<>();
        itens.add(new ItemDto("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        itens.add(new ItemDto("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

        request.setItens(itens);
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom(null);
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("30.11"), response.getAjustePagamento());
        assertEquals(new BigDecimal("455.41"), response.getTotalFinal());
        assertEquals(6, response.getParcelas());
        assertEquals(new BigDecimal("75.90"), response.getValorParcela());
    }

    @Test
    public void exemplo3_Menos50_Boleto() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemDto> itens = new ArrayList<>();
        itens.add(new ItemDto("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));

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
    public void exemplo4_Leve3Pague2_Cartao3x() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemDto> itens = new ArrayList<>();
        itens.add(new ItemDto("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")));
        itens.add(new ItemDto("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));

        request.setItens(itens);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("299.10"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), response.getFrete());
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("0.00"), response.getAjustePagamento());
        assertEquals(new BigDecimal("259.30"), response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(new BigDecimal("86.43"), response.getValorParcela());
    }

    @Test
    public void erroCarrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(new ArrayList<>());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("PEDIDO_INVALIDO", ex.getCodigo());
    }

    @Test
    public void erroItemComPrecoNegativo() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemDto> itens = new ArrayList<>();
        itens.add(new ItemDto("Produto", new BigDecimal("-10.00"), 1, new BigDecimal("0.5")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("PEDIDO_INVALIDO", ex.getCodigo());
    }

    @Test
    public void erroModalidadeInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemDto> itens = new ArrayList<>();
        itens.add(new ItemDto("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.5")));

        request.setItens(itens);
        request.setModalidadeEntrega("INVALIDA");
        request.setFormaPagamento("PIX");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("MODALIDADE_INVALIDA", ex.getCodigo());
    }

    @Test
    public void erroMotoboySuperacincoKg() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemDto> itens = new ArrayList<>();
        itens.add(new ItemDto("Produto", new BigDecimal("10.00"), 1, new BigDecimal("6.0")));

        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("MODALIDADE_INDISPONIVEL", ex.getCodigo());
    }

    @Test
    public void erroCupomInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemDto> itens = new ArrayList<>();
        itens.add(new ItemDto("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.5")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("CUPOMINVALIDO");
        request.setFormaPagamento("PIX");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("CUPOM_INVALIDO", ex.getCodigo());
    }

    @Test
    public void erroCupomNaoAplicavelMenos50() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemDto> itens = new ArrayList<>();
        itens.add(new ItemDto("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("CUPOM_NAO_APLICAVEL", ex.getCodigo());
    }

    @Test
    public void erroFormaPagamentoInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemDto> itens = new ArrayList<>();
        itens.add(new ItemDto("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.5")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("INVALIDA");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("FORMA_PAGAMENTO_INVALIDA", ex.getCodigo());
    }

    @Test
    public void erroParcelamentoBoleto() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemDto> itens = new ArrayList<>();
        itens.add(new ItemDto("Produto", new BigDecimal("10.00"), 1, new BigDecimal("0.5")));

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(2);

        CheckoutException ex = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("PARCELAMENTO_INVALIDO", ex.getCodigo());
    }

    @Test
    public void erroBoleto_acimaDeMilReais() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemDto> itens = new ArrayList<>();
        itens.add(new ItemDto("Produto", new BigDecimal("1000.01"), 1, new BigDecimal("0.5")));

        request.setItens(itens);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);

        CheckoutException ex = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ex.getCodigo());
    }
}
