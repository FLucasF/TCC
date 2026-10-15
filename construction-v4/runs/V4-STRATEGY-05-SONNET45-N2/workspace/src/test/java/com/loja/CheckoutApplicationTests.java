package com.loja;

import com.loja.model.ItemCarrinho;
import com.loja.model.PedidoRequest;
import com.loja.model.ResumoResponse;
import com.loja.service.ResumoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class CheckoutApplicationTests {

    @Autowired
    private ResumoService resumoService;

    @Test
    void exemplo1() {
        PedidoRequest pedido = new PedidoRequest();
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

        pedido.setItens(itens);
        pedido.setModalidadeEntrega("EXPRESSA");
        pedido.setCupom("BEMVINDO10");
        pedido.setFormaPagamento("PIX");
        pedido.setNivelClube("BRONZE");
        pedido.setRegiao("NORTE");

        ResumoResponse resumo = resumoService.calcularResumo(pedido);

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
        assertEquals(false, resumo.getBrinde());
    }

    @Test
    void exemplo2() {
        PedidoRequest pedido = new PedidoRequest();
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

        pedido.setItens(itens);
        pedido.setModalidadeEntrega("ECONOMICA");
        pedido.setFormaPagamento("CARTAO");
        pedido.setParcelas(6);
        pedido.setNivelClube("PRATA");
        pedido.setRegiao("CENTRO_OESTE");

        ResumoResponse resumo = resumoService.calcularResumo(pedido);

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
        assertEquals(false, resumo.getBrinde());
    }

    @Test
    void exemplo3() {
        PedidoRequest pedido = new PedidoRequest();
        List<ItemCarrinho> itens = new ArrayList<>();

        ItemCarrinho fone = new ItemCarrinho();
        fone.setNome("Fone");
        fone.setPrecoUnitario(new BigDecimal("199.90"));
        fone.setQuantidade(2);
        fone.setPesoKg(new BigDecimal("0.25"));
        itens.add(fone);

        pedido.setItens(itens);
        pedido.setModalidadeEntrega("MOTOBOY");
        pedido.setCupom("MENOS50");
        pedido.setFormaPagamento("BOLETO");
        pedido.setNivelClube("BRONZE");
        pedido.setRegiao("NORDESTE");

        ResumoResponse resumo = resumoService.calcularResumo(pedido);

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
        assertEquals(false, resumo.getBrinde());
    }

    @Test
    void exemplo4() {
        PedidoRequest pedido = new PedidoRequest();
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

        pedido.setItens(itens);
        pedido.setModalidadeEntrega("RETIRADA_LOJA");
        pedido.setCupom("LEVE3PAGUE2");
        pedido.setFormaPagamento("CARTAO");
        pedido.setParcelas(3);
        pedido.setNivelClube("PRATA");
        pedido.setRegiao("SUL");

        ResumoResponse resumo = resumoService.calcularResumo(pedido);

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
        assertEquals(false, resumo.getBrinde());
    }

    @Test
    void exemplo5() {
        PedidoRequest pedido = new PedidoRequest();
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

        pedido.setItens(itens);
        pedido.setModalidadeEntrega("EXPRESSA");
        pedido.setFormaPagamento("PIX");
        pedido.setNivelClube("OURO");
        pedido.setRegiao("SUDESTE");

        ResumoResponse resumo = resumoService.calcularResumo(pedido);

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
        assertEquals(false, resumo.getBrinde());
    }
}
