package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.exception.CheckoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CheckoutServiceTest {

    private CheckoutService service;

    @BeforeEach
    public void setUp() {
        service = new CheckoutService();
    }

    @Test
    public void testExemplo1() {
        CheckoutRequest request = new CheckoutRequest();
        List<CheckoutRequest.Item> itens = new ArrayList<>();

        CheckoutRequest.Item item1 = new CheckoutRequest.Item();
        item1.setNome("Camiseta");
        item1.setPrecoUnitario(79.90);
        item1.setQuantidade(2);
        item1.setPesoKg(0.30);
        itens.add(item1);

        CheckoutRequest.Item item2 = new CheckoutRequest.Item();
        item2.setNome("Tênis");
        item2.setPrecoUnitario(249.90);
        item2.setQuantidade(1);
        item2.setPesoKg(1.20);
        itens.add(item2);

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        CheckoutResponse response = service.calcular(request);

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
        List<CheckoutRequest.Item> itens = new ArrayList<>();

        CheckoutRequest.Item item1 = new CheckoutRequest.Item();
        item1.setNome("Camiseta");
        item1.setPrecoUnitario(79.90);
        item1.setQuantidade(2);
        item1.setPesoKg(0.30);
        itens.add(item1);

        CheckoutRequest.Item item2 = new CheckoutRequest.Item();
        item2.setNome("Tênis");
        item2.setPrecoUnitario(249.90);
        item2.setQuantidade(1);
        item2.setPesoKg(1.20);
        itens.add(item2);

        request.setItens(itens);
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);
        request.setNivelClube("PRATA");
        request.setRegiao("CENTRO_OESTE");

        CheckoutResponse response = service.calcular(request);

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
        List<CheckoutRequest.Item> itens = new ArrayList<>();

        CheckoutRequest.Item item1 = new CheckoutRequest.Item();
        item1.setNome("Fone");
        item1.setPrecoUnitario(199.90);
        item1.setQuantidade(2);
        item1.setPesoKg(0.25);
        itens.add(item1);

        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORDESTE");

        CheckoutResponse response = service.calcular(request);

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
        List<CheckoutRequest.Item> itens = new ArrayList<>();

        CheckoutRequest.Item item1 = new CheckoutRequest.Item();
        item1.setNome("Meia");
        item1.setPrecoUnitario(19.90);
        item1.setQuantidade(7);
        item1.setPesoKg(0.10);
        itens.add(item1);

        CheckoutRequest.Item item2 = new CheckoutRequest.Item();
        item2.setNome("Camiseta");
        item2.setPrecoUnitario(79.90);
        item2.setQuantidade(2);
        item2.setPesoKg(0.30);
        itens.add(item2);

        request.setItens(itens);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);
        request.setNivelClube("PRATA");
        request.setRegiao("SUL");

        CheckoutResponse response = service.calcular(request);

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
        List<CheckoutRequest.Item> itens = new ArrayList<>();

        CheckoutRequest.Item item1 = new CheckoutRequest.Item();
        item1.setNome("Camiseta");
        item1.setPrecoUnitario(79.90);
        item1.setQuantidade(2);
        item1.setPesoKg(0.30);
        itens.add(item1);

        CheckoutRequest.Item item2 = new CheckoutRequest.Item();
        item2.setNome("Tênis");
        item2.setPrecoUnitario(249.90);
        item2.setQuantidade(1);
        item2.setPesoKg(1.20);
        itens.add(item2);

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("OURO");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = service.calcular(request);

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
    public void testErro_PedidoInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(new ArrayList<>());
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutException.class, () -> service.calcular(request));
    }

    @Test
    public void testErro_NivelClubeInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        List<CheckoutRequest.Item> itens = new ArrayList<>();

        CheckoutRequest.Item item1 = new CheckoutRequest.Item();
        item1.setNome("Camiseta");
        item1.setPrecoUnitario(79.90);
        item1.setQuantidade(1);
        item1.setPesoKg(0.30);
        itens.add(item1);

        request.setItens(itens);
        request.setNivelClube("INVALIDO");
        request.setRegiao("SUDESTE");
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutException.class, () -> service.calcular(request));
    }

    @Test
    public void testErro_RegiaInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        List<CheckoutRequest.Item> itens = new ArrayList<>();

        CheckoutRequest.Item item1 = new CheckoutRequest.Item();
        item1.setNome("Camiseta");
        item1.setPrecoUnitario(79.90);
        item1.setQuantidade(1);
        item1.setPesoKg(0.30);
        itens.add(item1);

        request.setItens(itens);
        request.setNivelClube("BRONZE");
        request.setRegiao("INVALIDA");
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutException.class, () -> service.calcular(request));
    }

    @Test
    public void testErro_ModalidadeInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        List<CheckoutRequest.Item> itens = new ArrayList<>();

        CheckoutRequest.Item item1 = new CheckoutRequest.Item();
        item1.setNome("Camiseta");
        item1.setPrecoUnitario(79.90);
        item1.setQuantidade(1);
        item1.setPesoKg(0.30);
        itens.add(item1);

        request.setItens(itens);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");
        request.setModalidadeEntrega("INVALIDA");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutException.class, () -> service.calcular(request));
    }

    @Test
    public void testErro_CupomInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        List<CheckoutRequest.Item> itens = new ArrayList<>();

        CheckoutRequest.Item item1 = new CheckoutRequest.Item();
        item1.setNome("Camiseta");
        item1.setPrecoUnitario(79.90);
        item1.setQuantidade(1);
        item1.setPesoKg(0.30);
        itens.add(item1);

        request.setItens(itens);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("INVALIDO");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutException.class, () -> service.calcular(request));
    }

    @Test
    public void testErro_FormaPagamentoInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        List<CheckoutRequest.Item> itens = new ArrayList<>();

        CheckoutRequest.Item item1 = new CheckoutRequest.Item();
        item1.setNome("Camiseta");
        item1.setPrecoUnitario(79.90);
        item1.setQuantidade(1);
        item1.setPesoKg(0.30);
        itens.add(item1);

        request.setItens(itens);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("INVALIDA");

        assertThrows(CheckoutException.class, () -> service.calcular(request));
    }

    @Test
    public void testErro_ModalidadeIndisponivel_MotoboySuperaPeso() {
        CheckoutRequest request = new CheckoutRequest();
        List<CheckoutRequest.Item> itens = new ArrayList<>();

        CheckoutRequest.Item item1 = new CheckoutRequest.Item();
        item1.setNome("Camiseta");
        item1.setPrecoUnitario(79.90);
        item1.setQuantidade(20);
        item1.setPesoKg(0.30);
        itens.add(item1);

        request.setItens(itens);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutException.class, () -> service.calcular(request));
    }

    @Test
    public void testErro_CupomNaoAplicavel_Menos50() {
        CheckoutRequest request = new CheckoutRequest();
        List<CheckoutRequest.Item> itens = new ArrayList<>();

        CheckoutRequest.Item item1 = new CheckoutRequest.Item();
        item1.setNome("Camiseta");
        item1.setPrecoUnitario(79.90);
        item1.setQuantidade(1);
        item1.setPesoKg(0.30);
        itens.add(item1);

        request.setItens(itens);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutException.class, () -> service.calcular(request));
    }

    @Test
    public void testErro_ParcelamentoInvalido_PilxAcimaDe1() {
        CheckoutRequest request = new CheckoutRequest();
        List<CheckoutRequest.Item> itens = new ArrayList<>();

        CheckoutRequest.Item item1 = new CheckoutRequest.Item();
        item1.setNome("Camiseta");
        item1.setPrecoUnitario(79.90);
        item1.setQuantidade(1);
        item1.setPesoKg(0.30);
        itens.add(item1);

        request.setItens(itens);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(2);

        assertThrows(CheckoutException.class, () -> service.calcular(request));
    }

    @Test
    public void testErro_FormaPagamentoIndisponivel_BoletoAcimaDeLimit() {
        CheckoutRequest request = new CheckoutRequest();
        List<CheckoutRequest.Item> itens = new ArrayList<>();

        for (int i = 0; i < 15; i++) {
            CheckoutRequest.Item item = new CheckoutRequest.Item();
            item.setNome("Produto");
            item.setPrecoUnitario(100.00);
            item.setQuantidade(1);
            item.setPesoKg(0.10);
            itens.add(item);
        }

        request.setItens(itens);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("BOLETO");

        assertThrows(CheckoutException.class, () -> service.calcular(request));
    }
}
