package com.loja.checkout;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
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

        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho camiseta = new ItemCarrinho();
        camiseta.setNome("Camiseta");
        camiseta.setPrecoUnitario(new BigDecimal("79.90"));
        camiseta.setQuantidade(2);
        camiseta.setPesoKg(new BigDecimal("0.30"));
        itens.add(camiseta);

        ItemCarrinho tenis = new ItemCarrinho();
        tenis.setNome("Tênis");
        tenis.setPrecoUnitario(new BigDecimal("249.90"));
        tenis.setQuantidade(1);
        tenis.setPesoKg(new BigDecimal("1.20"));
        itens.add(tenis);

        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento(FormaPagamento.PIX);
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

        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho camiseta = new ItemCarrinho();
        camiseta.setNome("Camiseta");
        camiseta.setPrecoUnitario(new BigDecimal("79.90"));
        camiseta.setQuantidade(2);
        camiseta.setPesoKg(new BigDecimal("0.30"));
        itens.add(camiseta);

        ItemCarrinho tenis = new ItemCarrinho();
        tenis.setNome("Tênis");
        tenis.setPrecoUnitario(new BigDecimal("249.90"));
        tenis.setQuantidade(1);
        tenis.setPesoKg(new BigDecimal("1.20"));
        itens.add(tenis);

        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setFormaPagamento(FormaPagamento.CARTAO);
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

        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho fone = new ItemCarrinho();
        fone.setNome("Fone");
        fone.setPrecoUnitario(new BigDecimal("199.90"));
        fone.setQuantidade(2);
        fone.setPesoKg(new BigDecimal("0.25"));
        itens.add(fone);

        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.MOTOBOY);
        request.setCupom("MENOS50");
        request.setFormaPagamento(FormaPagamento.BOLETO);
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

        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho meia = new ItemCarrinho();
        meia.setNome("Meia");
        meia.setPrecoUnitario(new BigDecimal("19.90"));
        meia.setQuantidade(7);
        meia.setPesoKg(new BigDecimal("0.10"));
        itens.add(meia);

        ItemCarrinho camiseta = new ItemCarrinho();
        camiseta.setNome("Camiseta");
        camiseta.setPrecoUnitario(new BigDecimal("79.90"));
        camiseta.setQuantidade(2);
        camiseta.setPesoKg(new BigDecimal("0.30"));
        itens.add(camiseta);

        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.RETIRADA_LOJA);
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento(FormaPagamento.CARTAO);
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

        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho camiseta = new ItemCarrinho();
        camiseta.setNome("Camiseta");
        camiseta.setPrecoUnitario(new BigDecimal("79.90"));
        camiseta.setQuantidade(2);
        camiseta.setPesoKg(new BigDecimal("0.30"));
        itens.add(camiseta);

        ItemCarrinho tenis = new ItemCarrinho();
        tenis.setNome("Tênis");
        tenis.setPrecoUnitario(new BigDecimal("249.90"));
        tenis.setQuantidade(1);
        tenis.setPesoKg(new BigDecimal("1.20"));
        itens.add(tenis);

        request.setItens(itens);
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
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
}
