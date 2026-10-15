package com.loja.checkout.service;

import com.loja.checkout.api.ItemRequisicao;
import com.loja.checkout.api.RequisicaoCheckout;
import com.loja.checkout.api.RespostaCheckout;
import com.loja.checkout.exceptions.ErroCheckout;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ServicoCheckoutTest {

    private ServicoCheckout servico = new ServicoCheckout();

    @Test
    public void testExemplo1() throws ErroCheckout {
        // Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg),
        // EXPRESSA, cupom BEMVINDO10, PIX
        // → subtotal 409,70 · cupom 40,97 · frete 33,10 · prazo 2 · ajuste −20,09 · total final 381,74 · 1× de 381,74

        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.itens = new ArrayList<>();

        ItemRequisicao camiseta = new ItemRequisicao();
        camiseta.nome = "Camiseta";
        camiseta.precoUnitario = new BigDecimal("79.90");
        camiseta.quantidade = 2;
        camiseta.pesoKg = new BigDecimal("0.30");
        requisicao.itens.add(camiseta);

        ItemRequisicao tenis = new ItemRequisicao();
        tenis.nome = "Tênis";
        tenis.precoUnitario = new BigDecimal("249.90");
        tenis.quantidade = 1;
        tenis.pesoKg = new BigDecimal("1.20");
        requisicao.itens.add(tenis);

        requisicao.modalidadeEntrega = "EXPRESSA";
        requisicao.cupom = "BEMVINDO10";
        requisicao.formaPagamento = "PIX";
        requisicao.parcelas = 1;

        RespostaCheckout resposta = servico.calcularResumo(requisicao);

        assertEquals(0, new BigDecimal("409.70").compareTo(resposta.subtotalProdutos));
        assertEquals(0, new BigDecimal("40.97").compareTo(resposta.descontoCupom));
        assertEquals(0, new BigDecimal("33.10").compareTo(resposta.frete));
        assertEquals(2, resposta.prazoEntregaDias);
        assertEquals(0, new BigDecimal("-20.09").compareTo(resposta.ajustePagamento));
        assertEquals(0, new BigDecimal("381.74").compareTo(resposta.totalFinal));
        assertEquals(1, resposta.parcelas);
        assertEquals(0, new BigDecimal("381.74").compareTo(resposta.valorParcela));
    }

    @Test
    public void testExemplo2() throws ErroCheckout {
        // mesmos itens, ECONOMICA, sem cupom, CARTAO em 6×
        // → subtotal 409,70 · cupom 0,00 · frete 15,60 · prazo 7 · ajuste 30,10 · total final 455,40 · 6× de 75,90

        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.itens = new ArrayList<>();

        ItemRequisicao camiseta = new ItemRequisicao();
        camiseta.nome = "Camiseta";
        camiseta.precoUnitario = new BigDecimal("79.90");
        camiseta.quantidade = 2;
        camiseta.pesoKg = new BigDecimal("0.30");
        requisicao.itens.add(camiseta);

        ItemRequisicao tenis = new ItemRequisicao();
        tenis.nome = "Tênis";
        tenis.precoUnitario = new BigDecimal("249.90");
        tenis.quantidade = 1;
        tenis.pesoKg = new BigDecimal("1.20");
        requisicao.itens.add(tenis);

        requisicao.modalidadeEntrega = "ECONOMICA";
        requisicao.cupom = null;
        requisicao.formaPagamento = "CARTAO";
        requisicao.parcelas = 6;

        RespostaCheckout resposta = servico.calcularResumo(requisicao);

        assertEquals(0, new BigDecimal("409.70").compareTo(resposta.subtotalProdutos));
        assertEquals(0, new BigDecimal("0.00").compareTo(resposta.descontoCupom));
        assertEquals(0, new BigDecimal("15.60").compareTo(resposta.frete));
        assertEquals(7, resposta.prazoEntregaDias);
        assertEquals(0, new BigDecimal("30.10").compareTo(resposta.ajustePagamento));
        assertEquals(0, new BigDecimal("455.40").compareTo(resposta.totalFinal));
        assertEquals(6, resposta.parcelas);
        assertEquals(0, new BigDecimal("75.90").compareTo(resposta.valorParcela));
    }

    @Test
    public void testExemplo3() throws ErroCheckout {
        // Fone 199,90 × 2 (0,25 kg), MOTOBOY, cupom MENOS50, BOLETO
        // → subtotal 399,80 · cupom 50,00 · frete 18,00 · prazo 0 · ajuste 3,49 · total final 371,29 · 1× de 371,29

        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.itens = new ArrayList<>();

        ItemRequisicao fone = new ItemRequisicao();
        fone.nome = "Fone";
        fone.precoUnitario = new BigDecimal("199.90");
        fone.quantidade = 2;
        fone.pesoKg = new BigDecimal("0.25");
        requisicao.itens.add(fone);

        requisicao.modalidadeEntrega = "MOTOBOY";
        requisicao.cupom = "MENOS50";
        requisicao.formaPagamento = "BOLETO";
        requisicao.parcelas = 1;

        RespostaCheckout resposta = servico.calcularResumo(requisicao);

        assertEquals(0, new BigDecimal("399.80").compareTo(resposta.subtotalProdutos));
        assertEquals(0, new BigDecimal("50.00").compareTo(resposta.descontoCupom));
        assertEquals(0, new BigDecimal("18.00").compareTo(resposta.frete));
        assertEquals(0, resposta.prazoEntregaDias);
        assertEquals(0, new BigDecimal("3.49").compareTo(resposta.ajustePagamento));
        assertEquals(0, new BigDecimal("371.29").compareTo(resposta.totalFinal));
        assertEquals(1, resposta.parcelas);
        assertEquals(0, new BigDecimal("371.29").compareTo(resposta.valorParcela));
    }

    @Test
    public void testExemplo4() throws ErroCheckout {
        // Meia 19,90 × 7 (0,10 kg) + Camiseta 79,90 × 2 (0,30 kg), RETIRADA_LOJA, cupom LEVE3PAGUE2, CARTAO em 3×
        // → subtotal 299,10 · cupom 39,80 · frete 0,00 · prazo 1 · ajuste 0,00 · total final 259,30 · 3× de 86,43

        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.itens = new ArrayList<>();

        ItemRequisicao meia = new ItemRequisicao();
        meia.nome = "Meia";
        meia.precoUnitario = new BigDecimal("19.90");
        meia.quantidade = 7;
        meia.pesoKg = new BigDecimal("0.10");
        requisicao.itens.add(meia);

        ItemRequisicao camiseta = new ItemRequisicao();
        camiseta.nome = "Camiseta";
        camiseta.precoUnitario = new BigDecimal("79.90");
        camiseta.quantidade = 2;
        camiseta.pesoKg = new BigDecimal("0.30");
        requisicao.itens.add(camiseta);

        requisicao.modalidadeEntrega = "RETIRADA_LOJA";
        requisicao.cupom = "LEVE3PAGUE2";
        requisicao.formaPagamento = "CARTAO";
        requisicao.parcelas = 3;

        RespostaCheckout resposta = servico.calcularResumo(requisicao);

        assertEquals(0, new BigDecimal("299.10").compareTo(resposta.subtotalProdutos));
        assertEquals(0, new BigDecimal("39.80").compareTo(resposta.descontoCupom));
        assertEquals(0, new BigDecimal("0.00").compareTo(resposta.frete));
        assertEquals(1, resposta.prazoEntregaDias);
        assertEquals(0, new BigDecimal("0.00").compareTo(resposta.ajustePagamento));
        assertEquals(0, new BigDecimal("259.30").compareTo(resposta.totalFinal));
        assertEquals(3, resposta.parcelas);
        assertEquals(0, new BigDecimal("86.43").compareTo(resposta.valorParcela));
    }

    @Test
    public void testErroCarrinhoVazio() {
        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.itens = new ArrayList<>();
        requisicao.modalidadeEntrega = "EXPRESSA";
        requisicao.formaPagamento = "PIX";

        assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
    }

    @Test
    public void testErroModalidadeInvalida() {
        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.itens = new ArrayList<>();

        ItemRequisicao item = new ItemRequisicao();
        item.nome = "Teste";
        item.precoUnitario = new BigDecimal("10.00");
        item.quantidade = 1;
        item.pesoKg = new BigDecimal("0.10");
        requisicao.itens.add(item);

        requisicao.modalidadeEntrega = "INVALIDA";
        requisicao.formaPagamento = "PIX";

        assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
    }

    @Test
    public void testErroMotoboy5kg() {
        RequisicaoCheckout requisicao = new RequisicaoCheckout();
        requisicao.itens = new ArrayList<>();

        ItemRequisicao item = new ItemRequisicao();
        item.nome = "Teste";
        item.precoUnitario = new BigDecimal("10.00");
        item.quantidade = 1;
        item.pesoKg = new BigDecimal("6.00");
        requisicao.itens.add(item);

        requisicao.modalidadeEntrega = "MOTOBOY";
        requisicao.formaPagamento = "PIX";

        assertThrows(ErroCheckout.class, () -> servico.calcularResumo(requisicao));
    }
}
