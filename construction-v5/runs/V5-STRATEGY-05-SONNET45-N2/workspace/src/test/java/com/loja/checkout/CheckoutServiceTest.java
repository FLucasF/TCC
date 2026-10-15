package com.loja.checkout;

import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.regiao.Regiao;
import com.loja.checkout.model.request.CheckoutRequest;
import com.loja.checkout.model.request.ItemRequest;
import com.loja.checkout.model.response.CheckoutResponse;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    @Test
    void exemplo1() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemRequest> itens = new ArrayList<>();
        itens.add(criarItem("Camiseta", "79.90", 2, "0.30"));
        itens.add(criarItem("Tênis", "249.90", 1, "1.20"));
        request.setItens(itens);

        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORTE);

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
        assertEquals(false, response.getBrinde());
    }

    @Test
    void exemplo2() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemRequest> itens = new ArrayList<>();
        itens.add(criarItem("Camiseta", "79.90", 2, "0.30"));
        itens.add(criarItem("Tênis", "249.90", 1, "1.20"));
        request.setItens(itens);

        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);
        request.setNivelClube(NivelClube.PRATA);
        request.setRegiao(Regiao.CENTRO_OESTE);

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
        assertEquals(false, response.getBrinde());
    }

    @Test
    void exemplo3() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemRequest> itens = new ArrayList<>();
        itens.add(criarItem("Fone", "199.90", 2, "0.25"));
        request.setItens(itens);

        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.NORDESTE);

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
        assertEquals(false, response.getBrinde());
    }

    @Test
    void exemplo4() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemRequest> itens = new ArrayList<>();
        itens.add(criarItem("Meia", "19.90", 7, "0.10"));
        itens.add(criarItem("Camiseta", "79.90", 2, "0.30"));
        request.setItens(itens);

        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);
        request.setNivelClube(NivelClube.PRATA);
        request.setRegiao(Regiao.SUL);

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
        assertEquals(false, response.getBrinde());
    }

    @Test
    void exemplo5() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemRequest> itens = new ArrayList<>();
        itens.add(criarItem("Camiseta", "79.90", 2, "0.30"));
        itens.add(criarItem("Tênis", "249.90", 1, "1.20"));
        request.setItens(itens);

        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube(NivelClube.OURO);
        request.setRegiao(Regiao.SUDESTE);

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
        assertEquals(false, response.getBrinde());
    }

    private ItemRequest criarItem(String nome, String preco, int quantidade, String peso) {
        ItemRequest item = new ItemRequest();
        item.setNome(nome);
        item.setPrecoUnitario(new BigDecimal(preco));
        item.setQuantidade(quantidade);
        item.setPesoKg(new BigDecimal(peso));
        return item;
    }
}
