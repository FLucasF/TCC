package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.clube.ClubeBronze;
import com.loja.checkout.clube.ClubeOuro;
import com.loja.checkout.clube.ClubePrata;
import com.loja.checkout.clube.NiveisClube;
import com.loja.checkout.cupom.CupomBemvindo10;
import com.loja.checkout.cupom.CupomFreteGratis;
import com.loja.checkout.cupom.CupomLeve3Pague2;
import com.loja.checkout.cupom.CupomMenos50;
import com.loja.checkout.cupom.Cupons;
import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.DadosCompra;
import com.loja.checkout.dominio.DadosCompra.DadosItem;
import com.loja.checkout.dominio.ResumoCompra;
import com.loja.checkout.entrega.EntregaEconomica;
import com.loja.checkout.entrega.EntregaExpressa;
import com.loja.checkout.entrega.EntregaMotoboy;
import com.loja.checkout.entrega.ModalidadesEntrega;
import com.loja.checkout.entrega.RetiradaLoja;
import com.loja.checkout.pagamento.FormasPagamento;
import com.loja.checkout.pagamento.PagamentoBoleto;
import com.loja.checkout.pagamento.PagamentoCartao;
import com.loja.checkout.pagamento.PagamentoPix;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo(
            new ModalidadesEntrega(List.of(
                    new EntregaEconomica(), new EntregaExpressa(), new RetiradaLoja(), new EntregaMotoboy())),
            new NiveisClube(List.of(new ClubeBronze(), new ClubePrata(), new ClubeOuro())),
            new Cupons(List.of(
                    new CupomBemvindo10(), new CupomMenos50(), new CupomFreteGratis(), new CupomLeve3Pague2())),
            new FormasPagamento(List.of(new PagamentoPix(), new PagamentoCartao(), new PagamentoBoleto())));

    private static final DadosItem CAMISETA = new DadosItem("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    private static final DadosItem TENIS = new DadosItem("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    private static final DadosItem FONE = new DadosItem("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
    private static final DadosItem MEIA = new DadosItem("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));

    private static DadosCompra compra(List<DadosItem> itens, String entrega, String cupom, String pagamento,
            Integer parcelas, String clube, String regiao) {
        return new DadosCompra(itens, entrega, cupom, pagamento, parcelas, clube, regiao);
    }

    private static DadosCompra compraPadrao() {
        return compra(List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
    }

    @Nested
    class ExemplosConferidosPeloFinanceiro {

        @Test
        void exemplo1_expressaComBemvindo10NoPixBronzeNorte() {
            ResumoCompra resumo = calculadora.calcular(
                    compra(List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

            assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
            assertThat(resumo.frete()).isEqualByComparingTo("33.10");
            assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
            assertThat(resumo.seguro()).isEqualByComparingTo("10.24");
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.60");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("391.47");
            assertThat(resumo.parcelas()).isEqualTo(1);
            assertThat(resumo.valorParcela()).isEqualByComparingTo("391.47");
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void exemplo2_economicaNoCartaoEm6xPrataCentroOeste() {
            ResumoCompra resumo = calculadora.calcular(
                    compra(List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

            assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
            assertThat(resumo.frete()).isEqualByComparingTo("15.60");
            assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
            assertThat(resumo.seguro()).isEqualByComparingTo("6.15");
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("30.55");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("462.00");
            assertThat(resumo.parcelas()).isEqualTo(6);
            assertThat(resumo.valorParcela()).isEqualByComparingTo("77.00");
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("8.19");
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void exemplo3_motoboyComMenos50NoBoletoBronzeNordeste() {
            ResumoCompra resumo = calculadora.calcular(
                    compra(List.of(FONE), "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));

            assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("399.80");
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
            assertThat(resumo.frete()).isEqualByComparingTo("18.00");
            assertThat(resumo.prazoEntregaDias()).isZero();
            assertThat(resumo.seguro()).isEqualByComparingTo("8.00");
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("379.29");
            assertThat(resumo.valorParcela()).isEqualByComparingTo("379.29");
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void exemplo4_retiradaComLeve3Pague2NoCartaoEm3xPrataSul() {
            ResumoCompra resumo = calculadora.calcular(
                    compra(List.of(MEIA, CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

            assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("299.10");
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
            assertThat(resumo.frete()).isEqualByComparingTo("0.00");
            assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
            assertThat(resumo.seguro()).isEqualByComparingTo("2.99");
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("262.29");
            assertThat(resumo.parcelas()).isEqualTo(3);
            assertThat(resumo.valorParcela()).isEqualByComparingTo("87.43");
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("5.98");
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void exemplo5_ouroNaoPagaFreteNoPixSudeste() {
            ResumoCompra resumo = calculadora.calcular(
                    compra(List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

            assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
            assertThat(resumo.frete()).isEqualByComparingTo("0.00");
            assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
            assertThat(resumo.seguro()).isEqualByComparingTo("4.10");
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.69");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("393.11");
            assertThat(resumo.valorParcela()).isEqualByComparingTo("393.11");
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void exemploDoAnexo_ouroComBemvindo10NoPixSudeste() {
            ResumoCompra resumo = calculadora.calcular(
                    compra(List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"));

            assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
            assertThat(resumo.frete()).isEqualByComparingTo("0.00");
            assertThat(resumo.seguro()).isEqualByComparingTo("4.10");
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-18.64");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("354.19");
            assertThat(resumo.valorParcela()).isEqualByComparingTo("354.19");
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
            assertThat(resumo.brinde()).isFalse();
        }
    }

    @Nested
    class Entrega {

        @Test
        void retiradaNaLojaNaoCobraFrete() {
            ResumoCompra resumo = calculadora.calcular(
                    compra(List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.frete()).isEqualByComparingTo("0.00");
            assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        }

        @Test
        void motoboyAtendePedidoDeExatamente5Kg() {
            DadosItem caixa = new DadosItem("Caixa", new BigDecimal("10.00"), 5, new BigDecimal("1.00"));

            ResumoCompra resumo = calculadora.calcular(
                    compra(List.of(caixa), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        }
    }

    @Nested
    class PromocoesComCupom {

        @Test
        void freteGratisDescontaExatamenteOValorDoFrete() {
            ResumoCompra resumo = calculadora.calcular(
                    compra(List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.frete()).isEqualByComparingTo("33.10");
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
            // 409,70 - 33,10 + 33,10 + 4,10
            assertThat(resumo.totalFinal()).isEqualByComparingTo("413.80");
        }

        @Test
        void freteGratisNaoDescontaNadaQuandoOuroJaNaoPagaFrete() {
            ResumoCompra resumo = calculadora.calcular(
                    compra(List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "OURO", "SUDESTE"));

            assertThat(resumo.frete()).isEqualByComparingTo("0.00");
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        }

        @Test
        void leve3pague2NaoDescontaQuandoNenhumItemChegaATresUnidades() {
            ResumoCompra resumo = calculadora.calcular(
                    compra(List.of(CAMISETA, TENIS), "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        }

        @Test
        void menos50ValeAPartirDeTrezentosReaisEmProdutos() {
            DadosItem vestido = new DadosItem("Vestido", new BigDecimal("150.00"), 2, new BigDecimal("0.40"));

            ResumoCompra resumo = calculadora.calcular(
                    compra(List.of(vestido), "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        }
    }

    @Nested
    class ClubeDaLoja {

        @Test
        void bronzeNaoGanhaNada() {
            ResumoCompra resumo = calculadora.calcular(
                    compra(List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
            assertThat(resumo.frete()).isEqualByComparingTo("33.10");
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void ouroGanhaBrindeAcimaDeQuinhentosReaisEmProdutos() {
            DadosItem jaqueta = new DadosItem("Jaqueta", new BigDecimal("300.00"), 2, new BigDecimal("0.80"));

            ResumoCompra resumo = calculadora.calcular(
                    compra(List.of(jaqueta), "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

            assertThat(resumo.brinde()).isTrue();
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("30.00");
        }

        @Test
        void ouroNaoGanhaBrindeEmQuinhentosReaisExatos() {
            DadosItem jaqueta = new DadosItem("Jaqueta", new BigDecimal("250.00"), 2, new BigDecimal("0.80"));

            ResumoCompra resumo = calculadora.calcular(
                    compra(List.of(jaqueta), "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

            assertThat(resumo.brinde()).isFalse();
        }
    }

    @Nested
    class Pagamento {

        @Test
        void cartaoSemJurosAteTresParcelas() {
            ResumoCompra resumo = calculadora.calcular(
                    compra(List.of(CAMISETA, TENIS), "RETIRADA_LOJA", null, "CARTAO", 3, "BRONZE", "SUDESTE"));

            // 409,70 + 0 + 4,10
            assertThat(resumo.totalFinal()).isEqualByComparingTo("413.80");
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
            assertThat(resumo.valorParcela()).isEqualByComparingTo("137.93");
        }

        @Test
        void cartaoEmDozeVezesCobraJuros() {
            ResumoCompra resumo = calculadora.calcular(
                    compra(List.of(CAMISETA, TENIS), "RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE"));

            assertThat(resumo.valorParcela()).isEqualByComparingTo("39.10");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("469.20");
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("55.40");
        }

        @Test
        void parcelasAusentesContamComoUma() {
            DadosCompra dados = compra(List.of(CAMISETA), "RETIRADA_LOJA", null, "CARTAO", null, "BRONZE", "SUDESTE");

            ResumoCompra resumo = calculadora.calcular(dados);

            assertThat(resumo.parcelas()).isEqualTo(1);
            assertThat(resumo.valorParcela()).isEqualByComparingTo(resumo.totalFinal());
        }

        @Test
        void boletoSomaATarifaDoBanco() {
            ResumoCompra resumo = calculadora.calcular(
                    compra(List.of(CAMISETA), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
        }
    }

    @Nested
    class PedidosRecusados {

        private void esperaErro(DadosCompra dados, CodigoErro codigo) {
            assertThatThrownBy(() -> calculadora.calcular(dados))
                    .isInstanceOf(CheckoutException.class)
                    .extracting(erro -> ((CheckoutException) erro).codigo())
                    .isEqualTo(codigo);
        }

        @Test
        void carrinhoVazio() {
            esperaErro(compra(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void carrinhoNulo() {
            esperaErro(compra(null, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void itemComQuantidadeZerada() {
            DadosItem invalido = new DadosItem("Camiseta", new BigDecimal("79.90"), 0, new BigDecimal("0.30"));
            esperaErro(compra(List.of(invalido), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void itemComPrecoNegativo() {
            DadosItem invalido = new DadosItem("Camiseta", new BigDecimal("-1.00"), 1, new BigDecimal("0.30"));
            esperaErro(compra(List.of(invalido), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void itemSemPeso() {
            DadosItem invalido = new DadosItem("Camiseta", new BigDecimal("79.90"), 1, null);
            esperaErro(compra(List.of(invalido), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void nivelDeClubeQueNaoExiste() {
            esperaErro(compra(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE"),
                    CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        @Test
        void nivelDeClubeNaoInformado() {
            esperaErro(compra(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, null, "SUDESTE"),
                    CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        @Test
        void regiaoQueNaoExiste() {
            esperaErro(compra(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE_LITORAL"),
                    CodigoErro.REGIAO_INVALIDA);
        }

        @Test
        void regiaoNaoInformada() {
            esperaErro(compra(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "BRONZE", null),
                    CodigoErro.REGIAO_INVALIDA);
        }

        @Test
        void modalidadeDeEntregaQueNaoExiste() {
            esperaErro(compra(List.of(CAMISETA), "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.MODALIDADE_INVALIDA);
        }

        @Test
        void modalidadeDeEntregaNaoInformada() {
            esperaErro(compra(List.of(CAMISETA), null, null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.MODALIDADE_INVALIDA);
        }

        @Test
        void motoboyAcimaDoPesoMaximo() {
            DadosItem caixa = new DadosItem("Caixa", new BigDecimal("10.00"), 6, new BigDecimal("1.00"));
            esperaErro(compra(List.of(caixa), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        @Test
        void cupomQueNaoExiste() {
            esperaErro(compra(List.of(CAMISETA), "EXPRESSA", "PROMOFAKE", "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.CUPOM_INVALIDO);
        }

        @Test
        void cupomEmLetrasMinusculasNaoExiste() {
            esperaErro(compra(List.of(CAMISETA), "EXPRESSA", "bemvindo10", "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.CUPOM_INVALIDO);
        }

        @Test
        void menos50AbaixoDoValorMinimo() {
            esperaErro(compra(List.of(CAMISETA), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        @Test
        void formaDePagamentoQueNaoExiste() {
            esperaErro(compra(List.of(CAMISETA), "EXPRESSA", null, "DINHEIRO", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        @Test
        void formaDePagamentoNaoInformada() {
            esperaErro(compra(List.of(CAMISETA), "EXPRESSA", null, null, 1, "BRONZE", "SUDESTE"),
                    CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        @Test
        void pixParcelado() {
            esperaErro(compra(List.of(CAMISETA), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE"),
                    CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void boletoParcelado() {
            esperaErro(compra(List.of(CAMISETA), "EXPRESSA", null, "BOLETO", 3, "BRONZE", "SUDESTE"),
                    CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void cartaoAcimaDeDozeParcelas() {
            esperaErro(compra(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE"),
                    CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void cartaoComParcelasZeradas() {
            esperaErro(compra(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 0, "BRONZE", "SUDESTE"),
                    CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void boletoAcimaDeMilReais() {
            DadosItem sofa = new DadosItem("Casaco", new BigDecimal("600.00"), 2, new BigDecimal("1.00"));
            esperaErro(compra(List.of(sofa), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        @Test
        void pedidoInvalidoVemAntesDeQualquerOutroProblema() {
            esperaErro(compra(List.of(), "DRONE", "PROMOFAKE", "DINHEIRO", 9, "DIAMANTE", "MARTE"),
                    CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void modalidadeIndisponivelVemAntesDoCupomInvalido() {
            DadosItem caixa = new DadosItem("Caixa", new BigDecimal("10.00"), 6, new BigDecimal("1.00"));
            esperaErro(compra(List.of(caixa), "MOTOBOY", "PROMOFAKE", "DINHEIRO", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        @Test
        void cupomNaoAplicavelVemAntesDaFormaDePagamentoInvalida() {
            esperaErro(compra(List.of(CAMISETA), "EXPRESSA", "MENOS50", "DINHEIRO", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        @Test
        void parcelamentoInvalidoVemAntesDaFormaDePagamentoIndisponivel() {
            DadosItem casaco = new DadosItem("Casaco", new BigDecimal("600.00"), 2, new BigDecimal("1.00"));
            esperaErro(compra(List.of(casaco), "RETIRADA_LOJA", null, "BOLETO", 2, "BRONZE", "SUDESTE"),
                    CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    @Nested
    class Arredondamento {

        @Test
        void arredondaMeioParaOPar() {
            // produtos 20,50 x 1 -> seguro do Sul (1%) = 0,205 -> 0,20
            DadosItem bone = new DadosItem("Bone", new BigDecimal("20.50"), 1, new BigDecimal("0.10"));

            ResumoCompra resumo = calculadora.calcular(
                    compra(List.of(bone), "RETIRADA_LOJA", null, "CARTAO", 1, "BRONZE", "SUL"));

            assertThat(resumo.seguro()).isEqualByComparingTo("0.20");
        }

        @Test
        void pesoFracionadoEntraNoFreteSemArredondar() {
            DadosCompra dados = compraPadrao();

            ResumoCompra resumo = calculadora.calcular(dados);

            // 1,80 kg: 25,00 + 4,50 x 1,8 = 33,10
            assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        }
    }
}
