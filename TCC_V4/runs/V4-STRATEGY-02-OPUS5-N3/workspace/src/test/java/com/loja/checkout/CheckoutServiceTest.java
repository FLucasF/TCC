package com.loja.checkout;

import static com.loja.checkout.ResumoFixture.CAMISETA;
import static com.loja.checkout.ResumoFixture.TENIS;
import static com.loja.checkout.ResumoFixture.item;
import static com.loja.checkout.ResumoFixture.pedido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.ResumoCompra;
import com.loja.checkout.web.CheckoutService;
import com.loja.checkout.web.ResumoRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CheckoutServiceTest {

    private final CheckoutService servico = new CheckoutService();

    @Nested
    class ExemplosConferidosPeloFinanceiro {

        @Test
        void expressaComBemvindo10NoPixBronzeNorte() {
            ResumoCompra resumo = servico.calcular(pedido()
                    .entrega("EXPRESSA").cupom("BEMVINDO10").pagamento("PIX")
                    .clube("BRONZE").regiao("NORTE").montar());

            assertThat(resumo).isEqualTo(new ResumoCompra(
                    valor("409.70"), valor("40.97"), valor("33.10"), 2, valor("10.24"),
                    valor("-20.60"), valor("391.47"), 1, valor("391.47"), valor("0.00"), false));
        }

        @Test
        void economicaNoCartaoEmSeisVezesPrataCentroOeste() {
            ResumoCompra resumo = servico.calcular(pedido()
                    .entrega("ECONOMICA").pagamento("CARTAO").parcelas(6)
                    .clube("PRATA").regiao("CENTRO_OESTE").montar());

            assertThat(resumo).isEqualTo(new ResumoCompra(
                    valor("409.70"), valor("0.00"), valor("15.60"), 7, valor("6.15"),
                    valor("30.55"), valor("462.00"), 6, valor("77.00"), valor("8.19"), false));
        }

        @Test
        void motoboyComMenos50NoBoletoBronzeNordeste() {
            ResumoCompra resumo = servico.calcular(pedido()
                    .itens(item("Fone", "199.90", 2, "0.25"))
                    .entrega("MOTOBOY").cupom("MENOS50").pagamento("BOLETO")
                    .clube("BRONZE").regiao("NORDESTE").montar());

            assertThat(resumo).isEqualTo(new ResumoCompra(
                    valor("399.80"), valor("50.00"), valor("18.00"), 0, valor("8.00"),
                    valor("3.49"), valor("379.29"), 1, valor("379.29"), valor("0.00"), false));
        }

        @Test
        void retiradaComLeve3Pague2NoCartaoEmTresVezesPrataSul() {
            ResumoCompra resumo = servico.calcular(pedido()
                    .itens(item("Meia", "19.90", 7, "0.10"), CAMISETA)
                    .entrega("RETIRADA_LOJA").cupom("LEVE3PAGUE2").pagamento("CARTAO").parcelas(3)
                    .clube("PRATA").regiao("SUL").montar());

            assertThat(resumo).isEqualTo(new ResumoCompra(
                    valor("299.10"), valor("39.80"), valor("0.00"), 1, valor("2.99"),
                    valor("0.00"), valor("262.29"), 3, valor("87.43"), valor("5.98"), false));
        }

        @Test
        void expressaNoPixOuroSudesteNaoPagaFrete() {
            ResumoCompra resumo = servico.calcular(pedido()
                    .entrega("EXPRESSA").pagamento("PIX").clube("OURO").regiao("SUDESTE").montar());

            assertThat(resumo).isEqualTo(new ResumoCompra(
                    valor("409.70"), valor("0.00"), valor("0.00"), 2, valor("4.10"),
                    valor("-20.69"), valor("393.11"), 1, valor("393.11"), valor("20.48"), false));
        }
    }

    @Nested
    class Entrega {

        @Test
        void motoboyNaoAtendeAcimaDeCincoQuilos() {
            assertErro(pedido().itens(item("Mala", "300.00", 1, "5.01")).entrega("MOTOBOY").montar(),
                    ErroCheckout.MODALIDADE_INDISPONIVEL);
        }

        @Test
        void motoboyAtendeExatamenteCincoQuilos() {
            ResumoCompra resumo = servico.calcular(
                    pedido().itens(item("Mala", "300.00", 1, "5.00")).entrega("MOTOBOY").montar());

            assertThat(resumo.frete()).isEqualTo(valor("18.00"));
            assertThat(resumo.prazoEntregaDias()).isZero();
        }
    }

    @Nested
    class Cupons {

        @Test
        void freteGratisDescontaExatamenteOFrete() {
            ResumoCompra resumo = servico.calcular(
                    pedido().entrega("ECONOMICA").cupom("FRETEGRATIS").montar());

            assertThat(resumo.frete()).isEqualTo(valor("15.60"));
            assertThat(resumo.descontoCupom()).isEqualTo(valor("15.60"));
        }

        @Test
        void freteGratisParaOuroNaoDescontaNada() {
            ResumoCompra resumo = servico.calcular(
                    pedido().entrega("ECONOMICA").cupom("FRETEGRATIS").clube("OURO").montar());

            assertThat(resumo.frete()).isEqualTo(valor("0.00"));
            assertThat(resumo.descontoCupom()).isEqualTo(valor("0.00"));
        }

        @Test
        void menos50ExigeTrezentosReaisEmProdutos() {
            assertErro(pedido().itens(item("Meia", "299.99", 1, "0.10")).cupom("MENOS50").montar(),
                    ErroCheckout.CUPOM_NAO_APLICAVEL);

            ResumoCompra resumo = servico.calcular(
                    pedido().itens(item("Meia", "300.00", 1, "0.10")).cupom("MENOS50").montar());
            assertThat(resumo.descontoCupom()).isEqualTo(valor("50.00"));
        }

        @Test
        void leve3Pague2ContaCadaItemSeparadamente() {
            ResumoCompra resumo = servico.calcular(pedido()
                    .itens(item("Meia", "10.00", 6, "0.10"), item("Bone", "30.00", 2, "0.20"))
                    .entrega("RETIRADA_LOJA").cupom("LEVE3PAGUE2").montar());

            assertThat(resumo.descontoCupom()).isEqualTo(valor("20.00"));
        }

        @Test
        void leve3Pague2SemNenhumTrioNaoDescontaNada() {
            ResumoCompra resumo = servico.calcular(pedido()
                    .itens(item("Meia", "19.90", 2, "0.10"))
                    .entrega("RETIRADA_LOJA").cupom("LEVE3PAGUE2").montar());

            assertThat(resumo.descontoCupom()).isEqualTo(valor("0.00"));
        }

        @Test
        void cupomDesconhecidoRecusaOPedido() {
            assertErro(pedido().cupom("bemvindo10").montar(), ErroCheckout.CUPOM_INVALIDO);
            assertErro(pedido().cupom("NATAL99").montar(), ErroCheckout.CUPOM_INVALIDO);
        }
    }

    @Nested
    class Clube {

        @Test
        void ouroGanhaBrindeAcimaDeQuinhentosReaisEmProdutos() {
            ResumoCompra resumo = servico.calcular(pedido()
                    .itens(item("Tenis", "250.01", 2, "1.20"))
                    .entrega("RETIRADA_LOJA").clube("OURO").montar());

            assertThat(resumo.brinde()).isTrue();
            assertThat(resumo.creditoProximaCompra()).isEqualTo(valor("25.00"));
        }

        @Test
        void ouroNaoGanhaBrindeExatamenteEmQuinhentosReais() {
            ResumoCompra resumo = servico.calcular(pedido()
                    .itens(item("Tenis", "250.00", 2, "1.20"))
                    .entrega("RETIRADA_LOJA").clube("OURO").montar());

            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void bronzeNaoGanhaCredito() {
            assertThat(servico.calcular(pedido().montar()).creditoProximaCompra()).isEqualTo(valor("0.00"));
        }
    }

    @Nested
    class Pagamento {

        @Test
        void cartaoAteTresVezesNaoTemJuros() {
            ResumoCompra resumo = servico.calcular(pedido()
                    .entrega("RETIRADA_LOJA").pagamento("CARTAO").parcelas(2).montar());

            assertThat(resumo.ajustePagamento()).isEqualTo(valor("0.00"));
            assertThat(resumo.totalFinal()).isEqualTo(valor("413.80"));
            assertThat(resumo.valorParcela()).isEqualTo(valor("206.90"));
        }

        @Test
        void cartaoAcimaDeTresVezesTemJurosEOTotalEhAParcelaVezesAsParcelas() {
            ResumoCompra resumo = servico.calcular(pedido()
                    .entrega("RETIRADA_LOJA").pagamento("CARTAO").parcelas(12).montar());

            assertThat(resumo.totalFinal())
                    .isEqualTo(resumo.valorParcela().multiply(BigDecimal.valueOf(12)));
            assertThat(resumo.ajustePagamento()).isPositive();
        }

        @Test
        void boletoNaoAtendeAcimaDeMilReais() {
            assertErro(pedido().itens(item("Tenis", "1000.00", 1, "1.20"))
                            .entrega("RETIRADA_LOJA").pagamento("BOLETO").montar(),
                    ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        @Test
        void boletoAtendeUmTotalDeExatamenteMilReais() {
            // 990,10 de produtos + 1% de seguro (9,90) = 1.000,00 de total do pedido
            ResumoCompra resumo = servico.calcular(pedido()
                    .itens(item("Tenis", "990.10", 1, "1.20"))
                    .entrega("RETIRADA_LOJA").pagamento("BOLETO").regiao("SUL").montar());

            assertThat(resumo.totalFinal()).isEqualTo(valor("1003.49"));
        }

        @Test
        void pixEBoletoSaoSempreAVista() {
            assertErro(pedido().pagamento("PIX").parcelas(2).montar(), ErroCheckout.PARCELAMENTO_INVALIDO);
            assertErro(pedido().pagamento("BOLETO").parcelas(3).montar(), ErroCheckout.PARCELAMENTO_INVALIDO);
        }

        @Test
        void cartaoVaiDeUmaATrezeVezes() {
            assertErro(pedido().pagamento("CARTAO").parcelas(13).montar(), ErroCheckout.PARCELAMENTO_INVALIDO);
            assertErro(pedido().pagamento("CARTAO").parcelas(0).montar(), ErroCheckout.PARCELAMENTO_INVALIDO);
        }

        @Test
        void parcelasAusentesValemUma() {
            assertThat(servico.calcular(pedido().pagamento("CARTAO").montar()).parcelas()).isEqualTo(1);
        }
    }

    @Nested
    class OrdemDasRecusas {

        @Test
        void pedidoVazioVemAntesDeTudo() {
            assertErro(pedido().semItens().clube("DIAMANTE").regiao("MARTE").entrega("DRONE")
                    .cupom("NATAL99").pagamento("CHEQUE").montar(), ErroCheckout.PEDIDO_INVALIDO);
        }

        @Test
        void itemComValorZeradoOuAusenteInvalidaOPedido() {
            assertErro(pedido().itens(item("Meia", "0.00", 1, "0.10")).montar(), ErroCheckout.PEDIDO_INVALIDO);
            assertErro(pedido().itens(item("Meia", "19.90", 0, "0.10")).montar(), ErroCheckout.PEDIDO_INVALIDO);
            assertErro(pedido().itens(item("Meia", "19.90", -1, "0.10")).montar(), ErroCheckout.PEDIDO_INVALIDO);
            assertErro(pedido().itens(item("Meia", "19.90", 1, "0")).montar(), ErroCheckout.PEDIDO_INVALIDO);
            assertErro(pedido().itens(item("Meia", null, 1, "0.10")).montar(), ErroCheckout.PEDIDO_INVALIDO);
            assertErro(pedido().itens(item("Meia", "19.90", null, "0.10")).montar(), ErroCheckout.PEDIDO_INVALIDO);
            assertErro(pedido().itens(item("Meia", "19.90", 1, null)).montar(), ErroCheckout.PEDIDO_INVALIDO);
            assertErro(pedido().itens((ResumoRequest.ItemRequest[]) null).montar(), ErroCheckout.PEDIDO_INVALIDO);
        }

        @Test
        void nivelDoClubeVemAntesDaRegiao() {
            assertErro(pedido().clube("DIAMANTE").regiao("MARTE").montar(), ErroCheckout.NIVEL_CLUBE_INVALIDO);
            assertErro(pedido().clube(null).montar(), ErroCheckout.NIVEL_CLUBE_INVALIDO);
        }

        @Test
        void regiaoVemAntesDaModalidade() {
            assertErro(pedido().regiao("MARTE").entrega("DRONE").montar(), ErroCheckout.REGIAO_INVALIDA);
            assertErro(pedido().regiao(null).montar(), ErroCheckout.REGIAO_INVALIDA);
        }

        @Test
        void modalidadeInexistenteVemAntesDaIndisponivel() {
            assertErro(pedido().entrega("DRONE").montar(), ErroCheckout.MODALIDADE_INVALIDA);
            assertErro(pedido().entrega(null).montar(), ErroCheckout.MODALIDADE_INVALIDA);
        }

        @Test
        void modalidadeIndisponivelVemAntesDoCupom() {
            assertErro(pedido().itens(item("Mala", "100.00", 1, "6.00"))
                    .entrega("MOTOBOY").cupom("NATAL99").montar(), ErroCheckout.MODALIDADE_INDISPONIVEL);
        }

        @Test
        void cupomInexistenteVemAntesDaFormaDePagamento() {
            assertErro(pedido().cupom("NATAL99").pagamento("CHEQUE").montar(), ErroCheckout.CUPOM_INVALIDO);
        }

        @Test
        void cupomNaoAplicavelVemAntesDaFormaDePagamento() {
            assertErro(pedido().itens(item("Meia", "19.90", 1, "0.10")).cupom("MENOS50")
                    .pagamento("CHEQUE").montar(), ErroCheckout.CUPOM_NAO_APLICAVEL);
        }

        @Test
        void formaDePagamentoInexistenteVemAntesDoParcelamento() {
            assertErro(pedido().pagamento("CHEQUE").parcelas(99).montar(),
                    ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
            assertErro(pedido().pagamento(null).montar(), ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
        }

        @Test
        void parcelamentoVemAntesDaFormaIndisponivel() {
            assertErro(pedido().itens(item("Tenis", "2000.00", 1, "1.20"))
                            .entrega("RETIRADA_LOJA").pagamento("BOLETO").parcelas(2).montar(),
                    ErroCheckout.PARCELAMENTO_INVALIDO);
        }

        @Test
        void pedidoAusenteInvalidaOPedido() {
            assertThatThrownBy(() -> servico.calcular(null))
                    .isInstanceOf(CheckoutException.class)
                    .extracting(e -> ((CheckoutException) e).erro())
                    .isEqualTo(ErroCheckout.PEDIDO_INVALIDO);
        }
    }

    @Test
    void arredondaCentavosMeioParaOPar() {
        // Seguro de 1% sobre 299,50 = 2,995 -> 3,00; sobre 298,50 = 2,985 -> 2,98.
        assertThat(servico.calcular(pedido().itens(item("Meia", "299.50", 1, "0.10"))
                .entrega("RETIRADA_LOJA").regiao("SUL").montar()).seguro()).isEqualTo(valor("3.00"));
        assertThat(servico.calcular(pedido().itens(item("Meia", "298.50", 1, "0.10"))
                .entrega("RETIRADA_LOJA").regiao("SUL").montar()).seguro()).isEqualTo(valor("2.98"));
    }

    @Test
    void arredondaAsLinhasDoCarrinhoAntesDeSomar() {
        // 1,005 -> 1,00 e 2,005 -> 2,00, ou seja 3,00 (e não 3,01 da soma crua)
        ResumoCompra resumo = servico.calcular(pedido()
                .itens(item("Botao", "1.005", 1, "0.01"), item("Linha", "2.005", 1, "0.01"))
                .entrega("RETIRADA_LOJA").montar());

        assertThat(resumo.subtotalProdutos()).isEqualTo(valor("3.00"));
    }

    @Test
    void oPesoDoPedidoSomaOsItensSemArredondar() {
        // 0,15 kg x 3 + 1,20 kg = 1,65 kg -> economica = 12,00 + 2,00 x 1,65 = 15,30
        ResumoCompra resumo = servico.calcular(pedido()
                .itens(item("Meia", "19.90", 3, "0.15"), TENIS).entrega("ECONOMICA").montar());

        assertThat(resumo.frete()).isEqualTo(valor("15.30"));
    }

    private void assertErro(ResumoRequest pedido, ErroCheckout esperado) {
        assertThatThrownBy(() -> servico.calcular(pedido))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).erro())
                .isEqualTo(esperado);
    }

    private static BigDecimal valor(String valor) {
        return new BigDecimal(valor);
    }
}
