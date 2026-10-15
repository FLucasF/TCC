package com.loja.checkout.calculo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private static final List<ItemPedido> CARRINHO = List.of(
            item("Camiseta", "79.90", 2, "0.30"),
            item("Tênis", "249.90", 1, "1.20"));

    @Test
    void exemplo1PixComCupomBemvindoNoNorte() {
        ResumoResponse resultado = calcular(new PedidoRequest(
                CARRINHO, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"));

        assertThat(resultado).isEqualTo(new ResumoResponse(
                d("409.70"), d("40.97"), d("33.10"), 2, d("10.24"), d("-20.60"), d("391.47"),
                1, d("391.47"), d("0.00"), false));
    }

    @Test
    void exemplo2CartaoSeisVezesComJurosClubePrata() {
        ResumoResponse resultado = calcular(new PedidoRequest(
                CARRINHO, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(resultado).isEqualTo(new ResumoResponse(
                d("409.70"), d("0.00"), d("15.60"), 7, d("6.15"), d("30.55"), d("462.00"),
                6, d("77.00"), d("8.19"), false));
    }

    @Test
    void exemplo3BoletoComMotoboyECupomMenos50() {
        List<ItemPedido> carrinho = List.of(item("Fone", "199.90", 2, "0.25"));

        ResumoResponse resultado = calcular(new PedidoRequest(
                carrinho, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        assertThat(resultado).isEqualTo(new ResumoResponse(
                d("399.80"), d("50.00"), d("18.00"), 0, d("8.00"), d("3.49"), d("379.29"),
                1, d("379.29"), d("0.00"), false));
    }

    @Test
    void exemplo4CartaoTresVezesSemJurosLeve3Pague2RetiradaNoSul() {
        List<ItemPedido> carrinho = List.of(
                item("Meia", "19.90", 7, "0.10"),
                item("Camiseta", "79.90", 2, "0.30"));

        ResumoResponse resultado = calcular(new PedidoRequest(
                carrinho, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(resultado).isEqualTo(new ResumoResponse(
                d("299.10"), d("39.80"), d("0.00"), 1, d("2.99"), d("0.00"), d("262.29"),
                3, d("87.43"), d("5.98"), false));
    }

    @Test
    void exemplo5PixClubeOuroSemFreteNoSudeste() {
        ResumoResponse resultado = calcular(new PedidoRequest(
                CARRINHO, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"));

        assertThat(resultado).isEqualTo(new ResumoResponse(
                d("409.70"), d("0.00"), d("0.00"), 2, d("4.10"), d("-20.69"), d("393.11"),
                1, d("393.11"), d("20.48"), false));
    }

    @Test
    void ouroGanhaBrindeAcimaDe500() {
        List<ItemPedido> carrinho = List.of(item("Casaco", "300.00", 2, "0.80"));

        ResumoResponse resultado = calcular(new PedidoRequest(
                carrinho, "ECONOMICA", null, "PIX", null, "OURO", "SUL"));

        assertThat(resultado.brinde()).isTrue();
        assertThat(resultado.frete()).isEqualTo(d("0.00"));
    }

    @Test
    void freteGratisDaMesmaCobrancaDeFreteComoDesconto() {
        ResumoResponse resultado = calcular(new PedidoRequest(
                CARRINHO, "ECONOMICA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE"));

        assertThat(resultado.frete()).isEqualTo(d("15.60"));
        assertThat(resultado.descontoCupom()).isEqualTo(d("15.60"));
    }

    @Test
    void motoboyAcimaDe5KgEhModalidadeIndisponivel() {
        List<ItemPedido> carrinho = List.of(item("Jaqueta", "100.00", 3, "2.00"));

        assertErro(() -> calcular(new PedidoRequest(
                carrinho, "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE")), Erro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void carrinhoVazioEhPedidoInvalido() {
        assertErro(() -> calcular(new PedidoRequest(
                List.of(), "ECONOMICA", null, "PIX", null, "BRONZE", "SUDESTE")), Erro.PEDIDO_INVALIDO);
    }

    @Test
    void quantidadeZeroEhPedidoInvalido() {
        List<ItemPedido> carrinho = List.of(item("Meia", "19.90", 0, "0.10"));

        assertErro(() -> calcular(new PedidoRequest(
                carrinho, "ECONOMICA", null, "PIX", null, "BRONZE", "SUDESTE")), Erro.PEDIDO_INVALIDO);
    }

    @Test
    void pedidoInvalidoTemPrioridadeSobreOutrosErros() {
        List<ItemPedido> carrinho = List.of(item("Meia", "19.90", 0, "0.10"));

        assertErro(() -> calcular(new PedidoRequest(
                carrinho, "NAO_EXISTE", "NAO_EXISTE", "NAO_EXISTE", null, "PLATINA", "MARTE")), Erro.PEDIDO_INVALIDO);
    }

    @Test
    void nivelDesconhecidoEhNivelClubeInvalido() {
        assertErro(() -> calcular(new PedidoRequest(
                CARRINHO, "ECONOMICA", null, "PIX", null, "PLATINA", "SUDESTE")), Erro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoAusenteEhRegiaoInvalida() {
        assertErro(() -> calcular(new PedidoRequest(
                CARRINHO, "ECONOMICA", null, "PIX", null, "BRONZE", null)), Erro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeAusenteEhModalidadeInvalida() {
        assertErro(() -> calcular(new PedidoRequest(
                CARRINHO, null, null, "PIX", null, "BRONZE", "SUDESTE")), Erro.MODALIDADE_INVALIDA);
    }

    @Test
    void cupomDesconhecidoEhCupomInvalido() {
        assertErro(() -> calcular(new PedidoRequest(
                CARRINHO, "ECONOMICA", "bemvindo10", "PIX", null, "BRONZE", "SUDESTE")), Erro.CUPOM_INVALIDO);
    }

    @Test
    void menos50AbaixoDe300EhCupomNaoAplicavel() {
        List<ItemPedido> carrinho = List.of(item("Meia", "19.90", 2, "0.10"));

        assertErro(() -> calcular(new PedidoRequest(
                carrinho, "ECONOMICA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE")), Erro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoAusenteEhFormaInvalida() {
        assertErro(() -> calcular(new PedidoRequest(
                CARRINHO, "ECONOMICA", null, null, null, "BRONZE", "SUDESTE")), Erro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void pixComDuasParcelasEhParcelamentoInvalido() {
        assertErro(() -> calcular(new PedidoRequest(
                CARRINHO, "ECONOMICA", null, "PIX", 2, "BRONZE", "SUDESTE")), Erro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoEmTrezeVezesEhParcelamentoInvalido() {
        assertErro(() -> calcular(new PedidoRequest(
                CARRINHO, "ECONOMICA", null, "CARTAO", 13, "BRONZE", "SUDESTE")), Erro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDeMilEhFormaIndisponivel() {
        List<ItemPedido> carrinho = List.of(item("Casaco", "500.00", 2, "1.00"));

        assertErro(() -> calcular(new PedidoRequest(
                carrinho, "ECONOMICA", null, "BOLETO", null, "BRONZE", "SUDESTE")), Erro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    private ResumoResponse calcular(PedidoRequest pedido) {
        return calculadora.calcular(pedido);
    }

    private static void assertErro(Runnable acao, Erro esperado) {
        assertThatThrownBy(acao::run)
                .isInstanceOfSatisfying(ResumoException.class, excecao -> assertThat(excecao.erro()).isEqualTo(esperado));
    }

    private static ItemPedido item(String nome, String preco, int quantidade, String peso) {
        return new ItemPedido(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private static BigDecimal d(String valor) {
        return new BigDecimal(valor);
    }
}
