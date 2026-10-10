package com.loja.checkout;

import com.loja.checkout.dto.ItemDto;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private final CheckoutService checkoutService = new CheckoutService();

    @Test
    void exemplo1() {
        ResumoRequest request = new ResumoRequest();

        List<ItemDto> itens = new ArrayList<>();

        ItemDto camiseta = new ItemDto();
        camiseta.setNome("Camiseta");
        camiseta.setPrecoUnitario(79.90);
        camiseta.setQuantidade(2);
        camiseta.setPesoKg(0.30);
        itens.add(camiseta);

        ItemDto tenis = new ItemDto();
        tenis.setNome("Tênis");
        tenis.setPrecoUnitario(249.90);
        tenis.setQuantidade(1);
        tenis.setPesoKg(1.20);
        itens.add(tenis);

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        ResumoResponse response = checkoutService.calcularResumo(request);

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
        ResumoRequest request = new ResumoRequest();

        List<ItemDto> itens = new ArrayList<>();

        ItemDto camiseta = new ItemDto();
        camiseta.setNome("Camiseta");
        camiseta.setPrecoUnitario(79.90);
        camiseta.setQuantidade(2);
        camiseta.setPesoKg(0.30);
        itens.add(camiseta);

        ItemDto tenis = new ItemDto();
        tenis.setNome("Tênis");
        tenis.setPrecoUnitario(249.90);
        tenis.setQuantidade(1);
        tenis.setPesoKg(1.20);
        itens.add(tenis);

        request.setItens(itens);
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);
        request.setNivelClube("PRATA");
        request.setRegiao("CENTRO_OESTE");

        ResumoResponse response = checkoutService.calcularResumo(request);

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
        ResumoRequest request = new ResumoRequest();

        List<ItemDto> itens = new ArrayList<>();

        ItemDto fone = new ItemDto();
        fone.setNome("Fone");
        fone.setPrecoUnitario(199.90);
        fone.setQuantidade(2);
        fone.setPesoKg(0.25);
        itens.add(fone);

        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORDESTE");

        ResumoResponse response = checkoutService.calcularResumo(request);

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
        ResumoRequest request = new ResumoRequest();

        List<ItemDto> itens = new ArrayList<>();

        ItemDto meia = new ItemDto();
        meia.setNome("Meia");
        meia.setPrecoUnitario(19.90);
        meia.setQuantidade(7);
        meia.setPesoKg(0.10);
        itens.add(meia);

        ItemDto camiseta = new ItemDto();
        camiseta.setNome("Camiseta");
        camiseta.setPrecoUnitario(79.90);
        camiseta.setQuantidade(2);
        camiseta.setPesoKg(0.30);
        itens.add(camiseta);

        request.setItens(itens);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);
        request.setNivelClube("PRATA");
        request.setRegiao("SUL");

        ResumoResponse response = checkoutService.calcularResumo(request);

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
        ResumoRequest request = new ResumoRequest();

        List<ItemDto> itens = new ArrayList<>();

        ItemDto camiseta = new ItemDto();
        camiseta.setNome("Camiseta");
        camiseta.setPrecoUnitario(79.90);
        camiseta.setQuantidade(2);
        camiseta.setPesoKg(0.30);
        itens.add(camiseta);

        ItemDto tenis = new ItemDto();
        tenis.setNome("Tênis");
        tenis.setPrecoUnitario(249.90);
        tenis.setQuantidade(1);
        tenis.setPesoKg(1.20);
        itens.add(tenis);

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("OURO");
        request.setRegiao("SUDESTE");

        ResumoResponse response = checkoutService.calcularResumo(request);

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
