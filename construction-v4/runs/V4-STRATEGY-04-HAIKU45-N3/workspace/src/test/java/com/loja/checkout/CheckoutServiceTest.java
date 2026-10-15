package com.loja.checkout;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.Item;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.ModalidadeEntrega;
import com.loja.checkout.model.NivelClube;
import com.loja.checkout.model.Region;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CheckoutServiceTest {

    @Autowired
    private CheckoutService checkoutService;

    @Test
    public void exemplo1() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 2, 0.30),
            new Item("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setParcelas(1);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Region.NORTE);

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertTrue(response.getSubtotalProdutos().compareTo(new BigDecimal("409.70")) == 0);
        assertTrue(response.getDescontoCupom().compareTo(new BigDecimal("40.97")) == 0);
        assertTrue(response.getFrete().compareTo(new BigDecimal("33.10")) == 0);
        assertEquals(2, response.getPrazoEntregaDias());
        assertTrue(response.getSeguro().compareTo(new BigDecimal("10.24")) == 0);
        assertTrue(response.getAjustePagamento().compareTo(new BigDecimal("-20.60")) == 0);
        assertTrue(response.getTotalFinal().compareTo(new BigDecimal("391.47")) == 0);
        assertEquals(1, response.getParcelas());
        assertTrue(response.getValorParcela().compareTo(new BigDecimal("391.47")) == 0);
        assertTrue(response.getCreditoProximaCompra().compareTo(BigDecimal.ZERO) >= 0);
        assertEquals(false, response.getBrinde());
    }

    @Test
    public void exemplo2() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 2, 0.30),
            new Item("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setCupom(null);
        request.setFormaPagamento(FormaPagamento.CARTAO);
        request.setParcelas(6);
        request.setNivelClube(NivelClube.PRATA);
        request.setRegiao(Region.CENTRO_OESTE);

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertTrue(response.getSubtotalProdutos().compareTo(new BigDecimal("409.70")) == 0);
        assertTrue(response.getDescontoCupom().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(response.getFrete().compareTo(new BigDecimal("15.60")) == 0);
        assertEquals(7, response.getPrazoEntregaDias());
        assertTrue(response.getSeguro().compareTo(new BigDecimal("6.15")) == 0);
        assertTrue(response.getAjustePagamento().compareTo(new BigDecimal("30.55")) == 0);
        assertTrue(response.getTotalFinal().compareTo(new BigDecimal("462.00")) == 0);
        assertEquals(6, response.getParcelas());
        assertTrue(response.getValorParcela().compareTo(new BigDecimal("77.00")) == 0);
        assertTrue(response.getCreditoProximaCompra().compareTo(new BigDecimal("8.19")) == 0);
        assertEquals(false, response.getBrinde());
    }

    @Test
    public void exemplo3() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Fone", 199.90, 2, 0.25)
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.MOTOBOY);
        request.setCupom("MENOS50");
        request.setFormaPagamento(FormaPagamento.BOLETO);
        request.setParcelas(1);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Region.NORDESTE);

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertTrue(response.getSubtotalProdutos().compareTo(new BigDecimal("399.80")) == 0);
        assertTrue(response.getDescontoCupom().compareTo(new BigDecimal("50.00")) == 0);
        assertTrue(response.getFrete().compareTo(new BigDecimal("18.00")) == 0);
        assertEquals(0, response.getPrazoEntregaDias());
        assertTrue(response.getSeguro().compareTo(new BigDecimal("8.00")) == 0);
        assertTrue(response.getAjustePagamento().compareTo(new BigDecimal("3.49")) == 0);
        assertTrue(response.getTotalFinal().compareTo(new BigDecimal("379.29")) == 0);
        assertEquals(1, response.getParcelas());
        assertTrue(response.getValorParcela().compareTo(new BigDecimal("379.29")) == 0);
        assertTrue(response.getCreditoProximaCompra().compareTo(BigDecimal.ZERO) >= 0);
        assertEquals(false, response.getBrinde());
    }

    @Test
    public void exemplo4() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Meia", 19.90, 7, 0.10),
            new Item("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.RETIRADA_LOJA);
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento(FormaPagamento.CARTAO);
        request.setParcelas(3);
        request.setNivelClube(NivelClube.PRATA);
        request.setRegiao(Region.SUL);

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertTrue(response.getSubtotalProdutos().compareTo(new BigDecimal("299.10")) == 0);
        assertTrue(response.getDescontoCupom().compareTo(new BigDecimal("39.80")) == 0);
        assertTrue(response.getFrete().compareTo(BigDecimal.ZERO) >= 0);
        assertEquals(1, response.getPrazoEntregaDias());
        assertTrue(response.getSeguro().compareTo(new BigDecimal("2.99")) == 0);
        assertTrue(response.getAjustePagamento().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(response.getTotalFinal().compareTo(new BigDecimal("262.29")) == 0);
        assertEquals(3, response.getParcelas());
        assertTrue(response.getValorParcela().compareTo(new BigDecimal("87.43")) == 0);
        assertTrue(response.getCreditoProximaCompra().compareTo(new BigDecimal("5.98")) == 0);
        assertEquals(false, response.getBrinde());
    }

    @Test
    public void exemplo5() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 2, 0.30),
            new Item("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setCupom(null);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setParcelas(1);
        request.setNivelClube(NivelClube.OURO);
        request.setRegiao(Region.SUDESTE);

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertTrue(response.getSubtotalProdutos().compareTo(new BigDecimal("409.70")) == 0);
        assertTrue(response.getDescontoCupom().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(response.getFrete().compareTo(BigDecimal.ZERO) >= 0);
        assertEquals(2, response.getPrazoEntregaDias());
        assertTrue(response.getSeguro().compareTo(new BigDecimal("4.10")) == 0);
        assertTrue(response.getAjustePagamento().compareTo(new BigDecimal("-20.69")) == 0);
        assertTrue(response.getTotalFinal().compareTo(new BigDecimal("393.11")) == 0);
        assertEquals(1, response.getParcelas());
        assertTrue(response.getValorParcela().compareTo(new BigDecimal("393.11")) == 0);
        assertTrue(response.getCreditoProximaCompra().compareTo(new BigDecimal("20.48")) == 0);
        assertEquals(false, response.getBrinde());
    }
}
