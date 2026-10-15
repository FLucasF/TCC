package com.loja.checkout;

import com.loja.checkout.clube.ClubeBronze;
import com.loja.checkout.clube.ClubeOuro;
import com.loja.checkout.clube.ClubePrata;
import com.loja.checkout.cupom.CupomBemVindo10;
import com.loja.checkout.cupom.CupomFreteGratis;
import com.loja.checkout.cupom.CupomLeve3Pague2;
import com.loja.checkout.cupom.CupomMenos50;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.entrega.EntregaEconomica;
import com.loja.checkout.entrega.EntregaExpressa;
import com.loja.checkout.entrega.EntregaMotoboy;
import com.loja.checkout.entrega.RetiradaNaLoja;
import com.loja.checkout.pagamento.PagamentoBoleto;
import com.loja.checkout.pagamento.PagamentoCartao;
import com.loja.checkout.pagamento.PagamentoPix;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo(
            new Catalogo<>(List.of(new EntregaEconomica(), new EntregaExpressa(),
                    new RetiradaNaLoja(), new EntregaMotoboy())),
            new Catalogo<>(List.of(new CupomBemVindo10(), new CupomMenos50(),
                    new CupomFreteGratis(), new CupomLeve3Pague2())),
            new Catalogo<>(List.of(new ClubeBronze(), new ClubePrata(), new ClubeOuro())),
            new Catalogo<>(List.of(new PagamentoPix(), new PagamentoBoleto(), new PagamentoCartao())));

    @Test
    @DisplayName("FRETEGRATIS: o frete aparece e o desconto fica igual a ele")
    void freteGratis() {
        ResumoCheckout resumo = calcular(umaRequisicao("EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualTo(new BigDecimal("33.10"));
        assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("33.10"));
    }

    @Test
    @DisplayName("OURO com FRETEGRATIS: nao ha frete a descontar")
    void freteGratisParaQuemJaNaoPagaFrete() {
        ResumoCheckout resumo = calcular(umaRequisicao("EXPRESSA", "FRETEGRATIS", "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.frete()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("OURO manda brinde acima de R$ 500,00 em produtos")
    void brindeDoOuro() {
        RequisicaoResumo requisicao = new RequisicaoResumo(
                List.of(new RequisicaoResumo.ItemRequisicao("Jaqueta", new BigDecimal("600.00"), 1, new BigDecimal("1.0"))),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoCheckout resumo = calculadora.calcular(requisicao);

        assertThat(resumo.brinde()).isTrue();
        assertThat(resumo.creditoProximaCompra()).isEqualTo(new BigDecimal("30.00"));
    }

    @Test
    @DisplayName("Cartao de 4x a 12x cobra juros pela tabela Price")
    void cartaoComJuros() {
        ResumoCheckout seisVezes = calcular(umaRequisicao("ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));
        assertThat(seisVezes.valorParcela()).isEqualTo(new BigDecimal("77.00"));
        assertThat(seisVezes.totalFinal()).isEqualTo(new BigDecimal("462.00"));

        ResumoCheckout dozeVezes = calcular(umaRequisicao("RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE"));
        assertThat(dozeVezes.valorParcela()).isEqualTo(new BigDecimal("39.10"));
        assertThat(dozeVezes.totalFinal()).isEqualTo(new BigDecimal("469.20"));
        assertThat(dozeVezes.ajustePagamento()).isEqualTo(new BigDecimal("55.40"));
    }

    @Test
    @DisplayName("Cartao em 4x e o primeiro com juros")
    void cartaoQuatroVezes() {
        ResumoCheckout tresVezes = calcular(umaRequisicao("RETIRADA_LOJA", null, "CARTAO", 3, "BRONZE", "SUDESTE"));
        assertThat(tresVezes.ajustePagamento()).isEqualTo(new BigDecimal("0.00"));

        ResumoCheckout quatroVezes = calcular(umaRequisicao("RETIRADA_LOJA", null, "CARTAO", 4, "BRONZE", "SUDESTE"));
        assertThat(quatroVezes.ajustePagamento()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Parcelas ausentes contam como 1")
    void parcelasAusentes() {
        RequisicaoResumo requisicao = new RequisicaoResumo(itens(),
                "RETIRADA_LOJA", null, "CARTAO", null, "BRONZE", "SUDESTE");

        ResumoCheckout resumo = calculadora.calcular(requisicao);

        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualTo(resumo.totalFinal());
    }

    @Test
    @DisplayName("Uma modalidade nova entra sem mexer no calculo")
    void modalidadeNova() {
        CalculadoraResumo comDrone = new CalculadoraResumo(
                new Catalogo<>(List.of(new DroneDeBairro())),
                new Catalogo<>(List.of()),
                new Catalogo<>(List.of(new ClubeBronze())),
                new Catalogo<>(List.of(new PagamentoPix())));

        ResumoCheckout resumo = comDrone.calcular(new RequisicaoResumo(itens(),
                "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualTo(new BigDecimal("9.00"));
        assertThat(resumo.prazoEntregaDias()).isZero();
    }

    private ResumoCheckout calcular(RequisicaoResumo requisicao) {
        return calculadora.calcular(requisicao);
    }

    private RequisicaoResumo umaRequisicao(String modalidade, String cupom, String formaPagamento,
                                           Integer parcelas, String nivelClube, String regiao) {
        return new RequisicaoResumo(itens(), modalidade, cupom, formaPagamento, parcelas, nivelClube, regiao);
    }

    /** Os mesmos itens dos exemplos do financeiro: 409,70 em produtos e 1,8 kg. */
    private List<RequisicaoResumo.ItemRequisicao> itens() {
        return List.of(
                new RequisicaoResumo.ItemRequisicao("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new RequisicaoResumo.ItemRequisicao("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));
    }

    /** Modalidade de exemplo, so para mostrar o que uma transportadora nova precisa escrever. */
    private static class DroneDeBairro implements com.loja.checkout.entrega.ModalidadeEntrega {

        @Override
        public String codigo() {
            return "DRONE";
        }

        @Override
        public boolean atende(com.loja.checkout.dominio.Pedido pedido) {
            return true;
        }

        @Override
        public com.loja.checkout.entrega.Entrega calcular(com.loja.checkout.dominio.Pedido pedido) {
            return new com.loja.checkout.entrega.Entrega(new BigDecimal("9.00"), 0);
        }
    }
}
