package com.loja.checkout;

import com.loja.checkout.servico.CalculadoraResumo;
import com.loja.checkout.servico.ServicoResumo;
import com.loja.checkout.servico.ValidadorPedido;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServicoResumoTest {

    private final ServicoResumo servico = new ServicoResumo(new ValidadorPedido(), new CalculadoraResumo());

    private static ResumoRequest.ItemRequest item(String nome, String preco, int qtd, String peso) {
        ResumoRequest.ItemRequest i = new ResumoRequest.ItemRequest();
        i.nome = nome;
        i.precoUnitario = new BigDecimal(preco);
        i.quantidade = qtd;
        i.pesoKg = new BigDecimal(peso);
        return i;
    }

    private static ResumoRequest req(List<ResumoRequest.ItemRequest> itens, String modalidade, String cupom,
                                     String forma, Integer parcelas, String nivel, String regiao) {
        ResumoRequest r = new ResumoRequest();
        r.itens = itens;
        r.modalidadeEntrega = modalidade;
        r.cupom = cupom;
        r.formaPagamento = forma;
        r.parcelas = parcelas;
        r.nivelClube = nivel;
        r.regiao = regiao;
        return r;
    }

    private static BigDecimal valor(String s) {
        return new BigDecimal(s);
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        ResumoResponse r = servico.resumir(req(
                new ArrayList<>(List.of(
                        item("Camiseta", "79.90", 2, "0.30"),
                        item("Tenis", "249.90", 1, "1.20"))),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

        assertEquals(valor("409.70"), r.subtotalProdutos());
        assertEquals(valor("40.97"), r.descontoCupom());
        assertEquals(valor("33.10"), r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(valor("10.24"), r.seguro());
        assertEquals(valor("-20.60"), r.ajustePagamento());
        assertEquals(valor("391.47"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(valor("391.47"), r.valorParcela());
        assertEquals(valor("0.00"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao6_prata_co() {
        ResumoResponse r = servico.resumir(req(
                new ArrayList<>(List.of(
                        item("Camiseta", "79.90", 2, "0.30"),
                        item("Tenis", "249.90", 1, "1.20"))),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertEquals(valor("409.70"), r.subtotalProdutos());
        assertEquals(valor("0.00"), r.descontoCupom());
        assertEquals(valor("15.60"), r.frete());
        assertEquals(7, r.prazoEntregaDias());
        assertEquals(valor("6.15"), r.seguro());
        assertEquals(valor("30.55"), r.ajustePagamento());
        assertEquals(valor("462.00"), r.totalFinal());
        assertEquals(6, r.parcelas());
        assertEquals(valor("77.00"), r.valorParcela());
        assertEquals(valor("8.19"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        ResumoResponse r = servico.resumir(req(
                new ArrayList<>(List.of(item("Fone", "199.90", 2, "0.25"))),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));

        assertEquals(valor("399.80"), r.subtotalProdutos());
        assertEquals(valor("50.00"), r.descontoCupom());
        assertEquals(valor("18.00"), r.frete());
        assertEquals(0, r.prazoEntregaDias());
        assertEquals(valor("8.00"), r.seguro());
        assertEquals(valor("3.49"), r.ajustePagamento());
        assertEquals(valor("379.29"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(valor("379.29"), r.valorParcela());
        assertEquals(valor("0.00"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao3_prata_sul() {
        ResumoResponse r = servico.resumir(req(
                new ArrayList<>(List.of(
                        item("Meia", "19.90", 7, "0.10"),
                        item("Camiseta", "79.90", 2, "0.30"))),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertEquals(valor("299.10"), r.subtotalProdutos());
        assertEquals(valor("39.80"), r.descontoCupom());
        assertEquals(valor("0.00"), r.frete());
        assertEquals(1, r.prazoEntregaDias());
        assertEquals(valor("2.99"), r.seguro());
        assertEquals(valor("0.00"), r.ajustePagamento());
        assertEquals(valor("262.29"), r.totalFinal());
        assertEquals(3, r.parcelas());
        assertEquals(valor("87.43"), r.valorParcela());
        assertEquals(valor("5.98"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        ResumoResponse r = servico.resumir(req(
                new ArrayList<>(List.of(
                        item("Camiseta", "79.90", 2, "0.30"),
                        item("Tenis", "249.90", 1, "1.20"))),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertEquals(valor("409.70"), r.subtotalProdutos());
        assertEquals(valor("0.00"), r.descontoCupom());
        assertEquals(valor("0.00"), r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(valor("4.10"), r.seguro());
        assertEquals(valor("-20.69"), r.ajustePagamento());
        assertEquals(valor("393.11"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(valor("393.11"), r.valorParcela());
        assertEquals(valor("20.48"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }
}
