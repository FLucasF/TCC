package com.loja.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loja.checkout.domain.CalculadoraResumo;
import com.loja.checkout.domain.CalculadoraResumo.Entrada;
import com.loja.checkout.domain.ErroCalculo;
import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.Resumo;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraResumoTest {

    private final CalculadoraResumo calc = new CalculadoraResumo();

    private static Item item(String nome, String preco, int qtd, String peso) {
        return new Item(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    @Test
    void exemplo1_expressa_bemvindo_pix_bronze_norte() {
        Resumo r = calc.calcular(new Entrada(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));
        assertEquals(d("409.70"), r.subtotalProdutos());
        assertEquals(d("40.97"), r.descontoCupom());
        assertEquals(d("33.10"), r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(d("10.24"), r.seguro());
        assertEquals(d("-20.60"), r.ajustePagamento());
        assertEquals(d("391.47"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(d("391.47"), r.valorParcela());
        assertEquals(d("0.00"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao6x_prata_centro_oeste() {
        Resumo r = calc.calcular(new Entrada(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));
        assertEquals(d("409.70"), r.subtotalProdutos());
        assertEquals(d("0.00"), r.descontoCupom());
        assertEquals(d("15.60"), r.frete());
        assertEquals(7, r.prazoEntregaDias());
        assertEquals(d("6.15"), r.seguro());
        assertEquals(d("30.55"), r.ajustePagamento());
        assertEquals(d("462.00"), r.totalFinal());
        assertEquals(6, r.parcelas());
        assertEquals(d("77.00"), r.valorParcela());
        assertEquals(d("8.19"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        Resumo r = calc.calcular(new Entrada(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));
        assertEquals(d("399.80"), r.subtotalProdutos());
        assertEquals(d("50.00"), r.descontoCupom());
        assertEquals(d("18.00"), r.frete());
        assertEquals(0, r.prazoEntregaDias());
        assertEquals(d("8.00"), r.seguro());
        assertEquals(d("3.49"), r.ajustePagamento());
        assertEquals(d("379.29"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(d("379.29"), r.valorParcela());
        assertEquals(d("0.00"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao3x_prata_sul() {
        Resumo r = calc.calcular(new Entrada(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));
        assertEquals(d("299.10"), r.subtotalProdutos());
        assertEquals(d("39.80"), r.descontoCupom());
        assertEquals(d("0.00"), r.frete());
        assertEquals(1, r.prazoEntregaDias());
        assertEquals(d("2.99"), r.seguro());
        assertEquals(d("0.00"), r.ajustePagamento());
        assertEquals(d("262.29"), r.totalFinal());
        assertEquals(3, r.parcelas());
        assertEquals(d("87.43"), r.valorParcela());
        assertEquals(d("5.98"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        Resumo r = calc.calcular(new Entrada(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"));
        assertEquals(d("409.70"), r.subtotalProdutos());
        assertEquals(d("0.00"), r.descontoCupom());
        assertEquals(d("0.00"), r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(d("4.10"), r.seguro());
        assertEquals(d("-20.69"), r.ajustePagamento());
        assertEquals(d("393.11"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(d("393.11"), r.valorParcela());
        assertEquals(d("20.48"), r.creditoProximaCompra());
        assertFalse(r.brinde());
    }

    @Test
    void erro_carrinho_vazio() {
        ErroCalculo e = assertThrows(ErroCalculo.class, () -> calc.calcular(new Entrada(
                List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")));
        assertEquals("PEDIDO_INVALIDO", e.codigo());
    }

    @Test
    void erro_motoboy_acima_do_limite() {
        ErroCalculo e = assertThrows(ErroCalculo.class, () -> calc.calcular(new Entrada(
                List.of(item("Fardo", "100.00", 1, "6.0")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE")));
        assertEquals("MODALIDADE_INDISPONIVEL", e.codigo());
    }

    @Test
    void erro_menos50_abaixo_do_minimo() {
        ErroCalculo e = assertThrows(ErroCalculo.class, () -> calc.calcular(new Entrada(
                List.of(item("Meia", "19.90", 1, "0.10")),
                "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE")));
        assertEquals("CUPOM_NAO_APLICAVEL", e.codigo());
    }

    @Test
    void erro_boleto_acima_de_mil() {
        ErroCalculo e = assertThrows(ErroCalculo.class, () -> calc.calcular(new Entrada(
                List.of(item("TV", "1500.00", 1, "5.0")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE")));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", e.codigo());
    }

    @Test
    void erro_cartao_fora_do_intervalo() {
        ErroCalculo e = assertThrows(ErroCalculo.class, () -> calc.calcular(new Entrada(
                List.of(item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", null, "CARTAO", 13, "BRONZE", "SUDESTE")));
        assertEquals("PARCELAMENTO_INVALIDO", e.codigo());
    }
}
