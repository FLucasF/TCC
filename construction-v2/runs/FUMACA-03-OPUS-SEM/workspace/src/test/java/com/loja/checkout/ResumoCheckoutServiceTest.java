package com.loja.checkout;

import com.loja.checkout.api.ResumoRequisicao;
import com.loja.checkout.api.ResumoRequisicao.ItemRequisicao;
import com.loja.checkout.api.ResumoResposta;
import com.loja.checkout.api.ResumoCheckoutService;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.ErroCheckout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ResumoCheckoutServiceTest {

    @Autowired
    ResumoCheckoutService servico;

    private static ItemRequisicao item(String nome, String preco, int qtd, String peso) {
        return new ItemRequisicao(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static final ItemRequisicao CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequisicao TENIS = item("Tenis", "249.90", 1, "1.20");

    private ResumoResposta calcular(ResumoRequisicao requisicao) {
        return servico.calcular(requisicao);
    }

    private void esperarErro(ResumoRequisicao requisicao, CodigoErro codigo) {
        assertThatThrownBy(() -> servico.calcular(requisicao))
                .isInstanceOf(ErroCheckout.class)
                .extracting(erro -> ((ErroCheckout) erro).getCodigo())
                .isEqualTo(codigo);
    }

    @Test
    @DisplayName("Exemplo 1: EXPRESSA + BEMVINDO10 + PIX")
    void exemplo1() {
        ResumoResposta resumo = calcular(new ResumoRequisicao(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.09");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("381.74");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("381.74");
    }

    @Test
    @DisplayName("Exemplo 2: ECONOMICA sem cupom + CARTAO 6x com juros")
    void exemplo2() {
        ResumoResposta resumo = calcular(new ResumoRequisicao(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("15.60");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("30.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("455.40");
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("75.90");
    }

    @Test
    @DisplayName("Exemplo 3: MOTOBOY + MENOS50 + BOLETO")
    void exemplo3() {
        ResumoResposta resumo = calcular(new ResumoRequisicao(
                List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO", null));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isZero();
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("371.29");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("371.29");
    }

    @Test
    @DisplayName("Exemplo 4: RETIRADA_LOJA + LEVE3PAGUE2 + CARTAO 3x sem juros")
    void exemplo4() {
        ResumoResposta resumo = calcular(new ResumoRequisicao(
                List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(resumo.parcelas()).isEqualTo(3);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("86.43");
    }

    @Test
    @DisplayName("FRETEGRATIS zera o frete mantendo ele no resumo")
    void freteGratis() {
        ResumoResposta resumo = calcular(new ResumoRequisicao(
                List.of(CAMISETA), "EXPRESSA", "FRETEGRATIS", "CARTAO", 1));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("159.80");
        assertThat(resumo.frete()).isEqualByComparingTo("27.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("27.70");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("159.80");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Parcelas ausentes valem 1")
    void parcelasAusentesValem1() {
        ResumoResposta resumo = calcular(new ResumoRequisicao(
                List.of(CAMISETA), "RETIRADA_LOJA", null, "CARTAO", null));

        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("159.80");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("159.80");
    }

    @Test
    @DisplayName("Arredondamento meio para o par nos centavos")
    void arredondamentoMeioParaOPar() {
        // 59.90 x 1 = 59.90; 10% = 5.99 exatos, sem ambiguidade
        ResumoResposta meioParaBaixo = calcular(new ResumoRequisicao(
                List.of(item("Bone", "29.85", 1, "0.10")), "RETIRADA_LOJA", "BEMVINDO10", "PIX", 1));
        // 10% de 29.85 = 2.985 -> 2.98
        assertThat(meioParaBaixo.descontoCupom()).isEqualByComparingTo("2.98");

        ResumoResposta meioParaCima = calcular(new ResumoRequisicao(
                List.of(item("Bone", "29.95", 1, "0.10")), "RETIRADA_LOJA", "BEMVINDO10", "PIX", 1));
        // 10% de 29.95 = 2.995 -> 3.00
        assertThat(meioParaCima.descontoCupom()).isEqualByComparingTo("3.00");
    }

    @Test
    void carrinhoVazioOuItemInvalido() {
        esperarErro(new ResumoRequisicao(List.of(), "EXPRESSA", null, "PIX", 1),
                CodigoErro.PEDIDO_INVALIDO);
        esperarErro(new ResumoRequisicao(null, "EXPRESSA", null, "PIX", 1),
                CodigoErro.PEDIDO_INVALIDO);
        esperarErro(new ResumoRequisicao(
                        List.of(item("Camiseta", "0.00", 1, "0.30")), "EXPRESSA", null, "PIX", 1),
                CodigoErro.PEDIDO_INVALIDO);
        esperarErro(new ResumoRequisicao(
                        List.of(item("Camiseta", "79.90", 0, "0.30")), "EXPRESSA", null, "PIX", 1),
                CodigoErro.PEDIDO_INVALIDO);
        esperarErro(new ResumoRequisicao(
                        List.of(item("Camiseta", "79.90", 1, "-0.30")), "EXPRESSA", null, "PIX", 1),
                CodigoErro.PEDIDO_INVALIDO);
        esperarErro(new ResumoRequisicao(
                        List.of(new ItemRequisicao("Camiseta", null, 1, new BigDecimal("0.30"))),
                        "EXPRESSA", null, "PIX", 1),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void modalidadeInvalidaOuIndisponivel() {
        esperarErro(new ResumoRequisicao(List.of(CAMISETA), "DRONE", null, "PIX", 1),
                CodigoErro.MODALIDADE_INVALIDA);
        esperarErro(new ResumoRequisicao(List.of(CAMISETA), null, null, "PIX", 1),
                CodigoErro.MODALIDADE_INVALIDA);
        esperarErro(new ResumoRequisicao(List.of(CAMISETA), "expressa", null, "PIX", 1),
                CodigoErro.MODALIDADE_INVALIDA);
        esperarErro(new ResumoRequisicao(
                        List.of(item("Halter", "99.90", 3, "2.00")), "MOTOBOY", null, "PIX", 1),
                CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void motoboyAceitaExatamente5Kg() {
        ResumoResposta resumo = calcular(new ResumoRequisicao(
                List.of(item("Halter", "99.90", 2, "2.50")), "MOTOBOY", null, "PIX", 1));

        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
    }

    @Test
    void cupomInvalidoOuNaoAplicavel() {
        esperarErro(new ResumoRequisicao(List.of(CAMISETA), "EXPRESSA", "PROMO999", "PIX", 1),
                CodigoErro.CUPOM_INVALIDO);
        esperarErro(new ResumoRequisicao(List.of(CAMISETA), "EXPRESSA", "menos50", "PIX", 1),
                CodigoErro.CUPOM_INVALIDO);
        esperarErro(new ResumoRequisicao(List.of(CAMISETA), "EXPRESSA", "MENOS50", "PIX", 1),
                CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void menos50ValeApartirDe300EmProdutos() {
        ResumoResposta resumo = calcular(new ResumoRequisicao(
                List.of(item("Jaqueta", "300.00", 1, "1.00")),
                "RETIRADA_LOJA", "MENOS50", "CARTAO", 1));

        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("250.00");
    }

    @Test
    void pagamentoInvalidoParcelamentoEDisponibilidade() {
        esperarErro(new ResumoRequisicao(List.of(CAMISETA), "EXPRESSA", null, "CRIPTO", 1),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        esperarErro(new ResumoRequisicao(List.of(CAMISETA), "EXPRESSA", null, null, 1),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        esperarErro(new ResumoRequisicao(List.of(CAMISETA), "EXPRESSA", null, "PIX", 2),
                CodigoErro.PARCELAMENTO_INVALIDO);
        esperarErro(new ResumoRequisicao(List.of(CAMISETA), "EXPRESSA", null, "BOLETO", 3),
                CodigoErro.PARCELAMENTO_INVALIDO);
        esperarErro(new ResumoRequisicao(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13),
                CodigoErro.PARCELAMENTO_INVALIDO);
        esperarErro(new ResumoRequisicao(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 0),
                CodigoErro.PARCELAMENTO_INVALIDO);
        esperarErro(new ResumoRequisicao(
                        List.of(item("Sofa", "1200.00", 1, "1.00")),
                        "RETIRADA_LOJA", null, "BOLETO", 1),
                CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    @DisplayName("Boleto aceita exatamente R$ 1.000,00 de total do pedido")
    void boletoAceitaLimite() {
        ResumoResposta resumo = calcular(new ResumoRequisicao(
                List.of(item("Sofa", "1000.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "BOLETO", 1));

        assertThat(resumo.totalFinal()).isEqualByComparingTo("1003.49");
    }

    @Test
    @DisplayName("A ordem dos erros segue o combinado com o site")
    void ordemDosErros() {
        // carrinho invalido ganha da modalidade e da forma de pagamento invalidas
        esperarErro(new ResumoRequisicao(List.of(), "DRONE", "PROMO999", "CRIPTO", 9),
                CodigoErro.PEDIDO_INVALIDO);
        // modalidade ganha do cupom e do pagamento
        esperarErro(new ResumoRequisicao(List.of(CAMISETA), "DRONE", "PROMO999", "CRIPTO", 9),
                CodigoErro.MODALIDADE_INVALIDA);
        // cupom ganha do pagamento
        esperarErro(new ResumoRequisicao(List.of(CAMISETA), "EXPRESSA", "PROMO999", "CRIPTO", 9),
                CodigoErro.CUPOM_INVALIDO);
        // forma de pagamento ganha do parcelamento
        esperarErro(new ResumoRequisicao(List.of(CAMISETA), "EXPRESSA", null, "CRIPTO", 9),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        // parcelamento ganha da disponibilidade do boleto
        esperarErro(new ResumoRequisicao(
                        List.of(item("Sofa", "1200.00", 1, "1.00")),
                        "RETIRADA_LOJA", null, "BOLETO", 2),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }
}
