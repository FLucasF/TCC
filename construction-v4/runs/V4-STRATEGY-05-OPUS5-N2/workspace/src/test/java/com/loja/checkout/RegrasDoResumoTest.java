package com.loja.checkout;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.Resumo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.loja.checkout.PedidoBuilder.camiseta;
import static com.loja.checkout.PedidoBuilder.fone;
import static com.loja.checkout.PedidoBuilder.item;
import static com.loja.checkout.PedidoBuilder.pedido;
import static com.loja.checkout.PedidoBuilder.tenis;
import static org.assertj.core.api.Assertions.assertThat;

class RegrasDoResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    @Nested
    @DisplayName("entrega")
    class Entrega {

        @ParameterizedTest(name = "{0}: frete {1}, prazo {2} dias")
        @CsvSource({
                "ECONOMICA, 15.60, 7",
                "EXPRESSA, 33.10, 2",
                "RETIRADA_LOJA, 0.00, 1",
                "MOTOBOY, 18.00, 0"
        })
        @DisplayName("cobra e prazo conforme a modalidade, para um pedido de 1,80 kg")
        void freteEPrazoPorModalidade(String modalidade, String frete, int prazo) {
            Resumo resumo = calculadora.calcular(pedido()
                    .itens(camiseta(2), tenis(1))
                    .entrega(modalidade)
                    .construir());

            assertThat(resumo.frete()).isEqualByComparingTo(frete);
            assertThat(resumo.prazoEntregaDias()).isEqualTo(prazo);
        }

        @Test
        @DisplayName("motoboy leva pedidos de exatamente 5 kg")
        void motoboyNoLimiteDePeso() {
            Resumo resumo = calculadora.calcular(pedido()
                    .itens(item("Mochila", "150.00", 2, "2.50"))
                    .entrega("MOTOBOY")
                    .construir());

            assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        }
    }

    @Nested
    @DisplayName("cupons")
    class Cupons {

        @Test
        @DisplayName("BEMVINDO10 desconta 10% dos produtos")
        void bemvindo10() {
            assertThat(descontoCom("BEMVINDO10")).isEqualByComparingTo("40.97");
        }

        @Test
        @DisplayName("MENOS50 desconta 50,00 a partir de 300,00 em produtos")
        void menos50() {
            assertThat(descontoCom("MENOS50")).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("FRETEGRATIS desconta o valor do frete, que continua aparecendo no resumo")
        void freteGratis() {
            Resumo resumo = calculadora.calcular(pedido()
                    .itens(camiseta(2), tenis(1))
                    .entrega("EXPRESSA")
                    .cupom("FRETEGRATIS")
                    .pagamento("CARTAO")
                    .construir());

            assertThat(resumo.frete()).isEqualByComparingTo("33.10");
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("413.80");
        }

        @Test
        @DisplayName("FRETEGRATIS nao desconta nada quando o OURO ja nao paga frete")
        void freteGratisParaQuemNaoPagaFrete() {
            Resumo resumo = calculadora.calcular(pedido()
                    .itens(camiseta(2), tenis(1))
                    .entrega("EXPRESSA")
                    .cupom("FRETEGRATIS")
                    .clube("OURO")
                    .construir());

            assertThat(resumo.frete()).isEqualByComparingTo("0.00");
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("LEVE3PAGUE2 libera uma unidade a cada 3 do mesmo item")
        void leve3Pague2() {
            Resumo resumo = calculadora.calcular(pedido()
                    .itens(fone(6), camiseta(2))
                    .entrega("RETIRADA_LOJA")
                    .cupom("LEVE3PAGUE2")
                    .construir());

            // 2 fones de graca, nenhuma camiseta
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("399.80");
        }

        @Test
        @DisplayName("sem cupom o desconto e zero")
        void semCupom() {
            assertThat(descontoCom(null)).isEqualByComparingTo("0.00");
        }

        private java.math.BigDecimal descontoCom(String cupom) {
            return calculadora.calcular(pedido()
                    .itens(camiseta(2), tenis(1))
                    .cupom(cupom)
                    .construir())
                    .descontoCupom();
        }
    }

    @Nested
    @DisplayName("clube da loja")
    class ClubeDaLoja {

        @ParameterizedTest(name = "{0} rende {1} de credito")
        @CsvSource({"BRONZE, 0.00", "PRATA, 8.19", "OURO, 20.48"})
        @DisplayName("credito para a proxima compra conforme o nivel")
        void creditoPorNivel(String nivel, String credito) {
            Resumo resumo = calculadora.calcular(pedido()
                    .itens(camiseta(2), tenis(1))
                    .clube(nivel)
                    .construir());

            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(credito);
        }

        @Test
        @DisplayName("OURO nao paga frete nunca")
        void ouroNaoPagaFrete() {
            Resumo resumo = calculadora.calcular(pedido()
                    .itens(camiseta(2), tenis(1))
                    .entrega("ECONOMICA")
                    .clube("OURO")
                    .construir());

            assertThat(resumo.frete()).isEqualByComparingTo("0.00");
            assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        }

        @Test
        @DisplayName("OURO ganha brinde acima de 500,00 em produtos")
        void ouroComBrinde() {
            Resumo resumo = calculadora.calcular(pedido()
                    .itens(tenis(1), camiseta(4))
                    .clube("OURO")
                    .construir());

            assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("569.50");
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("28.48");
            assertThat(resumo.brinde()).isTrue();
        }

        @Test
        @DisplayName("500,00 exatos em produtos nao dao brinde")
        void ouroSemBrindeNoLimite() {
            Resumo resumo = calculadora.calcular(pedido()
                    .itens(item("Jaqueta", "250.00", 2, "0.80"))
                    .clube("OURO")
                    .construir());

            assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("500.00");
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        @DisplayName("PRATA ganha credito mas paga frete")
        void prataPagaFrete() {
            Resumo resumo = calculadora.calcular(pedido()
                    .itens(camiseta(2), tenis(1))
                    .entrega("ECONOMICA")
                    .clube("PRATA")
                    .construir());

            assertThat(resumo.frete()).isEqualByComparingTo("15.60");
        }
    }

    @Nested
    @DisplayName("seguro do envio")
    class Seguro {

        @ParameterizedTest(name = "{0} cobra {1}")
        @CsvSource({
                "SUDESTE, 4.10",
                "SUL, 4.10",
                "CENTRO_OESTE, 6.15",
                "NORTE, 10.24",
                "NORDESTE, 8.19"
        })
        @DisplayName("percentual sobre os produtos conforme a regiao")
        void seguroPorRegiao(String regiao, String seguro) {
            Resumo resumo = calculadora.calcular(pedido()
                    .itens(camiseta(2), tenis(1))
                    .regiao(regiao)
                    .construir());

            assertThat(resumo.seguro()).isEqualByComparingTo(seguro);
        }

        @Test
        @DisplayName("nao considera desconto nem frete")
        void seguroIgnoraDescontoEFrete() {
            Resumo resumo = calculadora.calcular(pedido()
                    .itens(camiseta(2), tenis(1))
                    .entrega("EXPRESSA")
                    .cupom("BEMVINDO10")
                    .regiao("SUDESTE")
                    .construir());

            assertThat(resumo.seguro()).isEqualByComparingTo("4.10");
        }
    }

    @Nested
    @DisplayName("formas de pagamento")
    class FormasDePagamento {

        @Test
        @DisplayName("PIX desconta 5% do total do pedido")
        void pix() {
            Resumo resumo = calculadora.calcular(pedido()
                    .itens(camiseta(2), tenis(1))
                    .entrega("RETIRADA_LOJA")
                    .pagamento("PIX")
                    .construir());

            // total do pedido 409,70 + 0,00 + 4,10 = 413,80
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.69");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("393.11");
            assertThat(resumo.parcelas()).isEqualTo(1);
        }

        @Test
        @DisplayName("BOLETO soma a tarifa do banco")
        void boleto() {
            Resumo resumo = calculadora.calcular(pedido()
                    .itens(camiseta(2), tenis(1))
                    .entrega("RETIRADA_LOJA")
                    .pagamento("BOLETO")
                    .construir());

            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("417.29");
        }

        @ParameterizedTest(name = "CARTAO em {0}x: parcela de {1}")
        @CsvSource({"1, 413.80", "2, 206.90", "3, 137.93"})
        @DisplayName("ate 3x sem juros o total do pedido nao muda")
        void cartaoSemJuros(int parcelas, String valorParcela) {
            Resumo resumo = calculadora.calcular(pedido()
                    .itens(camiseta(2), tenis(1))
                    .entrega("RETIRADA_LOJA")
                    .pagamento("CARTAO")
                    .parcelas(parcelas)
                    .construir());

            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("413.80");
            assertThat(resumo.valorParcela()).isEqualByComparingTo(valorParcela);
        }

        @Test
        @DisplayName("de 4x a 12x cobra juros pela tabela Price")
        void cartaoComJuros() {
            Resumo resumo = calculadora.calcular(pedido()
                    .itens(fone(2))
                    .entrega("RETIRADA_LOJA")
                    .pagamento("CARTAO")
                    .parcelas(12)
                    .construir());

            // total do pedido 399,80 + 0,00 + 4,00 = 403,80
            assertThat(resumo.valorParcela()).isEqualByComparingTo("38.16");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("457.92");
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("54.12");
            assertThat(resumo.parcelas()).isEqualTo(12);
        }

        @Test
        @DisplayName("sem parcelas informadas, e uma vez")
        void parcelasAusentes() {
            Resumo resumo = calculadora.calcular(pedido()
                    .pagamento("CARTAO")
                    .parcelas(null)
                    .construir());

            assertThat(resumo.parcelas()).isEqualTo(1);
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        }
    }
}
