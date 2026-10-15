package com.loja.checkout;

import static com.loja.checkout.Pedidos.CAMISETA;
import static com.loja.checkout.Pedidos.MEIA;
import static com.loja.checkout.Pedidos.TENIS;
import static com.loja.checkout.Pedidos.item;
import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.ItemRecebido;
import com.loja.checkout.dominio.PedidoRecebido;
import com.loja.checkout.dominio.ResumoCompra;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RegrasDeCalculoTest {

    @Autowired
    private CalculadoraResumo calculadora;

    @Nested
    @DisplayName("entrega")
    class Entrega {

        @ParameterizedTest(name = "{0}: frete {1} em {2} dias")
        @CsvSource({
                "ECONOMICA,    15.60, 7",
                "EXPRESSA,     33.10, 2",
                "RETIRADA_LOJA, 0.00, 1",
                "MOTOBOY,      18.00, 0",
        })
        @DisplayName("cobra e prometa o prazo conforme a modalidade")
        void cobraPorModalidade(String modalidade, String frete, int prazo) {
            ResumoCompra resumo = calculadora.calcular(new PedidoRecebido(
                    List.of(CAMISETA, TENIS), modalidade, null, "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.frete()).isEqualByComparingTo(frete);
            assertThat(resumo.prazoEntregaDias()).isEqualTo(prazo);
        }

        @Test
        @DisplayName("o peso do pedido soma peso vezes quantidade, sem arredondar")
        void somaPesoDosItens() {
            ResumoCompra resumo = calculadora.calcular(new PedidoRecebido(
                    List.of(item("Cinto", "30.00", 3, "0.125")), "ECONOMICA", null,
                    "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.frete()).isEqualByComparingTo("12.75");
        }

        @Test
        @DisplayName("motoboy leva exatamente 5 kg")
        void motoboyLevaCincoQuilos() {
            ResumoCompra resumo = calculadora.calcular(new PedidoRecebido(
                    List.of(item("Jaqueta", "100.00", 5, "1.00")), "MOTOBOY", null,
                    "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        }
    }

    @Nested
    @DisplayName("cupons")
    class Cupons {

        @Test
        @DisplayName("BEMVINDO10 desconta 10% dos produtos")
        void bemvindo10() {
            assertThat(comCupom("BEMVINDO10").descontoCupom()).isEqualByComparingTo("40.97");
        }

        @Test
        @DisplayName("MENOS50 vale a partir de R$ 300,00 em produtos")
        void menos50NoLimite() {
            ResumoCompra resumo = calculadora.calcular(new PedidoRecebido(
                    List.of(item("Mochila", "300.00", 1, "0.50")), "RETIRADA_LOJA", "MENOS50",
                    "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("FRETEGRATIS mostra o frete e desconta o mesmo valor")
        void freteGratis() {
            ResumoCompra resumo = comCupom("FRETEGRATIS");

            assertThat(resumo.frete()).isEqualByComparingTo("33.10");
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
        }

        @Test
        @DisplayName("FRETEGRATIS para quem e OURO nao desconta nada, ja nao havia frete")
        void freteGratisComOuro() {
            ResumoCompra resumo = calculadora.calcular(new PedidoRecebido(
                    List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "PIX", 1, "OURO", "SUDESTE"));

            assertThat(resumo.frete()).isEqualByComparingTo("0.00");
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("LEVE3PAGUE2 libera uma unidade a cada tres do mesmo item")
        void leve3Pague2() {
            ResumoCompra resumo = calculadora.calcular(new PedidoRecebido(
                    List.of(MEIA, CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2",
                    "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
        }

        @Test
        @DisplayName("sem cupom o desconto e zero")
        void semCupom() {
            assertThat(comCupom(null).descontoCupom()).isEqualByComparingTo("0.00");
        }

        private ResumoCompra comCupom(String cupom) {
            return calculadora.calcular(new PedidoRecebido(
                    List.of(CAMISETA, TENIS), "EXPRESSA", cupom, "PIX", 1, "BRONZE", "SUDESTE"));
        }
    }

    @Nested
    @DisplayName("clube da loja")
    class Clube {

        @ParameterizedTest(name = "{0} ganha {1} de credito")
        @CsvSource({"BRONZE, 0.00", "PRATA, 8.19", "OURO, 20.48"})
        @DisplayName("o credito e sobre os produtos, sem desconto e sem frete")
        void credito(String nivel, String credito) {
            ResumoCompra resumo = calculadora.calcular(new PedidoRecebido(
                    List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, nivel, "SUDESTE"));

            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(credito);
        }

        @ParameterizedTest(name = "produtos {0} -> brinde {1}")
        @CsvSource({"500.00, false", "500.01, true"})
        @DisplayName("OURO leva brinde acima de R$ 500,00 em produtos")
        void brindeDoOuro(String preco, boolean brinde) {
            ResumoCompra resumo = calculadora.calcular(new PedidoRecebido(
                    List.of(item("Vestido", preco, 1, "0.40")), "RETIRADA_LOJA", null,
                    "PIX", 1, "OURO", "SUDESTE"));

            assertThat(resumo.brinde()).isEqualTo(brinde);
        }

        @Test
        @DisplayName("quem nao e OURO nao leva brinde, mesmo acima de R$ 500,00")
        void semBrindeNosOutrosNiveis() {
            ResumoCompra resumo = calculadora.calcular(new PedidoRecebido(
                    List.of(item("Vestido", "900.00", 1, "0.40")), "RETIRADA_LOJA", null,
                    "PIX", 1, "PRATA", "SUDESTE"));

            assertThat(resumo.brinde()).isFalse();
        }
    }

    @Nested
    @DisplayName("seguro do envio")
    class Seguro {

        @ParameterizedTest(name = "{0} cobra {1}")
        @CsvSource({
                "SUDESTE,       4.10",
                "SUL,           4.10",
                "CENTRO_OESTE,  6.15",
                "NORTE,        10.24",
                "NORDESTE,      8.19",
        })
        @DisplayName("e a porcentagem da regiao sobre os produtos, sem desconto e sem frete")
        void porRegiao(String regiao, String seguro) {
            ResumoCompra resumo = calculadora.calcular(new PedidoRecebido(
                    List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", regiao));

            assertThat(resumo.seguro()).isEqualByComparingTo(seguro);
        }
    }

    @Nested
    @DisplayName("formas de pagamento")
    class Pagamento {

        private static final ItemRecebido PECA_UNICA = item("Casaco", "400.00", 1, "0.50");

        @Test
        @DisplayName("PIX desconta 5% do total do pedido")
        void pix() {
            ResumoCompra resumo = pagandoCom("PIX", 1);

            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.20");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("383.80");
            assertThat(resumo.valorParcela()).isEqualByComparingTo("383.80");
        }

        @Test
        @DisplayName("BOLETO soma a tarifa do banco")
        void boleto() {
            ResumoCompra resumo = pagandoCom("BOLETO", 1);

            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("407.49");
        }

        @ParameterizedTest(name = "CARTAO em {0}x nao cobra juros")
        @CsvSource({"1, 404.00", "2, 202.00", "3, 134.67"})
        @DisplayName("CARTAO em ate 3x fica no proprio total do pedido")
        void cartaoSemJuros(int parcelas, String valorParcela) {
            ResumoCompra resumo = pagandoCom("CARTAO", parcelas);

            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("404.00");
            assertThat(resumo.parcelas()).isEqualTo(parcelas);
            assertThat(resumo.valorParcela()).isEqualByComparingTo(valorParcela);
        }

        @Test
        @DisplayName("CARTAO acima de 3x cobra juros e o total final e a parcela vezes as parcelas")
        void cartaoComJuros() {
            ResumoCompra resumo = pagandoCom("CARTAO", 12);

            assertThat(resumo.valorParcela()).isEqualByComparingTo("38.18");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("458.16");
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("54.16");
        }

        @Test
        @DisplayName("sem informar as parcelas, e uma")
        void parcelasAusentes() {
            ResumoCompra resumo = calculadora.calcular(new PedidoRecebido(
                    List.of(PECA_UNICA), "RETIRADA_LOJA", null, "CARTAO", null, "BRONZE", "SUDESTE"));

            assertThat(resumo.parcelas()).isEqualTo(1);
            assertThat(resumo.valorParcela()).isEqualByComparingTo(resumo.totalFinal());
        }

        @Test
        @DisplayName("BOLETO vale no total de exatamente R$ 1.000,00")
        void boletoNoLimite() {
            ResumoCompra resumo = calculadora.calcular(new PedidoRecebido(
                    List.of(item("Bolsa", "990.10", 1, "0.50")), "RETIRADA_LOJA", null,
                    "BOLETO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.totalFinal()).isEqualByComparingTo("1003.49");
        }

        private ResumoCompra pagandoCom(String forma, int parcelas) {
            return calculadora.calcular(new PedidoRecebido(
                    List.of(PECA_UNICA), "RETIRADA_LOJA", null, forma, parcelas, "BRONZE", "SUDESTE"));
        }
    }
}
