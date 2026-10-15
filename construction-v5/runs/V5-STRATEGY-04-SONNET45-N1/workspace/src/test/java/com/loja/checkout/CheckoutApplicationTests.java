package com.loja.checkout;

import com.loja.checkout.dto.ItemDto;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CheckoutApplicationTests {
    @Autowired
    private CheckoutService checkoutService;

    @Test
    void exemplo1() {
        PedidoRequest request = new PedidoRequest();
        List<ItemDto> itens = new ArrayList<>();

        ItemDto camiseta = new ItemDto();
        camiseta.setNome("Camiseta");
        camiseta.setPrecoUnitario(new BigDecimal("79.90"));
        camiseta.setQuantidade(2);
        camiseta.setPesoKg(new BigDecimal("0.30"));
        itens.add(camiseta);

        ItemDto tenis = new ItemDto();
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

        ResumoResponse resumo = checkoutService.calcularResumo(request);

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
        assertFalse(resumo.isBrinde());
    }

    @Test
    void exemplo2() {
        PedidoRequest request = new PedidoRequest();
        List<ItemDto> itens = new ArrayList<>();

        ItemDto camiseta = new ItemDto();
        camiseta.setNome("Camiseta");
        camiseta.setPrecoUnitario(new BigDecimal("79.90"));
        camiseta.setQuantidade(2);
        camiseta.setPesoKg(new BigDecimal("0.30"));
        itens.add(camiseta);

        ItemDto tenis = new ItemDto();
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

        ResumoResponse resumo = checkoutService.calcularResumo(request);

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
        assertFalse(resumo.isBrinde());
    }

    @Test
    void exemplo3() {
        PedidoRequest request = new PedidoRequest();
        List<ItemDto> itens = new ArrayList<>();

        ItemDto fone = new ItemDto();
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

        ResumoResponse resumo = checkoutService.calcularResumo(request);

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
        assertFalse(resumo.isBrinde());
    }

    @Test
    void exemplo4() {
        PedidoRequest request = new PedidoRequest();
        List<ItemDto> itens = new ArrayList<>();

        ItemDto meia = new ItemDto();
        meia.setNome("Meia");
        meia.setPrecoUnitario(new BigDecimal("19.90"));
        meia.setQuantidade(7);
        meia.setPesoKg(new BigDecimal("0.10"));
        itens.add(meia);

        ItemDto camiseta = new ItemDto();
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

        ResumoResponse resumo = checkoutService.calcularResumo(request);

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
        assertFalse(resumo.isBrinde());
    }

    @Test
    void exemplo5() {
        PedidoRequest request = new PedidoRequest();
        List<ItemDto> itens = new ArrayList<>();

        ItemDto camiseta = new ItemDto();
        camiseta.setNome("Camiseta");
        camiseta.setPrecoUnitario(new BigDecimal("79.90"));
        camiseta.setQuantidade(2);
        camiseta.setPesoKg(new BigDecimal("0.30"));
        itens.add(camiseta);

        ItemDto tenis = new ItemDto();
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

        ResumoResponse resumo = checkoutService.calcularResumo(request);

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
        assertFalse(resumo.isBrinde());
    }
}
