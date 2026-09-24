package com.loja.checkout;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.servico.CalculadoraResumo;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    private ResumoResponse calcular(ResumoRequest request) {
        return calculadora.calcular(request);
    }

    private void esperaErro(ResumoRequest request, String codigo) {
        assertThatThrownBy(() -> calculadora.calcular(request))
                .isInstanceOf(ErroCheckout.class)
                .extracting(erro -> ((ErroCheckout) erro).codigo())
                .isEqualTo(codigo);
    }

    private static void confere(ResumoResponse resumo,
                                String subtotal, String cupom, String frete, int prazo,
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
    void exemplo1_expressa_bemvindo10_pix() {
        ResumoResponse resumo = calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1));

        confere(resumo, "409.70", "40.97", "33.10", 2, "-20.09", "381.74", 1, "381.74");
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x() {
        ResumoResponse resumo = calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6));

        confere(resumo, "409.70", "0.00", "15.60", 7, "30.10", "455.40", 6, "75.90");
    }

    @Test
    void exemplo3_motoboy_menos50_boleto() {
        ResumoResponse resumo = calcular(new ResumoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO", null));

        confere(resumo, "399.80", "50.00", "18.00", 0, "3.49", "371.29", 1, "371.29");
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x() {
        ResumoResponse resumo = calcular(new ResumoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3));

        confere(resumo, "299.10", "39.80", "0.00", 1, "0.00", "259.30", 3, "86.43");
    }

    @Test
    void fretegratis_zera_o_frete_no_total_mas_mostra_o_frete() {
        ResumoResponse resumo = calcular(new ResumoRequest(
                List.of(CAMISETA), "EXPRESSA", "FRETEGRATIS", "BOLETO", 1));

        assertThat(resumo.frete()).isEqualByComparingTo("27.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("27.70");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("163.29");
    }

    @Test
    void carrinho_vazio_ou_item_invalido() {
        esperaErro(new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", 1), "PEDIDO_INVALIDO");
        esperaErro(new ResumoRequest(null, "XPTO", null, null, null), "PEDIDO_INVALIDO");
        esperaErro(new ResumoRequest(List.of(item("Camiseta", "0.00", 2, "0.30")),
                "EXPRESSA", null, "PIX", 1), "PEDIDO_INVALIDO");
        esperaErro(new ResumoRequest(List.of(item("Camiseta", "79.90", -1, "0.30")),
                "EXPRESSA", null, "PIX", 1), "PEDIDO_INVALIDO");
        esperaErro(new ResumoRequest(List.of(new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, null)),
                "EXPRESSA", null, "PIX", 1), "PEDIDO_INVALIDO");
    }

    @Test
    void modalidade_invalida_ou_indisponivel() {
        esperaErro(new ResumoRequest(List.of(CAMISETA), "DRONE", null, "PIX", 1), "MODALIDADE_INVALIDA");
        esperaErro(new ResumoRequest(List.of(CAMISETA), null, null, "PIX", 1), "MODALIDADE_INVALIDA");
        esperaErro(new ResumoRequest(List.of(item("Halter", "199.90", 3, "2.00")),
                "MOTOBOY", null, "PIX", 1), "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupom_invalido_ou_nao_aplicavel() {
        esperaErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", "bemvindo10", "PIX", 1), "CUPOM_INVALIDO");
        esperaErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", "MENOS50", "PIX", 1), "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void pagamento_invalido_parcelamento_e_indisponibilidade() {
        esperaErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "CHEQUE", 1), "FORMA_PAGAMENTO_INVALIDA");
        esperaErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, null, 1), "FORMA_PAGAMENTO_INVALIDA");
        esperaErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "PIX", 2), "PARCELAMENTO_INVALIDO");
        esperaErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13), "PARCELAMENTO_INVALIDO");
        esperaErro(new ResumoRequest(List.of(item("Sofa", "1500.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "BOLETO", 1), "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void erros_sao_devolvidos_na_ordem_combinada() {
        esperaErro(new ResumoRequest(List.of(item("Halter", "199.90", 3, "2.00")),
                "MOTOBOY", "NAOEXISTE", "CHEQUE", 99), "MODALIDADE_INDISPONIVEL");
        esperaErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", "MENOS50", "CHEQUE", 99), "CUPOM_NAO_APLICAVEL");
        esperaErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "PIX", 99), "PARCELAMENTO_INVALIDO");
    }
}
