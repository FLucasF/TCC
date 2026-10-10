package com.loja.checkout;

import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    @Test
    void exemplo1() {
        PedidoRequest pedido = new PedidoRequest();

        ItemPedido item1 = new ItemPedido();
        item1.setNome("Camiseta");
        item1.setPrecoUnitario(new BigDecimal("79.90"));
        item1.setQuantidade(2);
        item1.setPesoKg(new BigDecimal("0.30"));

        ItemPedido item2 = new ItemPedido();
        item2.setNome("Tênis");
        item2.setPrecoUnitario(new BigDecimal("249.90"));
        item2.setQuantidade(1);
        item2.setPesoKg(new BigDecimal("1.20"));

        pedido.setItens(Arrays.asList(item1, item2));
        pedido.setModalidadeEntrega("EXPRESSA");
        pedido.setCupom("BEMVINDO10");
        pedido.setFormaPagamento("PIX");
        pedido.setNivelClube("BRONZE");
        pedido.setRegiao("NORTE");

        ResumoResponse resumo = service.calcularResumo(pedido);

        assertEquals(new BigDecimal("409.70"), resumo.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resumo.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), resumo.getFrete());
        assertEquals(2, resumo.getPrazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), resumo.getSeguro());
        assertEquals(new BigDecimal("-20.60"), resumo.getAjustePagamento());
        assertEquals(new BigDecimal("391.47"), resumo.getTotalFinal());
        assertEquals(1, resumo.getParcelas());
        assertEquals(new BigDecimal("391.47"), resumo.getValorParcela());
        assertEquals(new BigDecimal("0.00"), resumo.getCreditoProximaCompra());
        assertFalse(resumo.getBrinde());
    }

    @Test
    void exemplo2() {
        PedidoRequest pedido = new PedidoRequest();

        ItemPedido item1 = new ItemPedido();
        item1.setNome("Camiseta");
        item1.setPrecoUnitario(new BigDecimal("79.90"));
        item1.setQuantidade(2);
        item1.setPesoKg(new BigDecimal("0.30"));

        ItemPedido item2 = new ItemPedido();
        item2.setNome("Tênis");
        item2.setPrecoUnitario(new BigDecimal("249.90"));
        item2.setQuantidade(1);
        item2.setPesoKg(new BigDecimal("1.20"));

        pedido.setItens(Arrays.asList(item1, item2));
        pedido.setModalidadeEntrega("ECONOMICA");
        pedido.setFormaPagamento("CARTAO");
        pedido.setParcelas(6);
        pedido.setNivelClube("PRATA");
        pedido.setRegiao("CENTRO_OESTE");

        ResumoResponse resumo = service.calcularResumo(pedido);

        assertEquals(new BigDecimal("409.70"), resumo.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resumo.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), resumo.getFrete());
        assertEquals(7, resumo.getPrazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), resumo.getSeguro());
        assertEquals(new BigDecimal("30.55"), resumo.getAjustePagamento());
        assertEquals(new BigDecimal("462.00"), resumo.getTotalFinal());
        assertEquals(6, resumo.getParcelas());
        assertEquals(new BigDecimal("77.00"), resumo.getValorParcela());
        assertEquals(new BigDecimal("8.19"), resumo.getCreditoProximaCompra());
        assertFalse(resumo.getBrinde());
    }

    @Test
    void exemplo3() {
        PedidoRequest pedido = new PedidoRequest();

        ItemPedido item1 = new ItemPedido();
        item1.setNome("Fone");
        item1.setPrecoUnitario(new BigDecimal("199.90"));
        item1.setQuantidade(2);
        item1.setPesoKg(new BigDecimal("0.25"));

        pedido.setItens(Arrays.asList(item1));
        pedido.setModalidadeEntrega("MOTOBOY");
        pedido.setCupom("MENOS50");
        pedido.setFormaPagamento("BOLETO");
        pedido.setNivelClube("BRONZE");
        pedido.setRegiao("NORDESTE");

        ResumoResponse resumo = service.calcularResumo(pedido);

        assertEquals(new BigDecimal("399.80"), resumo.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resumo.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), resumo.getFrete());
        assertEquals(0, resumo.getPrazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), resumo.getSeguro());
        assertEquals(new BigDecimal("3.49"), resumo.getAjustePagamento());
        assertEquals(new BigDecimal("379.29"), resumo.getTotalFinal());
        assertEquals(1, resumo.getParcelas());
        assertEquals(new BigDecimal("379.29"), resumo.getValorParcela());
        assertEquals(new BigDecimal("0.00"), resumo.getCreditoProximaCompra());
        assertFalse(resumo.getBrinde());
    }

    @Test
    void exemplo4() {
        PedidoRequest pedido = new PedidoRequest();

        ItemPedido item1 = new ItemPedido();
        item1.setNome("Meia");
        item1.setPrecoUnitario(new BigDecimal("19.90"));
        item1.setQuantidade(7);
        item1.setPesoKg(new BigDecimal("0.10"));

        ItemPedido item2 = new ItemPedido();
        item2.setNome("Camiseta");
        item2.setPrecoUnitario(new BigDecimal("79.90"));
        item2.setQuantidade(2);
        item2.setPesoKg(new BigDecimal("0.30"));

        pedido.setItens(Arrays.asList(item1, item2));
        pedido.setModalidadeEntrega("RETIRADA_LOJA");
        pedido.setCupom("LEVE3PAGUE2");
        pedido.setFormaPagamento("CARTAO");
        pedido.setParcelas(3);
        pedido.setNivelClube("PRATA");
        pedido.setRegiao("SUL");

        ResumoResponse resumo = service.calcularResumo(pedido);

        assertEquals(new BigDecimal("299.10"), resumo.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resumo.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), resumo.getFrete());
        assertEquals(1, resumo.getPrazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), resumo.getSeguro());
        assertEquals(new BigDecimal("0.00"), resumo.getAjustePagamento());
        assertEquals(new BigDecimal("262.29"), resumo.getTotalFinal());
        assertEquals(3, resumo.getParcelas());
        assertEquals(new BigDecimal("87.43"), resumo.getValorParcela());
        assertEquals(new BigDecimal("5.98"), resumo.getCreditoProximaCompra());
        assertFalse(resumo.getBrinde());
    }

    @Test
    void exemplo5() {
        PedidoRequest pedido = new PedidoRequest();

        ItemPedido item1 = new ItemPedido();
        item1.setNome("Camiseta");
        item1.setPrecoUnitario(new BigDecimal("79.90"));
        item1.setQuantidade(2);
        item1.setPesoKg(new BigDecimal("0.30"));

        ItemPedido item2 = new ItemPedido();
        item2.setNome("Tênis");
        item2.setPrecoUnitario(new BigDecimal("249.90"));
        item2.setQuantidade(1);
        item2.setPesoKg(new BigDecimal("1.20"));

        pedido.setItens(Arrays.asList(item1, item2));
        pedido.setModalidadeEntrega("EXPRESSA");
        pedido.setFormaPagamento("PIX");
        pedido.setNivelClube("OURO");
        pedido.setRegiao("SUDESTE");

        ResumoResponse resumo = service.calcularResumo(pedido);

        assertEquals(new BigDecimal("409.70"), resumo.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resumo.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), resumo.getFrete());
        assertEquals(2, resumo.getPrazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), resumo.getSeguro());
        assertEquals(new BigDecimal("-20.69"), resumo.getAjustePagamento());
        assertEquals(new BigDecimal("393.11"), resumo.getTotalFinal());
        assertEquals(1, resumo.getParcelas());
        assertEquals(new BigDecimal("393.11"), resumo.getValorParcela());
        assertEquals(new BigDecimal("20.48"), resumo.getCreditoProximaCompra());
        assertFalse(resumo.getBrinde());
    }

    @Test
    void erroCarrinhoVazio() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(Arrays.asList());

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(pedido);
        });
        assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void erroMotoboyAcima5kg() {
        PedidoRequest pedido = new PedidoRequest();

        ItemPedido item = new ItemPedido();
        item.setNome("Item Pesado");
        item.setPrecoUnitario(new BigDecimal("100.00"));
        item.setQuantidade(2);
        item.setPesoKg(new BigDecimal("3.00"));

        pedido.setItens(Arrays.asList(item));
        pedido.setModalidadeEntrega("MOTOBOY");
        pedido.setFormaPagamento("PIX");
        pedido.setNivelClube("BRONZE");
        pedido.setRegiao("SUDESTE");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(pedido);
        });
        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigoErro());
    }

    @Test
    void erroBoletoAcima1000() {
        PedidoRequest pedido = new PedidoRequest();

        ItemPedido item = new ItemPedido();
        item.setNome("Item Caro");
        item.setPrecoUnitario(new BigDecimal("1100.00"));
        item.setQuantidade(1);
        item.setPesoKg(new BigDecimal("0.50"));

        pedido.setItens(Arrays.asList(item));
        pedido.setModalidadeEntrega("RETIRADA_LOJA");
        pedido.setFormaPagamento("BOLETO");
        pedido.setNivelClube("BRONZE");
        pedido.setRegiao("SUDESTE");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            service.calcularResumo(pedido);
        });
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigoErro());
    }
}
