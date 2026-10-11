package com.loja.checkout.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.dominio.Erro;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.RecusaPedido;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraResumoTest {

    private static final Item CAMISETA = new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    private static final Item TENIS = new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    private static final List<Item> CARRINHO = List.of(CAMISETA, TENIS);

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    @Test
    void exemplo1PixComCupomNoNorte() {
        ResumoCompra r = calculadora.calcular(pedido(CARRINHO, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"));

        assertThat(r.subtotalProdutos()).isEqualTo("409.70");
        assertThat(r.descontoCupom()).isEqualTo("40.97");
        assertThat(r.frete()).isEqualTo("33.10");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualTo("10.24");
        assertThat(r.ajustePagamento()).isEqualTo("-20.60");
        assertThat(r.totalFinal()).isEqualTo("391.47");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualTo("391.47");
        assertThat(r.creditoProximaCompra()).isEqualTo("0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2CartaoSeisVezesComJurosNoClublePrata() {
        ResumoCompra r = calculadora.calcular(pedido(CARRINHO, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(r.subtotalProdutos()).isEqualTo("409.70");
        assertThat(r.descontoCupom()).isEqualTo("0.00");
        assertThat(r.frete()).isEqualTo("15.60");
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertThat(r.seguro()).isEqualTo("6.15");
        assertThat(r.ajustePagamento()).isEqualTo("30.55");
        assertThat(r.totalFinal()).isEqualTo("462.00");
        assertThat(r.parcelas()).isEqualTo(6);
        assertThat(r.valorParcela()).isEqualTo("77.00");
        assertThat(r.creditoProximaCompra()).isEqualTo("8.19");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo3BoletoComMenos50NoNordeste() {
        List<Item> fones = List.of(new Item("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));

        ResumoCompra r = calculadora.calcular(pedido(fones, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        assertThat(r.subtotalProdutos()).isEqualTo("399.80");
        assertThat(r.descontoCupom()).isEqualTo("50.00");
        assertThat(r.frete()).isEqualTo("18.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertThat(r.seguro()).isEqualTo("8.00");
        assertThat(r.ajustePagamento()).isEqualTo("3.49");
        assertThat(r.totalFinal()).isEqualTo("379.29");
        assertThat(r.valorParcela()).isEqualTo("379.29");
        assertThat(r.creditoProximaCompra()).isEqualTo("0.00");
    }

    @Test
    void exemplo4LeveTresPagueDoisComTresParcelasSemJuros() {
        List<Item> carrinho = List.of(
                new Item("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));

        ResumoCompra r = calculadora.calcular(pedido(carrinho, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(r.subtotalProdutos()).isEqualTo("299.10");
        assertThat(r.descontoCupom()).isEqualTo("39.80");
        assertThat(r.frete()).isEqualTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertThat(r.seguro()).isEqualTo("2.99");
        assertThat(r.ajustePagamento()).isEqualTo("0.00");
        assertThat(r.totalFinal()).isEqualTo("262.29");
        assertThat(r.parcelas()).isEqualTo(3);
        assertThat(r.valorParcela()).isEqualTo("87.43");
        assertThat(r.creditoProximaCompra()).isEqualTo("5.98");
    }

    @Test
    void exemplo5OuroNaoPagaFreteEGanhaCredito() {
        ResumoCompra r = calculadora.calcular(pedido(CARRINHO, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"));

        assertThat(r.subtotalProdutos()).isEqualTo("409.70");
        assertThat(r.descontoCupom()).isEqualTo("0.00");
        assertThat(r.frete()).isEqualTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualTo("4.10");
        assertThat(r.ajustePagamento()).isEqualTo("-20.69");
        assertThat(r.totalFinal()).isEqualTo("393.11");
        assertThat(r.creditoProximaCompra()).isEqualTo("20.48");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void ouroComProdutosAcimaDe500GanhaBrinde() {
        List<Item> carrinho = List.of(new Item("Casaco", new BigDecimal("500.01"), 1, new BigDecimal("1")));

        ResumoCompra r = calculadora.calcular(pedido(carrinho, "ECONOMICA", null, "PIX", null, "OURO", "SUL"));

        assertThat(r.brinde()).isTrue();
    }

    @Test
    void ouroComExatamente500NaoGanhaBrinde() {
        List<Item> carrinho = List.of(new Item("Casaco", new BigDecimal("250.00"), 2, new BigDecimal("1")));

        ResumoCompra r = calculadora.calcular(pedido(carrinho, "ECONOMICA", null, "PIX", null, "OURO", "SUL"));

        assertThat(r.brinde()).isFalse();
    }

    @Test
    void freteGratisIgualaDescontoAoFreteJaZeradoParaOuro() {
        ResumoCompra bronze = calculadora.calcular(pedido(CARRINHO, "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE"));
        ResumoCompra ouro = calculadora.calcular(pedido(CARRINHO, "EXPRESSA", "FRETEGRATIS", "PIX", null, "OURO", "SUDESTE"));

        assertThat(bronze.frete()).isEqualTo("33.10");
        assertThat(bronze.descontoCupom()).isEqualTo("33.10");
        assertThat(ouro.frete()).isEqualTo("0.00");
        assertThat(ouro.descontoCupom()).isEqualTo("0.00");
    }

    @Test
    void motoboyAteCincoKgEhPermitidoAcimaNao() {
        List<Item> leve = List.of(new Item("Bolsa", new BigDecimal("10"), 1, new BigDecimal("5")));
        List<Item> pesado = List.of(new Item("Bolsa", new BigDecimal("10"), 1, new BigDecimal("5.01")));

        assertThat(calculadora.calcular(pedido(leve, "MOTOBOY", null, "PIX", null, "BRONZE", "SUL")).frete())
                .isEqualTo("18.00");
        assertErro(pedido(pesado, "MOTOBOY", null, "PIX", null, "BRONZE", "SUL"), Erro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void boletoAcimaDeMilNaoEhPermitido() {
        List<Item> caro = List.of(new Item("Casaco", new BigDecimal("985.00"), 1, new BigDecimal("1")));

        assertErro(pedido(caro, "ECONOMICA", null, "BOLETO", null, "BRONZE", "SUL"), Erro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void cartaoAteTresParcelasNaoTemJuros() {
        ResumoCompra tres = calculadora.calcular(pedido(CARRINHO, "ECONOMICA", null, "CARTAO", 3, "BRONZE", "SUL"));

        assertThat(tres.totalFinal()).isEqualTo("429.40");
        assertThat(tres.ajustePagamento()).isEqualTo("0.00");
        assertThat(tres.valorParcela()).isEqualTo("143.13");
    }

    @Test
    void cartaoAcimaDeTresParcelasTemJuros() {
        ResumoCompra quatro = calculadora.calcular(pedido(CARRINHO, "ECONOMICA", null, "CARTAO", 4, "BRONZE", "SUL"));

        assertThat(quatro.parcelas()).isEqualTo(4);
        assertThat(quatro.totalFinal()).isEqualTo(quatro.valorParcela().multiply(BigDecimal.valueOf(4)));
        assertThat(quatro.ajustePagamento()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void leve3pague2DescontaUmaCadaTresUnidadesDoMesmoItem() {
        List<Item> carrinho = List.of(new Item("Meia", new BigDecimal("10.00"), 3, new BigDecimal("0.10")));

        ResumoCompra r = calculadora.calcular(pedido(carrinho, "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", null, "BRONZE", "SUL"));

        assertThat(r.descontoCupom()).isEqualTo("10.00");
    }

    @Test
    void menos50AbaixoDe300NaoAplica() {
        List<Item> barato = List.of(new Item("Meia", new BigDecimal("99.99"), 3, new BigDecimal("0.10")));

        assertErro(pedido(barato, "ECONOMICA", "MENOS50", "PIX", null, "BRONZE", "SUL"), Erro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void cupomEmMinusculasEhInvalido() {
        assertErro(pedido(CARRINHO, "ECONOMICA", "bemvindo10", "PIX", null, "BRONZE", "SUL"), Erro.CUPOM_INVALIDO);
    }

    @Test
    void pixNaoParcela() {
        assertErro(pedido(CARRINHO, "ECONOMICA", null, "PIX", 2, "BRONZE", "SUL"), Erro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoAcimaDeDozeParcelasEhInvalido() {
        assertErro(pedido(CARRINHO, "ECONOMICA", null, "CARTAO", 13, "BRONZE", "SUL"), Erro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void carrinhoVazioOuItemSemPesoEhPedidoInvalido() {
        assertErro(pedido(List.of(), "ECONOMICA", null, "PIX", null, "BRONZE", "SUL"), Erro.PEDIDO_INVALIDO);
        assertErro(pedido(List.of(new Item("X", new BigDecimal("1"), 1, null)), "ECONOMICA", null, "PIX", null, "BRONZE", "SUL"),
                Erro.PEDIDO_INVALIDO);
    }

    @Test
    void erroDeItemTemPrioridadeSobreOsDemais() {
        assertErro(pedido(List.of(), "QUALQUER", "NAOEXISTE", "NAO", 9, "NADA", "NENHUMA"), Erro.PEDIDO_INVALIDO);
    }

    @Test
    void ordemDeErrosSegueAOrdemDoEnunciado() {
        assertErro(pedido(CARRINHO, "NAO", "NAOEXISTE", "NAO", 9, "NADA", "NENHUMA"), Erro.NIVEL_CLUBE_INVALIDO);
        assertErro(pedido(CARRINHO, "NAO", "NAOEXISTE", "NAO", 9, "BRONZE", "NENHUMA"), Erro.REGIAO_INVALIDA);
        assertErro(pedido(CARRINHO, "NAO", "NAOEXISTE", "NAO", 9, "BRONZE", "SUL"), Erro.MODALIDADE_INVALIDA);
        assertErro(pedido(CARRINHO, "ECONOMICA", "NAOEXISTE", "NAO", 9, "BRONZE", "SUL"), Erro.CUPOM_INVALIDO);
        assertErro(pedido(CARRINHO, "ECONOMICA", null, "NAO", 9, "BRONZE", "SUL"), Erro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void ouroNaoPagaFreteEmQualquerModalidade() {
        List<Item> leve = List.of(new Item("Bolsa", new BigDecimal("10"), 1, new BigDecimal("1")));

        assertThat(calculadora.calcular(pedido(leve, "MOTOBOY", null, "PIX", null, "OURO", "SUL")).frete())
                .isEqualTo("0.00");
        assertThat(calculadora.calcular(pedido(leve, "ECONOMICA", null, "PIX", null, "OURO", "SUL")).frete())
                .isEqualTo("0.00");
    }

    @Test
    void menos50NoLimiteDe300Aplica() {
        List<Item> justo = List.of(new Item("Casaco", new BigDecimal("150.00"), 2, new BigDecimal("1")));

        assertThat(calculadora.calcular(pedido(justo, "ECONOMICA", "MENOS50", "PIX", null, "BRONZE", "SUL"))
                .descontoCupom()).isEqualTo("50.00");
    }

    @Test
    void boletoNoLimiteDeMilEhPermitido() {
        List<Item> limite = List.of(new Item("Casaco", new BigDecimal("990.10"), 1, new BigDecimal("1")));

        ResumoCompra r = calculadora.calcular(pedido(limite, "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUL"));

        assertThat(r.totalFinal()).isEqualTo("1003.49");
    }

    @Test
    void cartaoDozeParcelasEhAceito() {
        ResumoCompra r = calculadora.calcular(pedido(CARRINHO, "ECONOMICA", null, "CARTAO", 12, "BRONZE", "SUL"));

        assertThat(r.parcelas()).isEqualTo(12);
        assertThat(r.totalFinal()).isEqualTo(r.valorParcela().multiply(BigDecimal.valueOf(12)));
    }

    @Test
    void itemComPrecoQuantidadeOuPesoNaoPositivosEhPedidoInvalido() {
        assertErro(pedido(List.of(new Item("X", BigDecimal.ZERO, 1, new BigDecimal("1"))), "ECONOMICA", null, "PIX", null, "BRONZE", "SUL"),
                Erro.PEDIDO_INVALIDO);
        assertErro(pedido(List.of(new Item("X", new BigDecimal("1"), 0, new BigDecimal("1"))), "ECONOMICA", null, "PIX", null, "BRONZE", "SUL"),
                Erro.PEDIDO_INVALIDO);
        assertErro(pedido(List.of(new Item("X", new BigDecimal("1"), 1, new BigDecimal("-1"))), "ECONOMICA", null, "PIX", null, "BRONZE", "SUL"),
                Erro.PEDIDO_INVALIDO);
    }

    @Test
    void modalidadeIndisponivelTemPrioridadeSobreCupomInvalido() {
        List<Item> pesado = List.of(new Item("Bolsa", new BigDecimal("10"), 1, new BigDecimal("6")));

        assertErro(pedido(pesado, "MOTOBOY", "NAOEXISTE", "NAO", 9, "BRONZE", "SUL"), Erro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomNaoAplicavelTemPrioridadeSobreFormaInvalida() {
        List<Item> barato = List.of(new Item("Meia", new BigDecimal("10"), 1, new BigDecimal("0.10")));

        assertErro(pedido(barato, "ECONOMICA", "MENOS50", "NAO", 9, "BRONZE", "SUL"), Erro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void parcelamentoInvalidoTemPrioridadeSobreFormaIndisponivel() {
        List<Item> caro = List.of(new Item("Casaco", new BigDecimal("990.10"), 1, new BigDecimal("1")));

        assertErro(pedido(caro, "RETIRADA_LOJA", null, "BOLETO", 2, "BRONZE", "SUL"), Erro.PARCELAMENTO_INVALIDO);
    }

    private static PedidoRequest pedido(List<Item> itens, String entrega, String cupom, String pagamento,
            Integer parcelas, String nivel, String regiao) {
        return new PedidoRequest(itens, entrega, cupom, pagamento, parcelas, nivel, regiao);
    }

    private void assertErro(PedidoRequest request, Erro esperado) {
        assertThatThrownBy(() -> calculadora.calcular(request))
                .isInstanceOfSatisfying(RecusaPedido.class, e -> assertThat(e.erro()).isEqualTo(esperado));
    }
}
