package com.loja.checkout;

import com.loja.checkout.erro.ErroPedido;
import com.loja.checkout.servico.CalculadoraResumo;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraResumoTest {

    private final CalculadoraResumo calc = new CalculadoraResumo();

    private static ItemRequest item(String nome, String preco, int qtd, String peso) {
        ItemRequest i = new ItemRequest();
        i.nome = nome;
        i.precoUnitario = new BigDecimal(preco);
        i.quantidade = qtd;
        i.pesoKg = new BigDecimal(peso);
        return i;
    }

    private static ResumoRequest base() {
        ResumoRequest r = new ResumoRequest();
        r.itens = new ArrayList<>();
        return r;
    }

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    @Test
    void exemplo1_bemvindo10_pix_bronze_norte() {
        ResumoRequest r = base();
        r.itens.add(item("Camiseta", "79.90", 2, "0.30"));
        r.itens.add(item("Tenis", "249.90", 1, "1.20"));
        r.modalidadeEntrega = "EXPRESSA";
        r.cupom = "BEMVINDO10";
        r.formaPagamento = "PIX";
        r.nivelClube = "BRONZE";
        r.regiao = "NORTE";

        ResumoResponse res = calc.calcular(r);
        assertThat(res.subtotalProdutos).isEqualByComparingTo(bd("409.70"));
        assertThat(res.descontoCupom).isEqualByComparingTo(bd("40.97"));
        assertThat(res.frete).isEqualByComparingTo(bd("33.10"));
        assertThat(res.prazoEntregaDias).isEqualTo(2);
        assertThat(res.seguro).isEqualByComparingTo(bd("10.24"));
        assertThat(res.ajustePagamento).isEqualByComparingTo(bd("-20.60"));
        assertThat(res.totalFinal).isEqualByComparingTo(bd("391.47"));
        assertThat(res.parcelas).isEqualTo(1);
        assertThat(res.valorParcela).isEqualByComparingTo(bd("391.47"));
        assertThat(res.creditoProximaCompra).isEqualByComparingTo(bd("0.00"));
        assertThat(res.brinde).isFalse();
    }

    @Test
    void exemplo2_sem_cupom_cartao6x_prata_centro_oeste() {
        ResumoRequest r = base();
        r.itens.add(item("Camiseta", "79.90", 2, "0.30"));
        r.itens.add(item("Tenis", "249.90", 1, "1.20"));
        r.modalidadeEntrega = "ECONOMICA";
        r.formaPagamento = "CARTAO";
        r.parcelas = 6;
        r.nivelClube = "PRATA";
        r.regiao = "CENTRO_OESTE";

        ResumoResponse res = calc.calcular(r);
        assertThat(res.subtotalProdutos).isEqualByComparingTo(bd("409.70"));
        assertThat(res.descontoCupom).isEqualByComparingTo(bd("0.00"));
        assertThat(res.frete).isEqualByComparingTo(bd("15.60"));
        assertThat(res.prazoEntregaDias).isEqualTo(7);
        assertThat(res.seguro).isEqualByComparingTo(bd("6.15"));
        assertThat(res.ajustePagamento).isEqualByComparingTo(bd("30.55"));
        assertThat(res.totalFinal).isEqualByComparingTo(bd("462.00"));
        assertThat(res.parcelas).isEqualTo(6);
        assertThat(res.valorParcela).isEqualByComparingTo(bd("77.00"));
        assertThat(res.creditoProximaCompra).isEqualByComparingTo(bd("8.19"));
        assertThat(res.brinde).isFalse();
    }

    @Test
    void exemplo3_menos50_motoboy_boleto_bronze_nordeste() {
        ResumoRequest r = base();
        r.itens.add(item("Fone", "199.90", 2, "0.25"));
        r.modalidadeEntrega = "MOTOBOY";
        r.cupom = "MENOS50";
        r.formaPagamento = "BOLETO";
        r.nivelClube = "BRONZE";
        r.regiao = "NORDESTE";

        ResumoResponse res = calc.calcular(r);
        assertThat(res.subtotalProdutos).isEqualByComparingTo(bd("399.80"));
        assertThat(res.descontoCupom).isEqualByComparingTo(bd("50.00"));
        assertThat(res.frete).isEqualByComparingTo(bd("18.00"));
        assertThat(res.prazoEntregaDias).isEqualTo(0);
        assertThat(res.seguro).isEqualByComparingTo(bd("8.00"));
        assertThat(res.ajustePagamento).isEqualByComparingTo(bd("3.49"));
        assertThat(res.totalFinal).isEqualByComparingTo(bd("379.29"));
        assertThat(res.valorParcela).isEqualByComparingTo(bd("379.29"));
        assertThat(res.creditoProximaCompra).isEqualByComparingTo(bd("0.00"));
    }

    @Test
    void exemplo4_leve3pague2_retirada_cartao3x_prata_sul() {
        ResumoRequest r = base();
        r.itens.add(item("Meia", "19.90", 7, "0.10"));
        r.itens.add(item("Camiseta", "79.90", 2, "0.30"));
        r.modalidadeEntrega = "RETIRADA_LOJA";
        r.cupom = "LEVE3PAGUE2";
        r.formaPagamento = "CARTAO";
        r.parcelas = 3;
        r.nivelClube = "PRATA";
        r.regiao = "SUL";

        ResumoResponse res = calc.calcular(r);
        assertThat(res.subtotalProdutos).isEqualByComparingTo(bd("299.10"));
        assertThat(res.descontoCupom).isEqualByComparingTo(bd("39.80"));
        assertThat(res.frete).isEqualByComparingTo(bd("0.00"));
        assertThat(res.prazoEntregaDias).isEqualTo(1);
        assertThat(res.seguro).isEqualByComparingTo(bd("2.99"));
        assertThat(res.ajustePagamento).isEqualByComparingTo(bd("0.00"));
        assertThat(res.totalFinal).isEqualByComparingTo(bd("262.29"));
        assertThat(res.parcelas).isEqualTo(3);
        assertThat(res.valorParcela).isEqualByComparingTo(bd("87.43"));
        assertThat(res.creditoProximaCompra).isEqualByComparingTo(bd("5.98"));
    }

    @Test
    void exemplo5_ouro_pix_sudeste() {
        ResumoRequest r = base();
        r.itens.add(item("Camiseta", "79.90", 2, "0.30"));
        r.itens.add(item("Tenis", "249.90", 1, "1.20"));
        r.modalidadeEntrega = "EXPRESSA";
        r.formaPagamento = "PIX";
        r.nivelClube = "OURO";
        r.regiao = "SUDESTE";

        ResumoResponse res = calc.calcular(r);
        assertThat(res.subtotalProdutos).isEqualByComparingTo(bd("409.70"));
        assertThat(res.descontoCupom).isEqualByComparingTo(bd("0.00"));
        assertThat(res.frete).isEqualByComparingTo(bd("0.00"));
        assertThat(res.prazoEntregaDias).isEqualTo(2);
        assertThat(res.seguro).isEqualByComparingTo(bd("4.10"));
        assertThat(res.ajustePagamento).isEqualByComparingTo(bd("-20.69"));
        assertThat(res.totalFinal).isEqualByComparingTo(bd("393.11"));
        assertThat(res.valorParcela).isEqualByComparingTo(bd("393.11"));
        assertThat(res.creditoProximaCompra).isEqualByComparingTo(bd("20.48"));
        assertThat(res.brinde).isFalse();
    }

    @Test
    void fretegratis_zera_frete_no_total() {
        ResumoRequest r = base();
        r.itens.add(item("Camiseta", "79.90", 2, "0.30"));
        r.itens.add(item("Tenis", "249.90", 1, "1.20"));
        r.modalidadeEntrega = "EXPRESSA";
        r.cupom = "FRETEGRATIS";
        r.formaPagamento = "PIX";
        r.nivelClube = "BRONZE";
        r.regiao = "SUDESTE";

        ResumoResponse res = calc.calcular(r);
        assertThat(res.frete).isEqualByComparingTo(bd("33.10"));
        assertThat(res.descontoCupom).isEqualByComparingTo(bd("33.10"));
        assertThat(res.seguro).isEqualByComparingTo(bd("4.10"));
        assertThat(res.totalFinal).isEqualByComparingTo(bd("393.11"));
    }

    @Test
    void ouro_ganha_brinde_quando_passa_de_500() {
        ResumoRequest r = base();
        r.itens.add(item("Casaco", "600.00", 1, "0.80"));
        r.modalidadeEntrega = "EXPRESSA";
        r.formaPagamento = "PIX";
        r.nivelClube = "OURO";
        r.regiao = "SUDESTE";

        ResumoResponse res = calc.calcular(r);
        assertThat(res.frete).isEqualByComparingTo(bd("0.00"));
        assertThat(res.brinde).isTrue();
        assertThat(res.creditoProximaCompra).isEqualByComparingTo(bd("30.00"));
    }

    @Test
    void cartao_4x_entra_na_regra_price() {
        ResumoRequest r = base();
        r.itens.add(item("Camiseta", "79.90", 2, "0.30"));
        r.itens.add(item("Tenis", "249.90", 1, "1.20"));
        r.modalidadeEntrega = "ECONOMICA";
        r.formaPagamento = "CARTAO";
        r.parcelas = 4;
        r.nivelClube = "BRONZE";
        r.regiao = "SUDESTE";

        ResumoResponse res = calc.calcular(r);
        assertThat(res.ajustePagamento.signum()).isPositive();
        assertThat(res.totalFinal).isEqualByComparingTo(
                res.valorParcela.multiply(new BigDecimal("4")));
    }

    @Test
    void cartao_13x_rejeita() {
        ResumoRequest r = base();
        r.itens.add(item("X", "100.00", 1, "0.10"));
        r.modalidadeEntrega = "EXPRESSA";
        r.formaPagamento = "CARTAO";
        r.parcelas = 13;
        r.nivelClube = "BRONZE";
        r.regiao = "SUDESTE";
        assertThatThrownBy(() -> calc.calcular(r))
                .hasMessage("PARCELAMENTO_INVALIDO");
    }

    @Test
    void pedido_invalido_carrinho_vazio() {
        ResumoRequest r = base();
        r.modalidadeEntrega = "EXPRESSA";
        r.formaPagamento = "PIX";
        r.nivelClube = "BRONZE";
        r.regiao = "SUDESTE";
        assertThatThrownBy(() -> calc.calcular(r))
                .isInstanceOf(ErroPedido.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void pedido_invalido_preco_zero() {
        ResumoRequest r = base();
        r.itens.add(item("X", "0.00", 1, "0.10"));
        r.modalidadeEntrega = "EXPRESSA";
        r.formaPagamento = "PIX";
        r.nivelClube = "BRONZE";
        r.regiao = "SUDESTE";
        assertThatThrownBy(() -> calc.calcular(r))
                .isInstanceOf(ErroPedido.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void nivel_clube_invalido() {
        ResumoRequest r = base();
        r.itens.add(item("X", "10.00", 1, "0.10"));
        r.modalidadeEntrega = "EXPRESSA";
        r.formaPagamento = "PIX";
        r.regiao = "SUDESTE";
        r.nivelClube = "PLATINA";
        assertThatThrownBy(() -> calc.calcular(r))
                .hasMessage("NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiao_invalida() {
        ResumoRequest r = base();
        r.itens.add(item("X", "10.00", 1, "0.10"));
        r.modalidadeEntrega = "EXPRESSA";
        r.formaPagamento = "PIX";
        r.nivelClube = "BRONZE";
        assertThatThrownBy(() -> calc.calcular(r))
                .hasMessage("REGIAO_INVALIDA");
    }

    @Test
    void modalidade_invalida() {
        ResumoRequest r = base();
        r.itens.add(item("X", "10.00", 1, "0.10"));
        r.formaPagamento = "PIX";
        r.nivelClube = "BRONZE";
        r.regiao = "SUDESTE";
        r.modalidadeEntrega = "DRONE";
        assertThatThrownBy(() -> calc.calcular(r))
                .hasMessage("MODALIDADE_INVALIDA");
    }

    @Test
    void modalidade_indisponivel_motoboy_acima_5kg() {
        ResumoRequest r = base();
        r.itens.add(item("Saco", "10.00", 1, "6.00"));
        r.modalidadeEntrega = "MOTOBOY";
        r.formaPagamento = "PIX";
        r.nivelClube = "BRONZE";
        r.regiao = "SUDESTE";
        assertThatThrownBy(() -> calc.calcular(r))
                .hasMessage("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupom_invalido() {
        ResumoRequest r = base();
        r.itens.add(item("X", "10.00", 1, "0.10"));
        r.modalidadeEntrega = "EXPRESSA";
        r.formaPagamento = "PIX";
        r.nivelClube = "BRONZE";
        r.regiao = "SUDESTE";
        r.cupom = "INEXISTENTE";
        assertThatThrownBy(() -> calc.calcular(r))
                .hasMessage("CUPOM_INVALIDO");
    }

    @Test
    void cupom_nao_aplicavel_menos50_abaixo_300() {
        ResumoRequest r = base();
        r.itens.add(item("X", "100.00", 1, "0.10"));
        r.modalidadeEntrega = "EXPRESSA";
        r.formaPagamento = "PIX";
        r.nivelClube = "BRONZE";
        r.regiao = "SUDESTE";
        r.cupom = "MENOS50";
        assertThatThrownBy(() -> calc.calcular(r))
                .hasMessage("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void forma_pagamento_invalida() {
        ResumoRequest r = base();
        r.itens.add(item("X", "10.00", 1, "0.10"));
        r.modalidadeEntrega = "EXPRESSA";
        r.nivelClube = "BRONZE";
        r.regiao = "SUDESTE";
        r.formaPagamento = "CRIPTO";
        assertThatThrownBy(() -> calc.calcular(r))
                .hasMessage("FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void parcelamento_invalido_pix_em_2x() {
        ResumoRequest r = base();
        r.itens.add(item("X", "10.00", 1, "0.10"));
        r.modalidadeEntrega = "EXPRESSA";
        r.nivelClube = "BRONZE";
        r.regiao = "SUDESTE";
        r.formaPagamento = "PIX";
        r.parcelas = 2;
        assertThatThrownBy(() -> calc.calcular(r))
                .hasMessage("PARCELAMENTO_INVALIDO");
    }

    @Test
    void forma_pagamento_indisponivel_boleto_acima_1000() {
        ResumoRequest r = base();
        r.itens.add(item("X", "600.00", 2, "0.10"));
        r.modalidadeEntrega = "EXPRESSA";
        r.nivelClube = "BRONZE";
        r.regiao = "SUDESTE";
        r.formaPagamento = "BOLETO";
        assertThatThrownBy(() -> calc.calcular(r))
                .hasMessage("FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
