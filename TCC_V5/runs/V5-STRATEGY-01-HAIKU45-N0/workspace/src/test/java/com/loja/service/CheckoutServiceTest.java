package com.loja.service;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ItemCarrinho;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CheckoutServiceTest {

    private CheckoutService checkoutService;

    @BeforeEach
    public void setUp() {
        checkoutService = new CheckoutService();
    }

    private CheckoutRequest criarRequest(List<ItemCarrinho> itens, String entrega, String cupom,
                                        String pagamento, Integer parcelas, String nivel, String regiao) {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(itens);
        request.setModalidadeEntrega(entrega);
        request.setCupom(cupom);
        request.setFormaPagamento(pagamento);
        request.setParcelas(parcelas);
        request.setNivelClube(nivel);
        request.setRegiao(regiao);
        return request;
    }

    @Test
    public void exemplo1() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        itens.add(new ItemCarrinho("Tênis", 249.90, 1, 1.20));

        CheckoutRequest request = criarRequest(itens, "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");
        CheckoutResponse response = checkoutService.calcular(request);

        assertEquals(409.70, response.getSubtotalProdutos(), 0.01);
        assertEquals(40.97, response.getDescontoCupom(), 0.01);
        assertEquals(33.10, response.getFrete(), 0.01);
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(10.24, response.getSeguro(), 0.01);
        assertEquals(-20.60, response.getAjustePagamento(), 0.01);
        assertEquals(391.47, response.getTotalFinal(), 0.01);
        assertEquals(1, response.getParcelas());
        assertEquals(391.47, response.getValorParcela(), 0.01);
        assertEquals(0.00, response.getCreditoProximaCompra(), 0.01);
        assertFalse(response.getBrinde());
    }

    @Test
    public void exemplo2() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        itens.add(new ItemCarrinho("Tênis", 249.90, 1, 1.20));

        CheckoutRequest request = criarRequest(itens, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");
        CheckoutResponse response = checkoutService.calcular(request);

        assertEquals(409.70, response.getSubtotalProdutos(), 0.01);
        assertEquals(0.00, response.getDescontoCupom(), 0.01);
        assertEquals(15.60, response.getFrete(), 0.01);
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(6.15, response.getSeguro(), 0.01);
        assertEquals(30.55, response.getAjustePagamento(), 0.01);
        assertEquals(462.00, response.getTotalFinal(), 0.01);
        assertEquals(6, response.getParcelas());
        assertEquals(77.00, response.getValorParcela(), 0.01);
        assertEquals(8.19, response.getCreditoProximaCompra(), 0.01);
        assertFalse(response.getBrinde());
    }

    @Test
    public void exemplo3() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Fone", 199.90, 2, 0.25));

        CheckoutRequest request = criarRequest(itens, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");
        CheckoutResponse response = checkoutService.calcular(request);

        assertEquals(399.80, response.getSubtotalProdutos(), 0.01);
        assertEquals(50.00, response.getDescontoCupom(), 0.01);
        assertEquals(18.00, response.getFrete(), 0.01);
        assertEquals(0, response.getPrazoEntregaDias());
        assertEquals(8.00, response.getSeguro(), 0.01);
        assertEquals(3.49, response.getAjustePagamento(), 0.01);
        assertEquals(379.29, response.getTotalFinal(), 0.01);
        assertEquals(1, response.getParcelas());
        assertEquals(379.29, response.getValorParcela(), 0.01);
        assertEquals(0.00, response.getCreditoProximaCompra(), 0.01);
        assertFalse(response.getBrinde());
    }

    @Test
    public void exemplo4() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Meia", 19.90, 7, 0.10));
        itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));

        CheckoutRequest request = criarRequest(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");
        CheckoutResponse response = checkoutService.calcular(request);

        assertEquals(299.10, response.getSubtotalProdutos(), 0.01);
        assertEquals(39.80, response.getDescontoCupom(), 0.01);
        assertEquals(0.00, response.getFrete(), 0.01);
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(2.99, response.getSeguro(), 0.01);
        assertEquals(0.00, response.getAjustePagamento(), 0.01);
        assertEquals(262.29, response.getTotalFinal(), 0.01);
        assertEquals(3, response.getParcelas());
        assertEquals(87.43, response.getValorParcela(), 0.01);
        assertEquals(5.98, response.getCreditoProximaCompra(), 0.01);
        assertFalse(response.getBrinde());
    }

    @Test
    public void exemplo5() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Camiseta", 79.90, 2, 0.30));
        itens.add(new ItemCarrinho("Tênis", 249.90, 1, 1.20));

        CheckoutRequest request = criarRequest(itens, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");
        CheckoutResponse response = checkoutService.calcular(request);

        assertEquals(409.70, response.getSubtotalProdutos(), 0.01);
        assertEquals(0.00, response.getDescontoCupom(), 0.01);
        assertEquals(0.00, response.getFrete(), 0.01);
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(4.10, response.getSeguro(), 0.01);
        assertEquals(-20.69, response.getAjustePagamento(), 0.01);
        assertEquals(393.11, response.getTotalFinal(), 0.01);
        assertEquals(1, response.getParcelas());
        assertEquals(393.11, response.getValorParcela(), 0.01);
        assertEquals(20.48, response.getCreditoProximaCompra(), 0.01);
        assertFalse(response.getBrinde());
    }

    @Test
    public void testCarrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(new ArrayList<>());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        assertThrows(CheckoutService.CheckoutException.class, () -> checkoutService.calcular(request));
    }

    @Test
    public void testMotoboyCom5Kg() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Produto", 100.0, 1, 5.0));

        CheckoutRequest request = criarRequest(itens, "MOTOBOY", null, "PIX", 1, "BRONZE", "NORTE");
        CheckoutResponse response = checkoutService.calcular(request);

        assertEquals(100.0, response.getSubtotalProdutos(), 0.01);
        assertEquals(18.00, response.getFrete(), 0.01);
    }

    @Test
    public void testMotoboyCom5p1Kg() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Produto", 100.0, 1, 5.1));

        CheckoutRequest request = criarRequest(itens, "MOTOBOY", null, "PIX", 1, "BRONZE", "NORTE");

        assertThrows(CheckoutService.CheckoutException.class, () -> checkoutService.calcular(request));
    }

    @Test
    public void testBoleto1100() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Produto", 1100.0, 1, 1.0));

        CheckoutRequest request = criarRequest(itens, "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "NORTE");

        assertThrows(CheckoutService.CheckoutException.class, () -> checkoutService.calcular(request));
    }

    @Test
    public void testFretegratisDesconto() {
        List<ItemCarrinho> itens = new ArrayList<>();
        itens.add(new ItemCarrinho("Produto", 100.0, 1, 1.0));

        CheckoutRequest request = criarRequest(itens, "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "NORTE");
        CheckoutResponse response = checkoutService.calcular(request);

        assertEquals(100.0, response.getSubtotalProdutos(), 0.01);
        assertEquals(0.00, response.getDescontoCupom(), 0.01);
        assertEquals(0.00, response.getFrete(), 0.01);
    }
}
