package com.loja;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ItemRequest;
import com.loja.service.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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

        List<ItemRequest> itens = new ArrayList<>();
        ItemRequest item1 = new ItemRequest();
        item1.setNome("Camiseta");
        item1.setPrecoUnitario(new BigDecimal("79.90"));
        item1.setQuantidade(2);
        item1.setPesoKg(new BigDecimal("0.30"));
        itens.add(item1);

        ItemRequest item2 = new ItemRequest();
        item2.setNome("Tênis");
        item2.setPrecoUnitario(new BigDecimal("249.90"));
        item2.setQuantidade(1);
        item2.setPesoKg(new BigDecimal("1.20"));
        itens.add(item2);

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
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo2() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemRequest> itens = new ArrayList<>();
        ItemRequest item1 = new ItemRequest();
        item1.setNome("Camiseta");
        item1.setPrecoUnitario(new BigDecimal("79.90"));
        item1.setQuantidade(2);
        item1.setPesoKg(new BigDecimal("0.30"));
        itens.add(item1);

        ItemRequest item2 = new ItemRequest();
        item2.setNome("Tênis");
        item2.setPrecoUnitario(new BigDecimal("249.90"));
        item2.setQuantidade(1);
        item2.setPesoKg(new BigDecimal("1.20"));
        itens.add(item2);

        request.setItens(itens);
        request.setModalidadeEntrega("ECONOMICA");
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
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo3() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemRequest> itens = new ArrayList<>();
        ItemRequest item1 = new ItemRequest();
        item1.setNome("Fone");
        item1.setPrecoUnitario(new BigDecimal("199.90"));
        item1.setQuantidade(2);
        item1.setPesoKg(new BigDecimal("0.25"));
        itens.add(item1);

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
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo4() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemRequest> itens = new ArrayList<>();
        ItemRequest item1 = new ItemRequest();
        item1.setNome("Meia");
        item1.setPrecoUnitario(new BigDecimal("19.90"));
        item1.setQuantidade(7);
        item1.setPesoKg(new BigDecimal("0.10"));
        itens.add(item1);

        ItemRequest item2 = new ItemRequest();
        item2.setNome("Camiseta");
        item2.setPrecoUnitario(new BigDecimal("79.90"));
        item2.setQuantidade(2);
        item2.setPesoKg(new BigDecimal("0.30"));
        itens.add(item2);

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
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo5() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemRequest> itens = new ArrayList<>();
        ItemRequest item1 = new ItemRequest();
        item1.setNome("Camiseta");
        item1.setPrecoUnitario(new BigDecimal("79.90"));
        item1.setQuantidade(2);
        item1.setPesoKg(new BigDecimal("0.30"));
        itens.add(item1);

        ItemRequest item2 = new ItemRequest();
        item2.setNome("Tênis");
        item2.setPrecoUnitario(new BigDecimal("249.90"));
        item2.setQuantidade(1);
        item2.setPesoKg(new BigDecimal("1.20"));
        itens.add(item2);

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
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
        assertFalse(response.getBrinde());
    }
}
