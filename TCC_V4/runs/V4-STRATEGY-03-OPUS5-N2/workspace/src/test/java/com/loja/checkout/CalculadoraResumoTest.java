package com.loja.checkout;

import static com.loja.checkout.PedidoDeTeste.CAMISETA;
import static com.loja.checkout.PedidoDeTeste.FONE;
import static com.loja.checkout.PedidoDeTeste.MEIA;
import static com.loja.checkout.PedidoDeTeste.TENIS;
import static com.loja.checkout.PedidoDeTeste.item;
import static com.loja.checkout.PedidoDeTeste.pedido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.api.CodigoErro;
import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.PedidoRecusadoException;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.servico.CalculadoraResumo;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Resumo da compra")
class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private static ResumoResponse resumo(String subtotal, String cupom, String frete, int prazo,
            String seguro, String ajuste, String total, int parcelas, String parcela,
            String credito, boolean brinde) {
        return new ResumoResponse(v(subtotal), v(cupom), v(frete), prazo, v(seguro), v(ajuste),
                v(total), parcelas, v(parcela), v(credito), brinde);
    }

    private static BigDecimal v(String valor) {
        return new BigDecimal(valor);
    }

    @Nested
    @DisplayName("Exemplos conferidos pelo financeiro")
    class Exemplos {

        @Test
        void exemplo1_expressaComBemvindo10NoPixBronzeNorte() {
            ResumoRequest pedido = pedido(List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10",
                    "PIX", 1, "BRONZE", "NORTE");

            assertThat(calculadora.calcular(pedido)).isEqualTo(resumo("409.70", "40.97", "33.10", 2,
                    "10.24", "-20.60", "391.47", 1, "391.47", "0.00", false));
        }

        @Test
        void exemplo2_economicaNoCartaoEm6vezesPrataCentroOeste() {
            ResumoRequest pedido = pedido(List.of(CAMISETA, TENIS), "ECONOMICA", null,
                    "CARTAO", 6, "PRATA", "CENTRO_OESTE");

            assertThat(calculadora.calcular(pedido)).isEqualTo(resumo("409.70", "0.00", "15.60", 7,
                    "6.15", "30.55", "462.00", 6, "77.00", "8.19", false));
        }

        @Test
        void exemplo3_motoboyComMenos50NoBoletoBronzeNordeste() {
            ResumoRequest pedido = pedido(List.of(FONE), "MOTOBOY", "MENOS50",
                    "BOLETO", 1, "BRONZE", "NORDESTE");

            assertThat(calculadora.calcular(pedido)).isEqualTo(resumo("399.80", "50.00", "18.00", 0,
                    "8.00", "3.49", "379.29", 1, "379.29", "0.00", false));
        }

        @Test
        void exemplo4_retiradaComLeve3Pague2NoCartaoEm3vezesPrataSul() {
            ResumoRequest pedido = pedido(List.of(MEIA, CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2",
                    "CARTAO", 3, "PRATA", "SUL");

            assertThat(calculadora.calcular(pedido)).isEqualTo(resumo("299.10", "39.80", "0.00", 1,
                    "2.99", "0.00", "262.29", 3, "87.43", "5.98", false));
        }

        @Test
        void exemplo5_expressaSemCupomNoPixOuroSudeste() {
            ResumoRequest pedido = pedido(List.of(CAMISETA, TENIS), "EXPRESSA", null,
                    "PIX", 1, "OURO", "SUDESTE");

            assertThat(calculadora.calcular(pedido)).isEqualTo(resumo("409.70", "0.00", "0.00", 2,
                    "4.10", "-20.69", "393.11", 1, "393.11", "20.48", false));
        }

        @Test
        void exemploDoAnexo_ouroComBemvindo10NoPixSudeste() {
            ResumoRequest pedido = pedido(List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10",
                    "PIX", 1, "OURO", "SUDESTE");

            assertThat(calculadora.calcular(pedido)).isEqualTo(resumo("409.70", "40.97", "0.00", 2,
                    "4.10", "-18.64", "354.19", 1, "354.19", "20.48", false));
        }
    }

    @Nested
    @DisplayName("Entrega")
    class Entrega {

        @Test
        void freteEconomicaSomaDozeReaisMaisDoisPorQuilo() {
            ResumoResponse resposta = calcularCom(List.of(CAMISETA, TENIS), "ECONOMICA", null, "PIX", 1,
                    "BRONZE", "SUDESTE");

            assertThat(resposta.frete()).isEqualTo(v("15.60"));
            assertThat(resposta.prazoEntregaDias()).isEqualTo(7);
        }

        @Test
        void retiradaNaLojaNaoTemFreteEEntregaNoDiaSeguinte() {
            ResumoResponse resposta = calcularCom(List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", 1,
                    "BRONZE", "SUDESTE");

            assertThat(resposta.frete()).isEqualTo(v("0.00"));
            assertThat(resposta.prazoEntregaDias()).isEqualTo(1);
        }

        @Test
        void motoboyCobraValorFixoEEntregaNoMesmoDia() {
            ResumoResponse resposta = calcularCom(List.of(CAMISETA, TENIS), "MOTOBOY", null, "PIX", 1,
                    "BRONZE", "SUDESTE");

            assertThat(resposta.frete()).isEqualTo(v("18.00"));
            assertThat(resposta.prazoEntregaDias()).isZero();
        }

        @Test
        void motoboyAtendePedidoDeExatamenteCincoQuilos() {
            ItemRequest caixa = item("Caixa", "100.00", 5, "1.00");

            assertThat(calcularCom(List.of(caixa), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE").frete())
                    .isEqualTo(v("18.00"));
        }

        @Test
        void motoboyNaoAtendeAcimaDeCincoQuilos() {
            ItemRequest caixa = item("Caixa", "100.00", 6, "1.00");

            recusa(pedido(List.of(caixa), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.MODALIDADE_INDISPONIVEL);
        }
    }

    @Nested
    @DisplayName("Cupons")
    class Cupons {

        @Test
        void menos50ValeApartirDeTrezentosEmProdutos() {
            ItemRequest exato = item("Jaqueta", "300.00", 1, "1.00");

            assertThat(calcularCom(List.of(exato), "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE")
                    .descontoCupom()).isEqualTo(v("50.00"));
        }

        @Test
        void menos50NaoValeAbaixoDeTrezentosEmProdutos() {
            ItemRequest barato = item("Bone", "299.99", 1, "0.20");

            recusa(pedido(List.of(barato), "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        @Test
        void freteGratisDescontaExatamenteOValorDoFrete() {
            ResumoResponse resposta = calcularCom(List.of(CAMISETA, TENIS), "ECONOMICA", "FRETEGRATIS",
                    "PIX", 1, "BRONZE", "SUDESTE");

            assertThat(resposta.frete()).isEqualTo(v("15.60"));
            assertThat(resposta.descontoCupom()).isEqualTo(v("15.60"));
            assertThat(resposta.totalFinal()).isEqualTo(v("393.11"));
        }

        @Test
        void freteGratisNaoDescontaNadaQuandoOuroJaNaoPagaFrete() {
            ResumoResponse resposta = calcularCom(List.of(CAMISETA, TENIS), "ECONOMICA", "FRETEGRATIS",
                    "PIX", 1, "OURO", "SUDESTE");

            assertThat(resposta.frete()).isEqualTo(v("0.00"));
            assertThat(resposta.descontoCupom()).isEqualTo(v("0.00"));
        }

        @Test
        void leve3pague2DaUmaUnidadeGratisACadaTresDoMesmoItem() {
            ItemRequest seisFones = item("Fone", "199.90", 6, "0.25");

            assertThat(calcularCom(List.of(seisFones), "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", 1,
                    "BRONZE", "SUDESTE").descontoCupom()).isEqualTo(v("399.80"));
        }

        @Test
        void cupomQueNaoExisteERecusado() {
            recusa(pedido(List.of(CAMISETA), "RETIRADA_LOJA", "PROMO999", "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.CUPOM_INVALIDO);
        }

        @Test
        void cupomForaDeMaiusculasERecusado() {
            recusa(pedido(List.of(CAMISETA), "RETIRADA_LOJA", "bemvindo10", "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.CUPOM_INVALIDO);
        }
    }

    @Nested
    @DisplayName("Clube da loja")
    class Clube {

        @Test
        void bronzeNaoGanhaNada() {
            ResumoResponse resposta = calcularCom(List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1,
                    "BRONZE", "SUDESTE");

            assertThat(resposta.creditoProximaCompra()).isEqualTo(v("0.00"));
            assertThat(resposta.frete()).isEqualTo(v("33.10"));
            assertThat(resposta.brinde()).isFalse();
        }

        @Test
        void ouroGanhaBrindeAcimaDeQuinhentosEmProdutos() {
            ItemRequest casaco = item("Casaco", "250.01", 2, "1.00");

            ResumoResponse resposta = calcularCom(List.of(casaco), "RETIRADA_LOJA", null, "PIX", 1,
                    "OURO", "SUDESTE");

            assertThat(resposta.brinde()).isTrue();
            assertThat(resposta.creditoProximaCompra()).isEqualTo(v("25.00"));
        }

        @Test
        void ouroNaoGanhaBrindeComExatamenteQuinhentosEmProdutos() {
            ItemRequest casaco = item("Casaco", "250.00", 2, "1.00");

            assertThat(calcularCom(List.of(casaco), "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE")
                    .brinde()).isFalse();
        }

        @Test
        void creditoNaoAbateNadaNestaCompra() {
            ResumoResponse resposta = calcularCom(List.of(CAMISETA, TENIS), "RETIRADA_LOJA", null,
                    "CARTAO", 1, "PRATA", "SUDESTE");

            assertThat(resposta.creditoProximaCompra()).isEqualTo(v("8.19"));
            assertThat(resposta.totalFinal()).isEqualTo(v("413.80"));
        }
    }

    @Nested
    @DisplayName("Seguro por regiao")
    class Seguro {

        @Test
        void cadaRegiaoTemSeuPercentualSobreOsProdutos() {
            assertThat(seguroDe("SUDESTE")).isEqualTo(v("4.10"));
            assertThat(seguroDe("SUL")).isEqualTo(v("4.10"));
            assertThat(seguroDe("CENTRO_OESTE")).isEqualTo(v("6.15"));
            assertThat(seguroDe("NORTE")).isEqualTo(v("10.24"));
            assertThat(seguroDe("NORDESTE")).isEqualTo(v("8.19"));
        }

        private BigDecimal seguroDe(String regiao) {
            return calcularCom(List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1,
                    "BRONZE", regiao).seguro();
        }
    }

    @Nested
    @DisplayName("Formas de pagamento")
    class Pagamentos {

        @Test
        void pixDaCincoPorCentoDeDescontoNoTotalDoPedido() {
            ResumoResponse resposta = calcularCom(List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", 1,
                    "BRONZE", "SUDESTE");

            assertThat(resposta.ajustePagamento()).isEqualTo(v("-8.07"));
            assertThat(resposta.totalFinal()).isEqualTo(v("153.33"));
        }

        @Test
        void boletoSomaTarifaDoBanco() {
            ResumoResponse resposta = calcularCom(List.of(CAMISETA), "RETIRADA_LOJA", null, "BOLETO", 1,
                    "BRONZE", "SUDESTE");

            assertThat(resposta.ajustePagamento()).isEqualTo(v("3.49"));
            assertThat(resposta.totalFinal()).isEqualTo(v("164.89"));
        }

        @Test
        void cartaoAteTresVezesNaoTemJuros() {
            ResumoResponse resposta = calcularCom(List.of(CAMISETA, TENIS), "ECONOMICA", null,
                    "CARTAO", 2, "BRONZE", "CENTRO_OESTE");

            assertThat(resposta.ajustePagamento()).isEqualTo(v("0.00"));
            assertThat(resposta.totalFinal()).isEqualTo(v("431.45"));
            assertThat(resposta.valorParcela()).isEqualTo(v("215.72"));
        }

        @Test
        void cartaoDeQuatroVezesEmDianteTemJurosDaTabelaPrice() {
            ResumoResponse quatro = calcularCom(List.of(CAMISETA, TENIS), "ECONOMICA", null,
                    "CARTAO", 4, "BRONZE", "CENTRO_OESTE");
            ResumoResponse doze = calcularCom(List.of(CAMISETA, TENIS), "ECONOMICA", null,
                    "CARTAO", 12, "BRONZE", "CENTRO_OESTE");

            assertThat(quatro.valorParcela()).isEqualTo(v("113.28"));
            assertThat(quatro.totalFinal()).isEqualTo(v("453.12"));
            assertThat(doze.valorParcela()).isEqualTo(v("40.77"));
            assertThat(doze.totalFinal()).isEqualTo(v("489.24"));
            assertThat(doze.ajustePagamento()).isEqualTo(v("57.79"));
        }

        @Test
        void semParcelasInformadasEhAVista() {
            ResumoResponse resposta = calcularCom(List.of(CAMISETA), "RETIRADA_LOJA", null, "CARTAO", null,
                    "BRONZE", "SUDESTE");

            assertThat(resposta.parcelas()).isEqualTo(1);
            assertThat(resposta.valorParcela()).isEqualTo(resposta.totalFinal());
        }

        @Test
        void pixEBoletoNaoParcelam() {
            recusa(pedido(List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", 2, "BRONZE", "SUDESTE"),
                    CodigoErro.PARCELAMENTO_INVALIDO);
            recusa(pedido(List.of(CAMISETA), "RETIRADA_LOJA", null, "BOLETO", 2, "BRONZE", "SUDESTE"),
                    CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void cartaoAceitaDeUmaADozeVezes() {
            recusa(pedido(List.of(CAMISETA), "RETIRADA_LOJA", null, "CARTAO", 13, "BRONZE", "SUDESTE"),
                    CodigoErro.PARCELAMENTO_INVALIDO);
            recusa(pedido(List.of(CAMISETA), "RETIRADA_LOJA", null, "CARTAO", 0, "BRONZE", "SUDESTE"),
                    CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void boletoNaoAtendeTotalAcimaDeMilReais() {
            ItemRequest movel = item("Poltrona", "1000.00", 2, "1.00");

            recusa(pedido(List.of(movel), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        @Test
        void boletoAtendeTotalDeExatamenteMilReais() {
            ItemRequest movel = item("Poltrona", "990.10", 1, "1.00");

            assertThat(calcularCom(List.of(movel), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE")
                    .totalFinal()).isEqualTo(v("1003.49"));
        }
    }

    @Nested
    @DisplayName("Pedidos recusados")
    class Recusas {

        @Test
        void carrinhoVazioOuAusente() {
            recusa(pedido(List.of(), "RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.PEDIDO_INVALIDO);
            recusa(pedido(null, "RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void itemComPrecoQuantidadeOuPesoInvalido() {
            recusa(pedido(List.of(item("X", "0.00", 1, "0.10")), "RETIRADA_LOJA", null, "PIX", 1,
                    "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
            recusa(pedido(List.of(item("X", "10.00", 0, "0.10")), "RETIRADA_LOJA", null, "PIX", 1,
                    "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
            recusa(pedido(List.of(item("X", "10.00", 1, "-0.10")), "RETIRADA_LOJA", null, "PIX", 1,
                    "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
            recusa(pedido(List.of(new ItemRequest("X", null, 1, new BigDecimal("0.10"))), "RETIRADA_LOJA",
                    null, "PIX", 1, "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
            recusa(pedido(List.of(new ItemRequest("X", new BigDecimal("10.00"), 1, null)), "RETIRADA_LOJA",
                    null, "PIX", 1, "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void opcoesQueNaoExistemOuNaoFoiInformadas() {
            recusa(pedido(List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", 1, "DIAMANTE", "SUDESTE"),
                    CodigoErro.NIVEL_CLUBE_INVALIDO);
            recusa(pedido(List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", 1, null, "SUDESTE"),
                    CodigoErro.NIVEL_CLUBE_INVALIDO);
            recusa(pedido(List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "EXTERIOR"),
                    CodigoErro.REGIAO_INVALIDA);
            recusa(pedido(List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", 1, "BRONZE", null),
                    CodigoErro.REGIAO_INVALIDA);
            recusa(pedido(List.of(CAMISETA), "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.MODALIDADE_INVALIDA);
            recusa(pedido(List.of(CAMISETA), null, null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.MODALIDADE_INVALIDA);
            recusa(pedido(List.of(CAMISETA), "RETIRADA_LOJA", null, "CHEQUE", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.FORMA_PAGAMENTO_INVALIDA);
            recusa(pedido(List.of(CAMISETA), "RETIRADA_LOJA", null, null, 1, "BRONZE", "SUDESTE"),
                    CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        @Test
        void devolveOPrimeiroProblemaDaOrdemDeConferencia() {
            recusa(pedido(List.of(), "DRONE", "PROMO999", "CHEQUE", 9, "DIAMANTE", "EXTERIOR"),
                    CodigoErro.PEDIDO_INVALIDO);
            recusa(pedido(List.of(CAMISETA), "DRONE", "PROMO999", "CHEQUE", 9, "DIAMANTE", "EXTERIOR"),
                    CodigoErro.NIVEL_CLUBE_INVALIDO);
            recusa(pedido(List.of(CAMISETA), "DRONE", "PROMO999", "CHEQUE", 9, "BRONZE", "EXTERIOR"),
                    CodigoErro.REGIAO_INVALIDA);
            recusa(pedido(List.of(CAMISETA), "DRONE", "PROMO999", "CHEQUE", 9, "BRONZE", "SUDESTE"),
                    CodigoErro.MODALIDADE_INVALIDA);
            recusa(pedido(List.of(item("Caixa", "100.00", 6, "1.00")), "MOTOBOY", "PROMO999", "CHEQUE", 9,
                    "BRONZE", "SUDESTE"), CodigoErro.MODALIDADE_INDISPONIVEL);
            recusa(pedido(List.of(CAMISETA), "RETIRADA_LOJA", "PROMO999", "CHEQUE", 9, "BRONZE", "SUDESTE"),
                    CodigoErro.CUPOM_INVALIDO);
            recusa(pedido(List.of(CAMISETA), "RETIRADA_LOJA", "MENOS50", "CHEQUE", 9, "BRONZE", "SUDESTE"),
                    CodigoErro.CUPOM_NAO_APLICAVEL);
            recusa(pedido(List.of(CAMISETA), "RETIRADA_LOJA", null, "CHEQUE", 9, "BRONZE", "SUDESTE"),
                    CodigoErro.FORMA_PAGAMENTO_INVALIDA);
            recusa(pedido(List.of(item("Poltrona", "1000.00", 2, "1.00")), "RETIRADA_LOJA", null,
                    "BOLETO", 9, "BRONZE", "SUDESTE"), CodigoErro.PARCELAMENTO_INVALIDO);
            recusa(pedido(List.of(item("Poltrona", "1000.00", 2, "1.00")), "RETIRADA_LOJA", null,
                    "BOLETO", 1, "BRONZE", "SUDESTE"), CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
    }

    private ResumoResponse calcularCom(List<ItemRequest> itens, String modalidade, String cupom,
            String formaPagamento, Integer parcelas, String nivelClube, String regiao) {
        return calculadora.calcular(
                pedido(itens, modalidade, cupom, formaPagamento, parcelas, nivelClube, regiao));
    }

    private void recusa(ResumoRequest pedido, CodigoErro esperado) {
        assertThatThrownBy(() -> calculadora.calcular(pedido))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(erro -> ((PedidoRecusadoException) erro).codigo())
                .isEqualTo(esperado);
    }
}
