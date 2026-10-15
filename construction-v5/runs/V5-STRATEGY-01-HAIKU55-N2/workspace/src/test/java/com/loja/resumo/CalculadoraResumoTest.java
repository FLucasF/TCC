package com.loja.resumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private static ResumoRequest.Item camiseta(int quantidade) {
        return new ResumoRequest.Item("Camiseta", new BigDecimal("79.90"), quantidade, new BigDecimal("0.30"));
    }

    private static ResumoRequest.Item tenis() {
        return new ResumoRequest.Item("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    }

    private static ResumoRequest pedido(List<ResumoRequest.Item> itens, String modalidade, String cupom,
                                        String forma, Integer parcelas, String nivel, String regiao) {
        return new ResumoRequest(itens, modalidade, cupom, forma, parcelas, nivel, regiao);
    }

    @Test
    void exemplo1PixComBemVindoNoBronze() {
        var resumo = calculadora.calcular(pedido(List.of(camiseta(2), tenis()),
                "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"));

        assertThat(resumo).isEqualTo(new ResumoCompra(
                new BigDecimal("409.70"), new BigDecimal("40.97"), new BigDecimal("33.10"), 2,
                new BigDecimal("10.24"), new BigDecimal("-20.60"), new BigDecimal("391.47"),
                1, new BigDecimal("391.47"), new BigDecimal("0.00"), false));
    }

    @Test
    void exemplo2CartaoSeisVezesComJurosNoPrata() {
        var resumo = calculadora.calcular(pedido(List.of(camiseta(2), tenis()),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(resumo).isEqualTo(new ResumoCompra(
                new BigDecimal("409.70"), new BigDecimal("0.00"), new BigDecimal("15.60"), 7,
                new BigDecimal("6.15"), new BigDecimal("30.55"), new BigDecimal("462.00"),
                6, new BigDecimal("77.00"), new BigDecimal("8.19"), false));
    }

    @Test
    void exemplo3BoletoComMenos50NoNordeste() {
        var fone = new ResumoRequest.Item("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
        var resumo = calculadora.calcular(pedido(List.of(fone),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        assertThat(resumo).isEqualTo(new ResumoCompra(
                new BigDecimal("399.80"), new BigDecimal("50.00"), new BigDecimal("18.00"), 0,
                new BigDecimal("8.00"), new BigDecimal("3.49"), new BigDecimal("379.29"),
                1, new BigDecimal("379.29"), new BigDecimal("0.00"), false));
    }

    @Test
    void exemplo4LeveTresPagueDoisNoPrataRetirada() {
        var meia = new ResumoRequest.Item("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
        var resumo = calculadora.calcular(pedido(List.of(meia, camiseta(2)),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(resumo).isEqualTo(new ResumoCompra(
                new BigDecimal("299.10"), new BigDecimal("39.80"), new BigDecimal("0.00"), 1,
                new BigDecimal("2.99"), new BigDecimal("0.00"), new BigDecimal("262.29"),
                3, new BigDecimal("87.43"), new BigDecimal("5.98"), false));
    }

    @Test
    void exemplo5OuroNaoPagaFretePeloPixEBrindeAcimaDe500() {
        var resumo = calculadora.calcular(pedido(List.of(camiseta(2), tenis()),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"));

        assertThat(resumo).isEqualTo(new ResumoCompra(
                new BigDecimal("409.70"), new BigDecimal("0.00"), new BigDecimal("0.00"), 2,
                new BigDecimal("4.10"), new BigDecimal("-20.69"), new BigDecimal("393.11"),
                1, new BigDecimal("393.11"), new BigDecimal("20.48"), false));
    }

    @Test
    void fretegratisDescontaOFreteEBrindeDoOuroAcimaDe500() {
        var resumo = calculadora.calcular(pedido(List.of(
                        new ResumoRequest.Item("Jaqueta", new BigDecimal("320.00"), 2, new BigDecimal("1.00"))),
                "ECONOMICA", "FRETEGRATIS", "PIX", null, "OURO", "SUDESTE"));

        assertThat(resumo.frete()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resumo.brinde()).isTrue();
    }

    @Test
    void fretegratisIgualAoFreteQuandoNaoHaClube() {
        var resumo = calculadora.calcular(pedido(List.of(camiseta(2), tenis()),
                "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualTo(new BigDecimal("33.10"));
        assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("33.10"));
    }

    @Test
    void carrinhoVazioEhPedidoInvalido() {
        assertErro(pedido(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUL"), "PEDIDO_INVALIDO");
    }

    @Test
    void itemComQuantidadeZeroEhPedidoInvalido() {
        assertErro(pedido(List.of(camiseta(0)), "EXPRESSA", null, "PIX", null, "BRONZE", "SUL"), "PEDIDO_INVALIDO");
    }

    @Test
    void motoboyAcimaDeCincoKgEhIndisponivel() {
        var pesado = new ResumoRequest.Item("Bota", new BigDecimal("300.00"), 1, new BigDecimal("5.01"));
        assertErro(pedido(List.of(pesado), "MOTOBOY", null, "PIX", null, "BRONZE", "SUL"), "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void menos50AbaixoDe300NaoEhAplicavel() {
        var barato = new ResumoRequest.Item("Meia", new BigDecimal("99.90"), 3, new BigDecimal("0.10"));
        assertErro(pedido(List.of(barato), "ECONOMICA", "MENOS50", "PIX", null, "BRONZE", "SUL"),
                "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void cupomInexistenteTemPrioridadeSobreFormaInvalida() {
        assertErro(pedido(List.of(camiseta(1)), "EXPRESSA", "XPTO", "BITCOIN", null, "BRONZE", "SUL"),
                "CUPOM_INVALIDO");
    }

    @Test
    void pixComDuasParcelasEhParcelamentoInvalido() {
        assertErro(pedido(List.of(camiseta(1)), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUL"),
                "PARCELAMENTO_INVALIDO");
    }

    @Test
    void cartaoComTrezeParcelasEhParcelamentoInvalido() {
        assertErro(pedido(List.of(camiseta(1)), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUL"),
                "PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDeMilEhIndisponivel() {
        var caro = new ResumoRequest.Item("Casaco", new BigDecimal("999.90"), 2, new BigDecimal("0.50"));
        assertErro(pedido(List.of(caro), "EXPRESSA", null, "BOLETO", null, "BRONZE", "SUL"),
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void nivelDesconhecidoEhNivelClubeInvalido() {
        assertErro(pedido(List.of(camiseta(1)), "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUL"),
                "NIVEL_CLUBE_INVALIDO");
    }

    private void assertErro(ResumoRequest pedido, String codigo) {
        assertThatThrownBy(() -> calculadora.calcular(pedido))
                .isInstanceOfSatisfying(ErroCompra.class, e -> assertThat(e.codigo().name()).isEqualTo(codigo));
    }
}
