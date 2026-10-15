package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.api.CodigoErro;
import com.loja.checkout.api.ErroCheckout;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoRequest.ItemRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.servico.CalculadoraResumo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    private static ItemRequest item(String nome, String preco, Integer quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade,
                peso == null ? null : new BigDecimal(peso));
    }

    private ResumoResponse calcular(List<ItemRequest> itens, String entrega, String cupom,
            String pagamento, Integer parcelas) {
        return calculadora.calcular(
                new ResumoRequest(itens, entrega, cupom, pagamento, parcelas));
    }

    private void esperaErro(CodigoErro codigo, List<ItemRequest> itens, String entrega,
            String cupom, String pagamento, Integer parcelas) {
        assertThatThrownBy(() -> calcular(itens, entrega, cupom, pagamento, parcelas))
                .isInstanceOf(ErroCheckout.class)
                .extracting(erro -> ((ErroCheckout) erro).codigo())
                .isEqualTo(codigo);
    }

    private void confere(ResumoResponse resumo, String subtotal, String cupom, String frete,
            int prazo, String ajuste, String totalFinal, int parcelas, String valorParcela) {
        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(subtotal);
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(cupom);
        assertThat(resumo.frete()).isEqualByComparingTo(frete);
        assertThat(resumo.prazoEntregaDias()).isEqualTo(prazo);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo(ajuste);
        assertThat(resumo.totalFinal()).isEqualByComparingTo(totalFinal);
        assertThat(resumo.parcelas()).isEqualTo(parcelas);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(valorParcela);
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix() {
        confere(calcular(List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1),
                "409.70", "40.97", "33.10", 2, "-20.09", "381.74", 1, "381.74");
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x() {
        confere(calcular(List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6),
                "409.70", "0.00", "15.60", 7, "30.10", "455.40", 6, "75.90");
    }

    @Test
    void exemplo3_motoboy_menos50_boleto() {
        confere(calcular(List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50",
                        "BOLETO", null),
                "399.80", "50.00", "18.00", 0, "3.49", "371.29", 1, "371.29");
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x() {
        confere(calcular(List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA), "RETIRADA_LOJA",
                        "LEVE3PAGUE2", "CARTAO", 3),
                "299.10", "39.80", "0.00", 1, "0.00", "259.30", 3, "86.43");
    }

    @Test
    void fretegratis_zera_o_frete_mas_mostra_ele_no_resumo() {
        confere(calcular(List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "CARTAO", 1),
                "409.70", "33.10", "33.10", 2, "0.00", "409.70", 1, "409.70");
    }

    @Test
    void carrinho_vazio_e_item_invalido() {
        esperaErro(CodigoErro.PEDIDO_INVALIDO, List.of(), "EXPRESSA", null, "PIX", 1);
        esperaErro(CodigoErro.PEDIDO_INVALIDO, List.of(item("Meia", "0.00", 1, "0.10")),
                "EXPRESSA", null, "PIX", 1);
        esperaErro(CodigoErro.PEDIDO_INVALIDO, List.of(item("Meia", "19.90", 0, "0.10")),
                "EXPRESSA", null, "PIX", 1);
        esperaErro(CodigoErro.PEDIDO_INVALIDO, List.of(item("Meia", "19.90", 1, null)),
                "EXPRESSA", null, "PIX", 1);
    }

    @Test
    void modalidade_inexistente_ou_ausente() {
        esperaErro(CodigoErro.MODALIDADE_INVALIDA, List.of(CAMISETA), "DRONE", null, "PIX", 1);
        esperaErro(CodigoErro.MODALIDADE_INVALIDA, List.of(CAMISETA), null, null, "PIX", 1);
    }

    @Test
    void motoboy_acima_de_cinco_quilos() {
        esperaErro(CodigoErro.MODALIDADE_INDISPONIVEL,
                List.of(item("Halter", "99.90", 6, "1.00")), "MOTOBOY", null, "PIX", 1);
    }

    @Test
    void motoboy_com_exatos_cinco_quilos_e_aceito() {
        assertThat(calcular(List.of(item("Halter", "99.90", 5, "1.00")), "MOTOBOY", null, "PIX", 1)
                .frete()).isEqualByComparingTo("18.00");
    }

    @Test
    void cupom_inexistente_e_cupom_sem_condicao() {
        esperaErro(CodigoErro.CUPOM_INVALIDO, List.of(CAMISETA), "EXPRESSA", "NATAL", "PIX", 1);
        esperaErro(CodigoErro.CUPOM_INVALIDO, List.of(CAMISETA), "EXPRESSA", "menos50", "PIX", 1);
        esperaErro(CodigoErro.CUPOM_NAO_APLICAVEL, List.of(CAMISETA), "EXPRESSA", "MENOS50",
                "PIX", 1);
    }

    @Test
    void forma_de_pagamento_inexistente_ou_ausente() {
        esperaErro(CodigoErro.FORMA_PAGAMENTO_INVALIDA, List.of(CAMISETA), "EXPRESSA", null,
                "DINHEIRO", 1);
        esperaErro(CodigoErro.FORMA_PAGAMENTO_INVALIDA, List.of(CAMISETA), "EXPRESSA", null,
                null, 1);
    }

    @Test
    void parcelamento_fora_do_permitido() {
        esperaErro(CodigoErro.PARCELAMENTO_INVALIDO, List.of(CAMISETA), "EXPRESSA", null, "PIX", 2);
        esperaErro(CodigoErro.PARCELAMENTO_INVALIDO, List.of(CAMISETA), "EXPRESSA", null,
                "BOLETO", 3);
        esperaErro(CodigoErro.PARCELAMENTO_INVALIDO, List.of(CAMISETA), "EXPRESSA", null,
                "CARTAO", 13);
        esperaErro(CodigoErro.PARCELAMENTO_INVALIDO, List.of(CAMISETA), "EXPRESSA", null,
                "CARTAO", 0);
    }

    @Test
    void boleto_acima_de_mil_reais() {
        esperaErro(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL,
                List.of(item("Jaqueta", "499.90", 3, "0.50")), "RETIRADA_LOJA", null, "BOLETO", 1);
    }

    @Test
    void ordem_dos_erros_do_pedido_ate_o_pagamento() {
        esperaErro(CodigoErro.PEDIDO_INVALIDO, List.of(), "DRONE", "NATAL", "DINHEIRO", 9);
        esperaErro(CodigoErro.MODALIDADE_INVALIDA, List.of(CAMISETA), "DRONE", "NATAL",
                "DINHEIRO", 9);
        esperaErro(CodigoErro.MODALIDADE_INDISPONIVEL, List.of(item("Halter", "99.90", 6, "1.00")),
                "MOTOBOY", "NATAL", "DINHEIRO", 9);
        esperaErro(CodigoErro.CUPOM_INVALIDO, List.of(CAMISETA), "EXPRESSA", "NATAL",
                "DINHEIRO", 9);
        esperaErro(CodigoErro.CUPOM_NAO_APLICAVEL, List.of(CAMISETA), "EXPRESSA", "MENOS50",
                "DINHEIRO", 9);
        esperaErro(CodigoErro.FORMA_PAGAMENTO_INVALIDA, List.of(CAMISETA), "EXPRESSA", null,
                "DINHEIRO", 9);
    }

    @Test
    void valores_saem_sempre_com_duas_casas() {
        ResumoResponse resumo = calcular(List.of(item("Meia", "19.90", 1, "0.10")),
                "RETIRADA_LOJA", null, "CARTAO", 1);
        assertThat(resumo.subtotalProdutos().scale()).isEqualTo(2);
        assertThat(resumo.descontoCupom().scale()).isEqualTo(2);
        assertThat(resumo.frete().scale()).isEqualTo(2);
        assertThat(resumo.ajustePagamento().scale()).isEqualTo(2);
        assertThat(resumo.totalFinal().scale()).isEqualTo(2);
        assertThat(resumo.valorParcela().scale()).isEqualTo(2);
    }
}
