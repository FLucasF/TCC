package com.loja.checkout.service;

import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.model.CheckoutResponse;
import com.loja.checkout.model.ItemCarrinho;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CheckoutServiceTest {

    private CheckoutService service;

    @BeforeEach
    void setUp() {
        service = new CheckoutService();
    }

    @Test
    void exemplo1() {
        CheckoutRequest request = new CheckoutRequest();

        ItemCarrinho camiseta = new ItemCarrinho();
        camiseta.setNome("Camiseta");
        camiseta.setPrecoUnitario(79.90);
        camiseta.setQuantidade(2);
        camiseta.setPesoKg(0.30);

        ItemCarrinho tenis = new ItemCarrinho();
        tenis.setNome("Tênis");
        tenis.setPrecoUnitario(249.90);
        tenis.setQuantidade(1);
        tenis.setPesoKg(1.20);

        request.setItens(Arrays.asList(camiseta, tenis));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(409.70, response.getSubtotalProdutos());
        assertEquals(40.97, response.getDescontoCupom());
        assertEquals(33.10, response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(10.24, response.getSeguro());
        assertEquals(-20.60, response.getAjustePagamento());
        assertEquals(391.47, response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(391.47, response.getValorParcela());
        assertEquals(0.00, response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo2() {
        CheckoutRequest request = new CheckoutRequest();

        ItemCarrinho camiseta = new ItemCarrinho();
        camiseta.setNome("Camiseta");
        camiseta.setPrecoUnitario(79.90);
        camiseta.setQuantidade(2);
        camiseta.setPesoKg(0.30);

        ItemCarrinho tenis = new ItemCarrinho();
        tenis.setNome("Tênis");
        tenis.setPrecoUnitario(249.90);
        tenis.setQuantidade(1);
        tenis.setPesoKg(1.20);

        request.setItens(Arrays.asList(camiseta, tenis));
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);
        request.setNivelClube("PRATA");
        request.setRegiao("CENTRO_OESTE");

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(409.70, response.getSubtotalProdutos());
        assertEquals(0.00, response.getDescontoCupom());
        assertEquals(15.60, response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(6.15, response.getSeguro());
        assertEquals(462.00, response.getTotalFinal());
        assertEquals(6, response.getParcelas());
        assertEquals(77.00, response.getValorParcela());
        assertEquals(8.19, response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo3() {
        CheckoutRequest request = new CheckoutRequest();

        ItemCarrinho fone = new ItemCarrinho();
        fone.setNome("Fone");
        fone.setPrecoUnitario(199.90);
        fone.setQuantidade(2);
        fone.setPesoKg(0.25);

        request.setItens(Arrays.asList(fone));
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORDESTE");

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(399.80, response.getSubtotalProdutos());
        assertEquals(50.00, response.getDescontoCupom());
        assertEquals(18.00, response.getFrete());
        assertEquals(0, response.getPrazoEntregaDias());
        assertEquals(8.00, response.getSeguro());
        assertEquals(3.49, response.getAjustePagamento());
        assertEquals(379.29, response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(379.29, response.getValorParcela());
        assertEquals(0.00, response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo4() {
        CheckoutRequest request = new CheckoutRequest();

        ItemCarrinho meia = new ItemCarrinho();
        meia.setNome("Meia");
        meia.setPrecoUnitario(19.90);
        meia.setQuantidade(7);
        meia.setPesoKg(0.10);

        ItemCarrinho camiseta = new ItemCarrinho();
        camiseta.setNome("Camiseta");
        camiseta.setPrecoUnitario(79.90);
        camiseta.setQuantidade(2);
        camiseta.setPesoKg(0.30);

        request.setItens(Arrays.asList(meia, camiseta));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);
        request.setNivelClube("PRATA");
        request.setRegiao("SUL");

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(299.10, response.getSubtotalProdutos());
        assertEquals(39.80, response.getDescontoCupom());
        assertEquals(0.00, response.getFrete());
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(2.99, response.getSeguro());
        assertEquals(0.00, response.getAjustePagamento());
        assertEquals(262.29, response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(87.43, response.getValorParcela());
        assertEquals(5.98, response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    void exemplo5() {
        CheckoutRequest request = new CheckoutRequest();

        ItemCarrinho camiseta = new ItemCarrinho();
        camiseta.setNome("Camiseta");
        camiseta.setPrecoUnitario(79.90);
        camiseta.setQuantidade(2);
        camiseta.setPesoKg(0.30);

        ItemCarrinho tenis = new ItemCarrinho();
        tenis.setNome("Tênis");
        tenis.setPrecoUnitario(249.90);
        tenis.setQuantidade(1);
        tenis.setPesoKg(1.20);

        request.setItens(Arrays.asList(camiseta, tenis));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("OURO");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(409.70, response.getSubtotalProdutos());
        assertEquals(0.00, response.getDescontoCupom());
        assertEquals(0.00, response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(4.10, response.getSeguro());
        assertEquals(-20.69, response.getAjustePagamento());
        assertEquals(393.11, response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(393.11, response.getValorParcela());
        assertEquals(20.48, response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }
}
