package br.com.loja.checkout;

import static br.com.loja.checkout.Cenarios.CAMISETA;
import static br.com.loja.checkout.Cenarios.FONE;
import static br.com.loja.checkout.Cenarios.MEIA;
import static br.com.loja.checkout.Cenarios.TENIS;
import static br.com.loja.checkout.Cenarios.pedido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.loja.checkout.api.ResumoResposta;
import br.com.loja.checkout.calculo.CalculadoraResumo;
import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.ErroCheckoutException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = Cenarios.calculadora();

    private static void valor(BigDecimal encontrado, String esperado) {
        assertThat(encontrado).isEqualByComparingTo(esperado);
        assertThat(encontrado.scale()).as("valor em centavos").isEqualTo(2);
    }

    @Nested
    @DisplayName("Exemplos conferidos pelo financeiro")
    class Exemplos {

        // Os exemplos 1 a 4 foram conferidos sem a parte do imposto (produtos, cupom,
        // frete e prazo). O ajuste de pagamento de cada um deles esta coberto em
        // PagamentoTest, sobre o total do pedido que estes numeros produzem.

        @Test
        @DisplayName("1: expressa + BEMVINDO10 + pix")
        void exemplo1() {
            ResumoResposta resumo = calculadora.calcular(pedido(CAMISETA, TENIS)
                    .entrega("EXPRESSA").cupom("BEMVINDO10").pagamento("PIX", 1).montar());

            valor(resumo.subtotalProdutos(), "409.70");
            valor(resumo.descontoCupom(), "40.97");
            valor(resumo.frete(), "33.10");
            assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        }

        @Test
        @DisplayName("2: economica sem cupom + cartao 6x")
        void exemplo2() {
            ResumoResposta resumo = calculadora.calcular(pedido(CAMISETA, TENIS)
                    .entrega("ECONOMICA").pagamento("CARTAO", 6).montar());

            valor(resumo.subtotalProdutos(), "409.70");
            valor(resumo.descontoCupom(), "0.00");
            valor(resumo.frete(), "15.60");
            assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
            assertThat(resumo.parcelas()).isEqualTo(6);
        }

        @Test
        @DisplayName("3: motoboy + MENOS50 + boleto")
        void exemplo3() {
            ResumoResposta resumo = calculadora.calcular(pedido(FONE)
                    .entrega("MOTOBOY").cupom("MENOS50").pagamento("BOLETO", 1).montar());

            valor(resumo.subtotalProdutos(), "399.80");
            valor(resumo.descontoCupom(), "50.00");
            valor(resumo.frete(), "18.00");
            assertThat(resumo.prazoEntregaDias()).isZero();
        }

        @Test
        @DisplayName("4: retirada + LEVE3PAGUE2 + cartao 3x")
        void exemplo4() {
            ResumoResposta resumo = calculadora.calcular(pedido(MEIA, CAMISETA)
                    .entrega("RETIRADA_LOJA").cupom("LEVE3PAGUE2").pagamento("CARTAO", 3).montar());

            valor(resumo.subtotalProdutos(), "299.10");
            valor(resumo.descontoCupom(), "39.80");
            valor(resumo.frete(), "0.00");
            assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        }

        @Test
        @DisplayName("5: expressa sem cupom + pix + clube OURO + sudeste")
        void exemplo5() {
            ResumoResposta resumo = calculadora.calcular(pedido(CAMISETA, TENIS)
                    .entrega("EXPRESSA").pagamento("PIX", 1).clube("OURO").regiao("SUDESTE")
                    .montar());

            valor(resumo.subtotalProdutos(), "409.70");
            valor(resumo.descontoCupom(), "0.00");
            valor(resumo.frete(), "0.00");
            assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
            valor(resumo.imposto(), "49.16");
            valor(resumo.ajustePagamento(), "-22.94");
            valor(resumo.totalFinal(), "435.92");
            assertThat(resumo.parcelas()).isEqualTo(1);
            valor(resumo.valorParcela(), "435.92");
            valor(resumo.creditoProximaCompra(), "20.48");
            assertThat(resumo.brinde()).isFalse();
        }
    }

    @Nested
    @DisplayName("Entrega")
    class Entrega {

        @Test
        void freteEconomicaSomaOPesoDoPedido() {
            // 2,4 kg -> 12,00 + 2,00 x 2,4
            ResumoResposta resumo = calculadora.calcular(
                    pedido(Cenarios.item("Jaqueta", "100.00", 2, "1.20"))
                            .entrega("ECONOMICA").montar());

            valor(resumo.frete(), "16.80");
        }

        @Test
        void motoboyNaoLevaAcimaDeCincoQuilos() {
            assertThatThrownBy(() -> calculadora.calcular(
                    pedido(Cenarios.item("Jaqueta", "100.00", 2, "2.60"))
                            .entrega("MOTOBOY").montar()))
                    .isInstanceOf(ErroCheckoutException.class)
                    .extracting(erro -> ((ErroCheckoutException) erro).codigo())
                    .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        @Test
        void motoboyLevaExatamenteCincoQuilos() {
            ResumoResposta resumo = calculadora.calcular(
                    pedido(Cenarios.item("Jaqueta", "100.00", 2, "2.50"))
                            .entrega("MOTOBOY").montar());

            valor(resumo.frete(), "18.00");
        }
    }

    @Nested
    @DisplayName("Cupons")
    class Cupons {

        @Test
        void freteGratisDescontaExatamenteOValorDoFrete() {
            ResumoResposta resumo = calculadora.calcular(pedido(CAMISETA, TENIS)
                    .entrega("EXPRESSA").cupom("FRETEGRATIS").montar());

            valor(resumo.frete(), "33.10");
            valor(resumo.descontoCupom(), "33.10");
        }

        @Test
        void freteGratisComOuroNaoDescontaNada() {
            ResumoResposta resumo = calculadora.calcular(pedido(CAMISETA, TENIS)
                    .entrega("EXPRESSA").cupom("FRETEGRATIS").clube("OURO").montar());

            valor(resumo.frete(), "0.00");
            valor(resumo.descontoCupom(), "0.00");
        }

        @Test
        void menos50PrecisaDeTrezentosReaisEmProdutos() {
            assertThatThrownBy(() -> calculadora.calcular(
                    pedido(Cenarios.item("Meia", "29.90", 10, "0.10"))
                            .cupom("MENOS50").montar()))
                    .isInstanceOf(ErroCheckoutException.class)
                    .extracting(erro -> ((ErroCheckoutException) erro).codigo())
                    .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        @Test
        void menos50ValeExatamenteEmTrezentosReais() {
            ResumoResposta resumo = calculadora.calcular(
                    pedido(Cenarios.item("Meia", "30.00", 10, "0.10")).cupom("MENOS50").montar());

            valor(resumo.descontoCupom(), "50.00");
        }

        @Test
        void leve3pague2LiberaUmaUnidadeACadaTres() {
            // 7 unidades -> 2 de graca
            ResumoResposta resumo = calculadora.calcular(
                    pedido(Cenarios.item("Meia", "19.90", 7, "0.10")).cupom("LEVE3PAGUE2").montar());

            valor(resumo.descontoCupom(), "39.80");
        }

        @Test
        void leve3pague2NaoValeSemTresUnidadesDoMesmoItem() {
            assertThatThrownBy(() -> calculadora.calcular(
                    pedido(CAMISETA, TENIS).cupom("LEVE3PAGUE2").montar()))
                    .isInstanceOf(ErroCheckoutException.class)
                    .extracting(erro -> ((ErroCheckoutException) erro).codigo())
                    .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        @Test
        void cupomDesconhecidoEhRecusado() {
            assertThatThrownBy(() -> calculadora.calcular(
                    pedido(CAMISETA).cupom("bemvindo10").montar()))
                    .isInstanceOf(ErroCheckoutException.class)
                    .extracting(erro -> ((ErroCheckoutException) erro).codigo())
                    .isEqualTo(CodigoErro.CUPOM_INVALIDO);
        }
    }

    @Nested
    @DisplayName("Clube da loja")
    class Clube {

        @Test
        void bronzeNaoGanhaNada() {
            ResumoResposta resumo = calculadora.calcular(
                    pedido(CAMISETA, TENIS).entrega("EXPRESSA").montar());

            valor(resumo.creditoProximaCompra(), "0.00");
            valor(resumo.frete(), "33.10");
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void prataGanhaDoisPorCentoDeCredito() {
            ResumoResposta resumo = calculadora.calcular(
                    pedido(CAMISETA, TENIS).clube("PRATA").montar());

            valor(resumo.creditoProximaCompra(), "8.19");
        }

        @Test
        void ouroGanhaBrindeAcimaDeQuinhentosReaisEmProdutos() {
            ResumoResposta resumo = calculadora.calcular(
                    pedido(Cenarios.item("Casaco", "300.00", 2, "0.80")).clube("OURO").montar());

            valor(resumo.creditoProximaCompra(), "30.00");
            assertThat(resumo.brinde()).isTrue();
        }

        @Test
        void ouroNaoGanhaBrindeExatamenteEmQuinhentosReais() {
            ResumoResposta resumo = calculadora.calcular(
                    pedido(Cenarios.item("Casaco", "250.00", 2, "0.80")).clube("OURO").montar());

            assertThat(resumo.brinde()).isFalse();
        }
    }

    @Nested
    @DisplayName("Imposto por regiao")
    class Imposto {

        @Test
        void aliquotaDeCadaRegiaoSobreOsProdutosComDesconto() {
            // produtos 409,70 - cupom 40,97 = 368,73
            assertImposto("SUDESTE", "44.25");
            assertImposto("SUL", "40.56");
            assertImposto("CENTRO_OESTE", "33.19");
            assertImposto("NORTE", "25.81");
            assertImposto("NORDESTE", "25.81");
        }

        private void assertImposto(String regiao, String esperado) {
            ResumoResposta resumo = calculadora.calcular(pedido(CAMISETA, TENIS)
                    .entrega("RETIRADA_LOJA").cupom("BEMVINDO10").regiao(regiao).montar());

            valor(resumo.imposto(), esperado);
        }

        @Test
        void impostoNaoIncideSobreOFrete() {
            ResumoResposta semFrete = calculadora.calcular(
                    pedido(CAMISETA, TENIS).entrega("RETIRADA_LOJA").montar());
            ResumoResposta comFrete = calculadora.calcular(
                    pedido(CAMISETA, TENIS).entrega("EXPRESSA").montar());

            assertThat(comFrete.imposto()).isEqualByComparingTo(semFrete.imposto());
        }
    }

    @Nested
    @DisplayName("Arredondamento")
    class Arredondamento {

        @Test
        void centavosVaoMeioParaOPar() {
            valor(Dinheiro.centavos(new BigDecimal("2.995")), "3.00");
            valor(Dinheiro.centavos(new BigDecimal("2.985")), "2.98");
        }

        @Test
        void totalFinalFechaComAsPartesDoResumo() {
            ResumoResposta resumo = calculadora.calcular(pedido(CAMISETA, TENIS)
                    .entrega("EXPRESSA").cupom("BEMVINDO10").pagamento("CARTAO", 2).montar());

            BigDecimal totalPedido = resumo.subtotalProdutos()
                    .subtract(resumo.descontoCupom())
                    .add(resumo.frete())
                    .add(resumo.imposto());

            assertThat(resumo.totalFinal())
                    .isEqualByComparingTo(totalPedido.add(resumo.ajustePagamento()));
        }
    }

    @Nested
    @DisplayName("Validacoes, na ordem combinada")
    class Validacoes {

        private void esperaErro(Cenarios.Requisicao requisicao, CodigoErro esperado) {
            assertThatThrownBy(() -> calculadora.calcular(requisicao.montar()))
                    .isInstanceOf(ErroCheckoutException.class)
                    .extracting(erro -> ((ErroCheckoutException) erro).codigo())
                    .isEqualTo(esperado);
        }

        @Test
        void carrinhoVazio() {
            assertThatThrownBy(() -> calculadora.calcular(
                    new br.com.loja.checkout.api.ResumoRequisicao(
                            List.of(), "RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isInstanceOf(ErroCheckoutException.class);
        }

        @Test
        void itemComValorOuPesoOuQuantidadeInvalidos() {
            esperaErro(pedido(Cenarios.item("Camiseta", "0.00", 1, "0.30")), CodigoErro.PEDIDO_INVALIDO);
            esperaErro(pedido(Cenarios.item("Camiseta", "79.90", 0, "0.30")), CodigoErro.PEDIDO_INVALIDO);
            esperaErro(pedido(Cenarios.item("Camiseta", "79.90", -1, "0.30")), CodigoErro.PEDIDO_INVALIDO);
            esperaErro(pedido(Cenarios.item("Camiseta", "79.90", 1, "0")), CodigoErro.PEDIDO_INVALIDO);
            esperaErro(pedido(new br.com.loja.checkout.api.ItemRequisicao("Camiseta", null, 1,
                    new BigDecimal("0.30"))), CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void carrinhoNuloOuComItemNulo() {
            esperaErro(new Cenarios.Requisicao(null, "RETIRADA_LOJA", null, "PIX", 1, "BRONZE",
                    "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
            esperaErro(new Cenarios.Requisicao(Collections.unmodifiableList(Arrays.asList(
                    (br.com.loja.checkout.api.ItemRequisicao) null)),
                    "RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void nivelDeClubeVemAntesDaRegiao() {
            esperaErro(pedido(CAMISETA).clube("DIAMANTE").regiao("MARTE"),
                    CodigoErro.NIVEL_CLUBE_INVALIDO);
            esperaErro(pedido(CAMISETA).clube(null), CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        @Test
        void regiaoVemAntesDaEntrega() {
            esperaErro(pedido(CAMISETA).regiao("MARTE").entrega("DRONE"), CodigoErro.REGIAO_INVALIDA);
            esperaErro(pedido(CAMISETA).regiao(null), CodigoErro.REGIAO_INVALIDA);
        }

        @Test
        void entregaInexistenteVemAntesDoCupom() {
            esperaErro(pedido(CAMISETA).entrega("DRONE").cupom("NAOEXISTE"),
                    CodigoErro.MODALIDADE_INVALIDA);
            esperaErro(pedido(CAMISETA).entrega(null), CodigoErro.MODALIDADE_INVALIDA);
        }

        @Test
        void entregaIndisponivelVemAntesDoCupom() {
            esperaErro(pedido(Cenarios.item("Jaqueta", "100.00", 2, "2.60"))
                    .entrega("MOTOBOY").cupom("NAOEXISTE"), CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        @Test
        void cupomVemAntesDaFormaDePagamento() {
            esperaErro(pedido(CAMISETA).cupom("NAOEXISTE").pagamento("CHEQUE", 1),
                    CodigoErro.CUPOM_INVALIDO);
        }

        @Test
        void formaDePagamentoInexistenteOuAusente() {
            esperaErro(pedido(CAMISETA).pagamento("CHEQUE", 1), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
            esperaErro(pedido(CAMISETA).pagamento(null, 1), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        @Test
        void parcelamentoForaDoPermitido() {
            esperaErro(pedido(CAMISETA).pagamento("PIX", 2), CodigoErro.PARCELAMENTO_INVALIDO);
            esperaErro(pedido(CAMISETA).pagamento("BOLETO", 3), CodigoErro.PARCELAMENTO_INVALIDO);
            esperaErro(pedido(CAMISETA).pagamento("CARTAO", 13), CodigoErro.PARCELAMENTO_INVALIDO);
            esperaErro(pedido(CAMISETA).pagamento("CARTAO", 0), CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void parcelasAusentesValemUm() {
            ResumoResposta resumo = calculadora.calcular(
                    pedido(CAMISETA).pagamento("CARTAO", null).montar());

            assertThat(resumo.parcelas()).isEqualTo(1);
        }

        @Test
        void boletoAcimaDeMilReaisVemDepoisDoParcelamento() {
            Cenarios.Requisicao caro = pedido(Cenarios.item("Casaco", "600.00", 2, "0.80"));

            esperaErro(caro.pagamento("BOLETO", 2), CodigoErro.PARCELAMENTO_INVALIDO);
            esperaErro(caro.pagamento("BOLETO", 1), CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        @Test
        void boletoValeExatamenteEmMilReais() {
            ResumoResposta resumo = calculadora.calcular(
                    pedido(Cenarios.item("Casaco", "500.00", 2, "0.80"))
                            .pagamento("BOLETO", 1).montar());

            valor(resumo.ajustePagamento(), "3.49");
        }

        @Test
        void limiteDoBoletoNaoContaOImposto() {
            // produtos 990,00 + frete 18,00 = 1.008,00 passa do teto mesmo sem imposto
            esperaErro(pedido(Cenarios.item("Casaco", "495.00", 2, "0.80"))
                    .entrega("MOTOBOY").pagamento("BOLETO", 1),
                    CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
    }
}
