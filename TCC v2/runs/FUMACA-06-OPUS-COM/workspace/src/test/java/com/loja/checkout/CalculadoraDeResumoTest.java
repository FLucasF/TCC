package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.dominio.CalculadoraDeResumo;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.ResumoCompra;
import com.loja.checkout.dominio.SolicitacaoResumo;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraDeResumoTest {

    private final CalculadoraDeResumo calculadora = new CalculadoraDeResumo();

    private static Item item(String nome, String preco, int quantidade, String peso) {
        return new Item(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private static final List<Item> CAMISETA_E_TENIS =
            List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20"));

    private ResumoCompra calcular(List<Item> itens, String modalidade, String cupom, String pagamento, Integer parcelas) {
        return calculadora.calcular(new SolicitacaoResumo(itens, modalidade, cupom, pagamento, parcelas));
    }

    private CodigoErro erroDe(List<Item> itens, String modalidade, String cupom, String pagamento, Integer parcelas) {
        try {
            calcular(itens, modalidade, cupom, pagamento, parcelas);
        } catch (ErroCheckout erro) {
            return erro.codigo();
        }
        throw new AssertionError("esperava um erro de checkout");
    }

    private void verificar(ResumoCompra resumo, String subtotal, String cupom, String frete, int prazo,
            String ajuste, String total, int parcelas, String valorParcela) {
        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(subtotal);
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(cupom);
        assertThat(resumo.frete()).isEqualByComparingTo(frete);
        assertThat(resumo.prazoEntregaDias()).isEqualTo(prazo);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo(ajuste);
        assertThat(resumo.totalFinal()).isEqualByComparingTo(total);
        assertThat(resumo.parcelas()).isEqualTo(parcelas);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(valorParcela);
    }

    @Test
    void exemplo1_expressa_com_bemvindo10_no_pix() {
        verificar(calcular(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", 1),
                "409.70", "40.97", "33.10", 2, "-20.09", "381.74", 1, "381.74");
    }

    @Test
    void exemplo2_economica_sem_cupom_no_cartao_em_6x() {
        verificar(calcular(CAMISETA_E_TENIS, "ECONOMICA", null, "CARTAO", 6),
                "409.70", "0.00", "15.60", 7, "30.10", "455.40", 6, "75.90");
    }

    @Test
    void exemplo3_motoboy_com_menos50_no_boleto() {
        verificar(calcular(List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO", null),
                "399.80", "50.00", "18.00", 0, "3.49", "371.29", 1, "371.29");
    }

    @Test
    void exemplo4_retirada_com_leve3pague2_no_cartao_em_3x() {
        verificar(calcular(List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                        "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3),
                "299.10", "39.80", "0.00", 1, "0.00", "259.30", 3, "86.43");
    }

    @Test
    void fretegratis_desconta_exatamente_o_frete() {
        ResumoCompra resumo = calcular(CAMISETA_E_TENIS, "EXPRESSA", "FRETEGRATIS", "CARTAO", 1);
        verificar(resumo, "409.70", "33.10", "33.10", 2, "0.00", "409.70", 1, "409.70");
    }

    @Test
    void carrinho_vazio_e_itens_invalidos_sao_pedido_invalido() {
        assertThat(erroDe(List.of(), "EXPRESSA", null, "PIX", 1)).isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(null, "EXPRESSA", null, "PIX", 1)).isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(List.of(item("Meia", "19.90", 0, "0.10")), "EXPRESSA", null, "PIX", 1))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(List.of(new Item("Meia", null, 1, new BigDecimal("0.10"))), "EXPRESSA", null, "PIX", 1))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(List.of(item("Meia", "19.90", 1, "-0.10")), "EXPRESSA", null, "PIX", 1))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void modalidade_inexistente_ou_ausente_e_invalida() {
        assertThat(erroDe(CAMISETA_E_TENIS, "DRONE", null, "PIX", 1)).isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
        assertThat(erroDe(CAMISETA_E_TENIS, null, null, "PIX", 1)).isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboy_acima_de_cinco_quilos_fica_indisponivel() {
        List<Item> pesado = List.of(item("Halter", "99.90", 3, "2.00"));
        assertThat(erroDe(pesado, "MOTOBOY", null, "PIX", 1)).isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void motoboy_com_exatos_cinco_quilos_e_aceito() {
        assertThat(calcular(List.of(item("Halter", "99.90", 5, "1.00")), "MOTOBOY", null, "BOLETO", 1).frete())
                .isEqualByComparingTo("18.00");
    }

    @Test
    void cupom_inexistente_e_cupom_sem_condicao() {
        assertThat(erroDe(CAMISETA_E_TENIS, "EXPRESSA", "NATAL99", "PIX", 1)).isEqualTo(CodigoErro.CUPOM_INVALIDO);
        assertThat(erroDe(CAMISETA_E_TENIS, "EXPRESSA", "bemvindo10", "PIX", 1)).isEqualTo(CodigoErro.CUPOM_INVALIDO);
        assertThat(erroDe(List.of(item("Meia", "19.90", 2, "0.10")), "EXPRESSA", "MENOS50", "PIX", 1))
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void forma_de_pagamento_e_parcelamento() {
        assertThat(erroDe(CAMISETA_E_TENIS, "EXPRESSA", null, "CRIPTO", 1))
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        assertThat(erroDe(CAMISETA_E_TENIS, "EXPRESSA", null, null, 1))
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        assertThat(erroDe(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", 2)).isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(CAMISETA_E_TENIS, "EXPRESSA", null, "BOLETO", 3)).isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(CAMISETA_E_TENIS, "EXPRESSA", null, "CARTAO", 13)).isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(CAMISETA_E_TENIS, "EXPRESSA", null, "CARTAO", 0)).isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boleto_acima_de_mil_reais_fica_indisponivel() {
        List<Item> caro = List.of(item("Jaqueta", "999.00", 1, "1.00"));
        assertThat(erroDe(caro, "EXPRESSA", null, "BOLETO", 1)).isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void parcelamento_e_verificado_antes_da_disponibilidade_do_boleto() {
        List<Item> caro = List.of(item("Jaqueta", "999.00", 1, "1.00"));
        assertThat(erroDe(caro, "EXPRESSA", null, "BOLETO", 2)).isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void parcelas_ausentes_valem_um() {
        assertThat(calcular(CAMISETA_E_TENIS, "RETIRADA_LOJA", null, "CARTAO", null).parcelas()).isEqualTo(1);
    }
}
