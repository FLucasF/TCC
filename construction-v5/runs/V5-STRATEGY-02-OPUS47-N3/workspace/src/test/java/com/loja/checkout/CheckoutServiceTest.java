package com.loja.checkout;

import com.loja.checkout.domain.CheckoutService;
import com.loja.checkout.domain.Resumo;
import com.loja.checkout.web.dto.CheckoutRequest;
import com.loja.checkout.web.dto.ItemRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static ItemRequest item(String nome, String preco, int qtd, String peso) {
        ItemRequest i = new ItemRequest();
        i.nome = nome;
        i.precoUnitario = new BigDecimal(preco);
        i.quantidade = qtd;
        i.pesoKg = new BigDecimal(peso);
        return i;
    }

    private static CheckoutRequest base() {
        CheckoutRequest r = new CheckoutRequest();
        r.itens = List.of(
                item("Camiseta", "79.90", 2, "0.30"),
                item("Tenis", "249.90", 1, "1.20"));
        return r;
    }

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        CheckoutRequest r = base();
        r.modalidadeEntrega = "EXPRESSA";
        r.cupom = "BEMVINDO10";
        r.formaPagamento = "PIX";
        r.nivelClube = "BRONZE";
        r.regiao = "NORTE";

        Resumo s = service.calcular(r);
        assertEquals(bd("409.70"), s.subtotalProdutos());
        assertEquals(bd("40.97"), s.descontoCupom());
        assertEquals(bd("33.10"), s.frete());
        assertEquals(2, s.prazoEntregaDias());
        assertEquals(bd("10.24"), s.seguro());
        assertEquals(bd("-20.60"), s.ajustePagamento());
        assertEquals(bd("391.47"), s.totalFinal());
        assertEquals(1, s.parcelas());
        assertEquals(bd("391.47"), s.valorParcela());
        assertEquals(bd("0.00"), s.creditoProximaCompra());
        assertFalse(s.brinde());
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x_prata_centro_oeste() {
        CheckoutRequest r = base();
        r.modalidadeEntrega = "ECONOMICA";
        r.formaPagamento = "CARTAO";
        r.parcelas = 6;
        r.nivelClube = "PRATA";
        r.regiao = "CENTRO_OESTE";

        Resumo s = service.calcular(r);
        assertEquals(bd("409.70"), s.subtotalProdutos());
        assertEquals(bd("0.00"), s.descontoCupom());
        assertEquals(bd("15.60"), s.frete());
        assertEquals(7, s.prazoEntregaDias());
        assertEquals(bd("6.15"), s.seguro());
        assertEquals(bd("30.55"), s.ajustePagamento());
        assertEquals(bd("462.00"), s.totalFinal());
        assertEquals(6, s.parcelas());
        assertEquals(bd("77.00"), s.valorParcela());
        assertEquals(bd("8.19"), s.creditoProximaCompra());
        assertFalse(s.brinde());
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        CheckoutRequest r = new CheckoutRequest();
        r.itens = List.of(item("Fone", "199.90", 2, "0.25"));
        r.modalidadeEntrega = "MOTOBOY";
        r.cupom = "MENOS50";
        r.formaPagamento = "BOLETO";
        r.nivelClube = "BRONZE";
        r.regiao = "NORDESTE";

        Resumo s = service.calcular(r);
        assertEquals(bd("399.80"), s.subtotalProdutos());
        assertEquals(bd("50.00"), s.descontoCupom());
        assertEquals(bd("18.00"), s.frete());
        assertEquals(0, s.prazoEntregaDias());
        assertEquals(bd("8.00"), s.seguro());
        assertEquals(bd("3.49"), s.ajustePagamento());
        assertEquals(bd("379.29"), s.totalFinal());
        assertEquals(1, s.parcelas());
        assertEquals(bd("379.29"), s.valorParcela());
        assertEquals(bd("0.00"), s.creditoProximaCompra());
        assertFalse(s.brinde());
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x_prata_sul() {
        CheckoutRequest r = new CheckoutRequest();
        r.itens = List.of(
                item("Meia", "19.90", 7, "0.10"),
                item("Camiseta", "79.90", 2, "0.30"));
        r.modalidadeEntrega = "RETIRADA_LOJA";
        r.cupom = "LEVE3PAGUE2";
        r.formaPagamento = "CARTAO";
        r.parcelas = 3;
        r.nivelClube = "PRATA";
        r.regiao = "SUL";

        Resumo s = service.calcular(r);
        assertEquals(bd("299.10"), s.subtotalProdutos());
        assertEquals(bd("39.80"), s.descontoCupom());
        assertEquals(bd("0.00"), s.frete());
        assertEquals(1, s.prazoEntregaDias());
        assertEquals(bd("2.99"), s.seguro());
        assertEquals(bd("0.00"), s.ajustePagamento());
        assertEquals(bd("262.29"), s.totalFinal());
        assertEquals(3, s.parcelas());
        assertEquals(bd("87.43"), s.valorParcela());
        assertEquals(bd("5.98"), s.creditoProximaCompra());
        assertFalse(s.brinde());
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        CheckoutRequest r = base();
        r.modalidadeEntrega = "EXPRESSA";
        r.formaPagamento = "PIX";
        r.nivelClube = "OURO";
        r.regiao = "SUDESTE";

        Resumo s = service.calcular(r);
        assertEquals(bd("409.70"), s.subtotalProdutos());
        assertEquals(bd("0.00"), s.descontoCupom());
        assertEquals(bd("0.00"), s.frete());
        assertEquals(2, s.prazoEntregaDias());
        assertEquals(bd("4.10"), s.seguro());
        assertEquals(bd("-20.69"), s.ajustePagamento());
        assertEquals(bd("393.11"), s.totalFinal());
        assertEquals(1, s.parcelas());
        assertEquals(bd("393.11"), s.valorParcela());
        assertEquals(bd("20.48"), s.creditoProximaCompra());
        assertFalse(s.brinde());
    }
}
