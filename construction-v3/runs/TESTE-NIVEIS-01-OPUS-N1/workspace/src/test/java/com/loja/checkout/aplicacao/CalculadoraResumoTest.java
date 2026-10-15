package com.loja.checkout.aplicacao;

import com.loja.checkout.aplicacao.PedidoCheckout.ItemPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CalculadoraResumoTest {

    private static final ItemPedido CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemPedido TENIS = item("Tenis", "249.90", 1, "1.20");

    @Autowired
    private CalculadoraResumo calculadora;

    @Test
    @DisplayName("Exemplo 1: EXPRESSA, BEMVINDO10, PIX, BRONZE, NORTE")
    void exemplo1() {
        ResumoCompra resumo = calculadora.calcular(new PedidoCheckout(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

        assertThat(resumo).isEqualTo(new ResumoCompra(
                reais("409.70"), reais("40.97"), reais("33.10"), 2, reais("10.24"),
                reais("-20.60"), reais("391.47"), 1, reais("391.47"), reais("0.00"), false));
    }

    @Test
    @DisplayName("Exemplo 2: ECONOMICA, sem cupom, CARTAO 6x, PRATA, CENTRO_OESTE")
    void exemplo2() {
        ResumoCompra resumo = calculadora.calcular(new PedidoCheckout(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(resumo).isEqualTo(new ResumoCompra(
                reais("409.70"), reais("0.00"), reais("15.60"), 7, reais("6.15"),
                reais("30.55"), reais("462.00"), 6, reais("77.00"), reais("8.19"), false));
    }

    @Test
    @DisplayName("Exemplo 3: MOTOBOY, MENOS50, BOLETO, BRONZE, NORDESTE")
    void exemplo3() {
        ResumoCompra resumo = calculadora.calcular(new PedidoCheckout(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));

        assertThat(resumo).isEqualTo(new ResumoCompra(
                reais("399.80"), reais("50.00"), reais("18.00"), 0, reais("8.00"),
                reais("3.49"), reais("379.29"), 1, reais("379.29"), reais("0.00"), false));
    }

    @Test
    @DisplayName("Exemplo 4: RETIRADA_LOJA, LEVE3PAGUE2, CARTAO 3x, PRATA, SUL")
    void exemplo4() {
        ResumoCompra resumo = calculadora.calcular(new PedidoCheckout(
                List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(resumo).isEqualTo(new ResumoCompra(
                reais("299.10"), reais("39.80"), reais("0.00"), 1, reais("2.99"),
                reais("0.00"), reais("262.29"), 3, reais("87.43"), reais("5.98"), false));
    }

    @Test
    @DisplayName("Exemplo 5: EXPRESSA, sem cupom, PIX, OURO, SUDESTE")
    void exemplo5() {
        ResumoCompra resumo = calculadora.calcular(new PedidoCheckout(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"));

        assertThat(resumo).isEqualTo(new ResumoCompra(
                reais("409.70"), reais("0.00"), reais("0.00"), 2, reais("4.10"),
                reais("-20.69"), reais("393.11"), 1, reais("393.11"), reais("20.48"), false));
    }

    @Test
    @DisplayName("Cupom FRETEGRATIS zera o frete no desconto, e o frete continua aparecendo")
    void freteGratis() {
        ResumoCompra resumo = calculadora.calcular(new PedidoCheckout(
                List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualTo(reais("33.10"));
        assertThat(resumo.descontoCupom()).isEqualTo(reais("33.10"));
        assertThat(resumo.totalFinal()).isEqualTo(reais("393.11"));
    }

    @Test
    @DisplayName("OURO nao paga frete, entao FRETEGRATIS nao desconta nada")
    void freteGratisComOuro() {
        ResumoCompra resumo = calculadora.calcular(new PedidoCheckout(
                List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.frete()).isEqualTo(reais("0.00"));
        assertThat(resumo.descontoCupom()).isEqualTo(reais("0.00"));
    }

    @Test
    @DisplayName("OURO acima de R$ 500,00 em produtos leva brinde")
    void brindeDoOuro() {
        ResumoCompra resumo = calculadora.calcular(new PedidoCheckout(
                List.of(item("Tenis", "249.90", 3, "1.20")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualTo(reais("749.70"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(reais("37.48"));
        assertThat(resumo.brinde()).isTrue();
    }

    @Test
    @DisplayName("Carrinho vazio ou item sem dado valido e pedido invalido")
    void pedidoInvalido() {
        assertThat(erroDe(new PedidoCheckout(
                List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(new PedidoCheckout(
                null, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(new PedidoCheckout(
                List.of(item("Camiseta", "79.90", 0, "0.30")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(new PedidoCheckout(
                List.of(new ItemPedido("Camiseta", null, 1, new BigDecimal("0.30"))),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(new PedidoCheckout(
                List.of(item("Camiseta", "79.90", 1, "-0.30")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    @DisplayName("Cada dado desconhecido tem seu codigo, conferido na ordem do enunciado")
    void dadosDesconhecidos() {
        assertThat(erroDe(new PedidoCheckout(
                List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE")))
                .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
        assertThat(erroDe(new PedidoCheckout(
                List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, null, "SUDESTE")))
                .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
        assertThat(erroDe(new PedidoCheckout(
                List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "BRONZE", "EUROPA")))
                .isEqualTo(CodigoErro.REGIAO_INVALIDA);
        assertThat(erroDe(new PedidoCheckout(
                List.of(CAMISETA), "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
        assertThat(erroDe(new PedidoCheckout(
                List.of(CAMISETA), "EXPRESSA", "NATAL", "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
        assertThat(erroDe(new PedidoCheckout(
                List.of(CAMISETA), "EXPRESSA", "bemvindo10", "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
        assertThat(erroDe(new PedidoCheckout(
                List.of(CAMISETA), "EXPRESSA", null, "CRIPTO", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    @DisplayName("Motoboy nao leva pedido acima de 5 kg")
    void motoboyAcimaDoPeso() {
        assertThat(erroDe(new PedidoCheckout(
                List.of(item("Tenis", "249.90", 5, "1.20")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    @DisplayName("MENOS50 abaixo de R$ 300,00 em produtos nao se aplica")
    void menos50AbaixoDoMinimo() {
        assertThat(erroDe(new PedidoCheckout(
                List.of(CAMISETA), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    @DisplayName("Pix e boleto so a vista, cartao de 1 a 12 vezes")
    void parcelamentoInvalido() {
        assertThat(erroDe(new PedidoCheckout(
                List.of(CAMISETA), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(new PedidoCheckout(
                List.of(CAMISETA), "EXPRESSA", null, "BOLETO", 3, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(new PedidoCheckout(
                List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(new PedidoCheckout(
                List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 0, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    @DisplayName("Boleto nao atende total acima de R$ 1.000,00")
    void boletoAcimaDoLimite() {
        assertThat(erroDe(new PedidoCheckout(
                List.of(item("Tenis", "249.90", 4, "1.20")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    @DisplayName("Cartao em 12x cobra juros de 1,99% ao mes pela tabela Price")
    void cartaoComJuros() {
        ResumoCompra resumo = calculadora.calcular(new PedidoCheckout(
                List.of(item("Tenis", "100.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE"));

        assertThat(resumo.totalFinal()).isEqualTo(reais("114.48"));
        assertThat(resumo.valorParcela()).isEqualTo(reais("9.54"));
        assertThat(resumo.ajustePagamento()).isEqualTo(reais("13.48"));
    }

    private CodigoErro erroDe(PedidoCheckout pedido) {
        assertThatThrownBy(() -> calculadora.calcular(pedido))
                .isInstanceOf(PedidoRecusadoException.class);
        try {
            calculadora.calcular(pedido);
            throw new AssertionError("era esperada a recusa do pedido");
        } catch (PedidoRecusadoException excecao) {
            return excecao.codigo();
        }
    }

    private static ItemPedido item(String nome, String preco, int quantidade, String peso) {
        return new ItemPedido(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private static BigDecimal reais(String valor) {
        return new BigDecimal(valor).setScale(2);
    }
}
