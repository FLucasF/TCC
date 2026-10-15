package com.loja.checkout.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import com.loja.checkout.model.ItemRequest;
import com.loja.checkout.model.PedidoRequest;
import com.loja.checkout.model.ResumoResponse;

class CheckoutServiceTest {

    private CheckoutService service = new CheckoutService();

    @Test
    void exemplo1() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(Arrays.asList(
            new ItemRequest("Camiseta", 79.90, 2, 0.30),
            new ItemRequest("Tênis", 249.90, 1, 1.20)
        ));
        pedido.setModalidadeEntrega("EXPRESSA");
        pedido.setCupom("BEMVINDO10");
        pedido.setFormaPagamento("PIX");
        pedido.setNivelClube("BRONZE");
        pedido.setRegiao("NORTE");

        ResumoResponse resposta = service.calcularResumo(pedido);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), resposta.getFrete());
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), resposta.getSeguro());
        assertEquals(new BigDecimal("-20.60"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("391.47"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("391.47"), resposta.getValorParcela());
        assertEquals(new BigDecimal("0.00"), resposta.getCreditoProximaCompra());
        assertEquals(false, resposta.getBrinde());
    }

    @Test
    void exemplo2() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(Arrays.asList(
            new ItemRequest("Camiseta", 79.90, 2, 0.30),
            new ItemRequest("Tênis", 249.90, 1, 1.20)
        ));
        pedido.setModalidadeEntrega("ECONOMICA");
        pedido.setFormaPagamento("CARTAO");
        pedido.setParcelas(6);
        pedido.setNivelClube("PRATA");
        pedido.setRegiao("CENTRO_OESTE");

        ResumoResponse resposta = service.calcularResumo(pedido);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), resposta.getFrete());
        assertEquals(7, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), resposta.getSeguro());
        assertEquals(new BigDecimal("30.55"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("462.00"), resposta.getTotalFinal());
        assertEquals(6, resposta.getParcelas());
        assertEquals(new BigDecimal("77.00"), resposta.getValorParcela());
        assertEquals(new BigDecimal("8.19"), resposta.getCreditoProximaCompra());
        assertEquals(false, resposta.getBrinde());
    }

    @Test
    void exemplo3() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(Arrays.asList(
            new ItemRequest("Fone", 199.90, 2, 0.25)
        ));
        pedido.setModalidadeEntrega("MOTOBOY");
        pedido.setCupom("MENOS50");
        pedido.setFormaPagamento("BOLETO");
        pedido.setNivelClube("BRONZE");
        pedido.setRegiao("NORDESTE");

        ResumoResponse resposta = service.calcularResumo(pedido);

        assertEquals(new BigDecimal("399.80"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), resposta.getFrete());
        assertEquals(0, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), resposta.getSeguro());
        assertEquals(new BigDecimal("3.49"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("379.29"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("379.29"), resposta.getValorParcela());
        assertEquals(new BigDecimal("0.00"), resposta.getCreditoProximaCompra());
        assertEquals(false, resposta.getBrinde());
    }

    @Test
    void exemplo4() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(Arrays.asList(
            new ItemRequest("Meia", 19.90, 7, 0.10),
            new ItemRequest("Camiseta", 79.90, 2, 0.30)
        ));
        pedido.setModalidadeEntrega("RETIRADA_LOJA");
        pedido.setCupom("LEVE3PAGUE2");
        pedido.setFormaPagamento("CARTAO");
        pedido.setParcelas(3);
        pedido.setNivelClube("PRATA");
        pedido.setRegiao("SUL");

        ResumoResponse resposta = service.calcularResumo(pedido);

        assertEquals(new BigDecimal("299.10"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), resposta.getFrete());
        assertEquals(1, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), resposta.getSeguro());
        assertEquals(new BigDecimal("0.00"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("262.29"), resposta.getTotalFinal());
        assertEquals(3, resposta.getParcelas());
        assertEquals(new BigDecimal("87.43"), resposta.getValorParcela());
        assertEquals(new BigDecimal("5.98"), resposta.getCreditoProximaCompra());
        assertEquals(false, resposta.getBrinde());
    }

    @Test
    void exemplo5() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(Arrays.asList(
            new ItemRequest("Camiseta", 79.90, 2, 0.30),
            new ItemRequest("Tênis", 249.90, 1, 1.20)
        ));
        pedido.setModalidadeEntrega("EXPRESSA");
        pedido.setFormaPagamento("PIX");
        pedido.setNivelClube("OURO");
        pedido.setRegiao("SUDESTE");

        ResumoResponse resposta = service.calcularResumo(pedido);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), resposta.getFrete());
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), resposta.getSeguro());
        assertEquals(new BigDecimal("-20.69"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("393.11"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("393.11"), resposta.getValorParcela());
        assertEquals(new BigDecimal("20.48"), resposta.getCreditoProximaCompra());
        assertEquals(false, resposta.getBrinde());
    }

    @Test
    void carrinhoVazioRetornaErro() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(Arrays.asList());
        pedido.setModalidadeEntrega("ECONOMICA");
        pedido.setFormaPagamento("PIX");
        pedido.setNivelClube("BRONZE");
        pedido.setRegiao("SUDESTE");

        assertThrows(ErroCheckoutException.class, () -> {
            service.calcularResumo(pedido);
        });
    }

    @Test
    void cupomNaoAplicavelRetornaErro() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(Arrays.asList(
            new ItemRequest("Item", 100.00, 1, 0.10)
        ));
        pedido.setModalidadeEntrega("ECONOMICA");
        pedido.setCupom("MENOS50");
        pedido.setFormaPagamento("PIX");
        pedido.setNivelClube("BRONZE");
        pedido.setRegiao("SUDESTE");

        assertThrows(ErroCheckoutException.class, () -> {
            service.calcularResumo(pedido);
        });
    }

    @Test
    void motoboySobrepeso() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(Arrays.asList(
            new ItemRequest("Item Pesado", 100.00, 1, 6.0)
        ));
        pedido.setModalidadeEntrega("MOTOBOY");
        pedido.setFormaPagamento("PIX");
        pedido.setNivelClube("BRONZE");
        pedido.setRegiao("SUDESTE");

        assertThrows(ErroCheckoutException.class, () -> {
            service.calcularResumo(pedido);
        });
    }

    @Test
    void parcelasCartaoInvalidas() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(Arrays.asList(
            new ItemRequest("Item", 100.00, 1, 0.10)
        ));
        pedido.setModalidadeEntrega("ECONOMICA");
        pedido.setFormaPagamento("CARTAO");
        pedido.setParcelas(13);
        pedido.setNivelClube("BRONZE");
        pedido.setRegiao("SUDESTE");

        assertThrows(ErroCheckoutException.class, () -> {
            service.calcularResumo(pedido);
        });
    }

    @Test
    void boletoNaoPermiteParcelamento() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(Arrays.asList(
            new ItemRequest("Item", 100.00, 1, 0.10)
        ));
        pedido.setModalidadeEntrega("ECONOMICA");
        pedido.setFormaPagamento("BOLETO");
        pedido.setParcelas(2);
        pedido.setNivelClube("BRONZE");
        pedido.setRegiao("SUDESTE");

        assertThrows(ErroCheckoutException.class, () -> {
            service.calcularResumo(pedido);
        });
    }

    @Test
    void boletoAcimaDeMil() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(Arrays.asList(
            new ItemRequest("Item Caro", 1100.00, 1, 0.10)
        ));
        pedido.setModalidadeEntrega("ECONOMICA");
        pedido.setFormaPagamento("BOLETO");
        pedido.setNivelClube("BRONZE");
        pedido.setRegiao("SUDESTE");

        assertThrows(ErroCheckoutException.class, () -> {
            service.calcularResumo(pedido);
        });
    }

}
