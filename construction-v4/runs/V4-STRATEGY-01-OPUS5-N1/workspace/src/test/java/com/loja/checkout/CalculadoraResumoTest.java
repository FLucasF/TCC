package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.api.CalculadoraResumo;
import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.PedidoRecusadoException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CalculadoraResumoTest {

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private static ResumoRequest pedido(List<ItemRequest> itens, String entrega, String cupom,
            String pagamento, Integer parcelas, String clube, String regiao) {
        return new ResumoRequest(itens, entrega, cupom, pagamento, parcelas, clube, regiao);
    }

    @Nested
    class ExemplosConferidosPeloFinanceiro {

        @Test
        void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
            ResumoResponse resumo = calculadora.calcular(pedido(List.of(CAMISETA, TENIS),
                    "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

            assertThat(resumo.subtotalProdutos()).isEqualTo(new BigDecimal("409.70"));
            assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("40.97"));
            assertThat(resumo.frete()).isEqualTo(new BigDecimal("33.10"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
            assertThat(resumo.seguro()).isEqualTo(new BigDecimal("10.24"));
            assertThat(resumo.ajustePagamento()).isEqualTo(new BigDecimal("-20.60"));
            assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("391.47"));
            assertThat(resumo.parcelas()).isEqualTo(1);
            assertThat(resumo.valorParcela()).isEqualTo(new BigDecimal("391.47"));
            assertThat(resumo.creditoProximaCompra()).isEqualTo(new BigDecimal("0.00"));
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void exemplo2_economica_sem_cupom_cartao_6x_prata_centro_oeste() {
            ResumoResponse resumo = calculadora.calcular(pedido(List.of(CAMISETA, TENIS),
                    "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

            assertThat(resumo.subtotalProdutos()).isEqualTo(new BigDecimal("409.70"));
            assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("0.00"));
            assertThat(resumo.frete()).isEqualTo(new BigDecimal("15.60"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
            assertThat(resumo.seguro()).isEqualTo(new BigDecimal("6.15"));
            assertThat(resumo.ajustePagamento()).isEqualTo(new BigDecimal("30.55"));
            assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("462.00"));
            assertThat(resumo.parcelas()).isEqualTo(6);
            assertThat(resumo.valorParcela()).isEqualTo(new BigDecimal("77.00"));
            assertThat(resumo.creditoProximaCompra()).isEqualTo(new BigDecimal("8.19"));
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
            ResumoResponse resumo = calculadora.calcular(
                    pedido(List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50",
                            "BOLETO", null, "BRONZE", "NORDESTE"));

            assertThat(resumo.subtotalProdutos()).isEqualTo(new BigDecimal("399.80"));
            assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("50.00"));
            assertThat(resumo.frete()).isEqualTo(new BigDecimal("18.00"));
            assertThat(resumo.prazoEntregaDias()).isZero();
            assertThat(resumo.seguro()).isEqualTo(new BigDecimal("8.00"));
            assertThat(resumo.ajustePagamento()).isEqualTo(new BigDecimal("3.49"));
            assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("379.29"));
            assertThat(resumo.parcelas()).isEqualTo(1);
            assertThat(resumo.valorParcela()).isEqualTo(new BigDecimal("379.29"));
            assertThat(resumo.creditoProximaCompra()).isEqualTo(new BigDecimal("0.00"));
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void exemplo4_retirada_leve3pague2_cartao_3x_prata_sul() {
            ResumoResponse resumo = calculadora.calcular(
                    pedido(List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA), "RETIRADA_LOJA",
                            "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

            assertThat(resumo.subtotalProdutos()).isEqualTo(new BigDecimal("299.10"));
            assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("39.80"));
            assertThat(resumo.frete()).isEqualTo(new BigDecimal("0.00"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
            assertThat(resumo.seguro()).isEqualTo(new BigDecimal("2.99"));
            assertThat(resumo.ajustePagamento()).isEqualTo(new BigDecimal("0.00"));
            assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("262.29"));
            assertThat(resumo.parcelas()).isEqualTo(3);
            assertThat(resumo.valorParcela()).isEqualTo(new BigDecimal("87.43"));
            assertThat(resumo.creditoProximaCompra()).isEqualTo(new BigDecimal("5.98"));
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
            ResumoResponse resumo = calculadora.calcular(pedido(List.of(CAMISETA, TENIS),
                    "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

            assertThat(resumo.subtotalProdutos()).isEqualTo(new BigDecimal("409.70"));
            assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("0.00"));
            assertThat(resumo.frete()).isEqualTo(new BigDecimal("0.00"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
            assertThat(resumo.seguro()).isEqualTo(new BigDecimal("4.10"));
            assertThat(resumo.ajustePagamento()).isEqualTo(new BigDecimal("-20.69"));
            assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("393.11"));
            assertThat(resumo.valorParcela()).isEqualTo(new BigDecimal("393.11"));
            assertThat(resumo.creditoProximaCompra()).isEqualTo(new BigDecimal("20.48"));
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void exemplo_do_anexo_expressa_bemvindo10_pix_ouro_sudeste() {
            ResumoResponse resumo = calculadora.calcular(pedido(List.of(CAMISETA, TENIS),
                    "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"));

            assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("40.97"));
            assertThat(resumo.frete()).isEqualTo(new BigDecimal("0.00"));
            assertThat(resumo.seguro()).isEqualTo(new BigDecimal("4.10"));
            assertThat(resumo.ajustePagamento()).isEqualTo(new BigDecimal("-18.64"));
            assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("354.19"));
            assertThat(resumo.creditoProximaCompra()).isEqualTo(new BigDecimal("20.48"));
            assertThat(resumo.brinde()).isFalse();
        }
    }

    @Nested
    class Entrega {

        @Test
        void retirada_na_loja_nao_cobra_frete() {
            ResumoResponse resumo = calculadora.calcular(pedido(List.of(TENIS), "RETIRADA_LOJA",
                    null, "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.frete()).isEqualTo(new BigDecimal("0.00"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        }

        @Test
        void motoboy_cobra_valor_fixo_independente_do_peso() {
            ResumoResponse leve = calculadora.calcular(pedido(List.of(item("Meia", "19.90", 1,
                    "0.10")), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"));
            ResumoResponse pesado = calculadora.calcular(pedido(List.of(item("Mala", "19.90", 1,
                    "4.90")), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(leve.frete()).isEqualTo(new BigDecimal("18.00"));
            assertThat(pesado.frete()).isEqualTo(new BigDecimal("18.00"));
        }

        @Test
        void motoboy_atende_exatamente_cinco_quilos() {
            ResumoResponse resumo = calculadora.calcular(pedido(List.of(item("Mala", "100.00", 5,
                    "1.00")), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.frete()).isEqualTo(new BigDecimal("18.00"));
        }

        @Test
        void motoboy_nao_atende_acima_de_cinco_quilos() {
            assertThatThrownBy(() -> calculadora.calcular(pedido(List.of(item("Mala", "100.00", 5,
                    "1.01")), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isInstanceOf(PedidoRecusadoException.class)
                    .extracting(erro -> ((PedidoRecusadoException) erro).codigo())
                    .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
    }

    @Nested
    class Cupons {

        @Test
        void fretegratis_desconta_exatamente_o_frete_mostrado() {
            ResumoResponse resumo = calculadora.calcular(pedido(List.of(CAMISETA, TENIS),
                    "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.frete()).isEqualTo(new BigDecimal("33.10"));
            assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("33.10"));
            assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("413.80"));
        }

        @Test
        void fretegratis_com_ouro_nao_desconta_nada_porque_o_frete_ja_e_zero() {
            ResumoResponse resumo = calculadora.calcular(pedido(List.of(CAMISETA, TENIS),
                    "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "OURO", "SUDESTE"));

            assertThat(resumo.frete()).isEqualTo(new BigDecimal("0.00"));
            assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("0.00"));
        }

        @Test
        void leve3pague2_libera_uma_unidade_a_cada_tres_do_mesmo_item() {
            ResumoResponse resumo = calculadora.calcular(pedido(List.of(item("Meia", "10.00", 6,
                    "0.10"), item("Bone", "30.00", 2, "0.10")), "RETIRADA_LOJA", "LEVE3PAGUE2",
                    "CARTAO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("20.00"));
        }

        @Test
        void menos50_vale_a_partir_de_trezentos_em_produtos() {
            ResumoResponse resumo = calculadora.calcular(pedido(List.of(item("Jaqueta", "300.00",
                    1, "1.00")), "RETIRADA_LOJA", "MENOS50", "CARTAO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("50.00"));
        }

        @Test
        void menos50_nao_vale_abaixo_de_trezentos_em_produtos() {
            assertThatThrownBy(() -> calculadora.calcular(pedido(List.of(item("Jaqueta", "299.99",
                    1, "1.00")), "RETIRADA_LOJA", "MENOS50", "CARTAO", 1, "BRONZE", "SUDESTE")))
                    .extracting(erro -> ((PedidoRecusadoException) erro).codigo())
                    .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        @Test
        void codigo_fora_do_catalogo_e_recusado() {
            assertThatThrownBy(() -> calculadora.calcular(pedido(List.of(TENIS), "RETIRADA_LOJA",
                    "bemvindo10", "CARTAO", 1, "BRONZE", "SUDESTE")))
                    .extracting(erro -> ((PedidoRecusadoException) erro).codigo())
                    .isEqualTo(CodigoErro.CUPOM_INVALIDO);
        }
    }

    @Nested
    class Clube {

        @Test
        void ouro_manda_brinde_acima_de_quinhentos_em_produtos() {
            ResumoResponse comBrinde = calculadora.calcular(pedido(List.of(item("Casaco",
                    "500.01", 1, "1.00")), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));
            ResumoResponse semBrinde = calculadora.calcular(pedido(List.of(item("Casaco",
                    "500.00", 1, "1.00")), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

            assertThat(comBrinde.brinde()).isTrue();
            assertThat(semBrinde.brinde()).isFalse();
        }

        @Test
        void prata_nao_ganha_frete_gratis_nem_brinde() {
            ResumoResponse resumo = calculadora.calcular(pedido(List.of(item("Casaco", "600.00",
                    1, "1.00")), "EXPRESSA", null, "PIX", 1, "PRATA", "SUDESTE"));

            assertThat(resumo.frete()).isEqualTo(new BigDecimal("29.50"));
            assertThat(resumo.brinde()).isFalse();
            assertThat(resumo.creditoProximaCompra()).isEqualTo(new BigDecimal("12.00"));
        }

        @Test
        void credito_nao_abate_nada_nesta_compra() {
            ResumoResponse resumo = calculadora.calcular(pedido(List.of(item("Casaco", "600.00",
                    1, "1.00")), "RETIRADA_LOJA", null, "CARTAO", 1, "PRATA", "SUDESTE"));

            assertThat(resumo.creditoProximaCompra()).isEqualTo(new BigDecimal("12.00"));
            assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("606.00"));
        }
    }

    @Nested
    class Pagamento {

        @Test
        void cartao_em_ate_tres_vezes_nao_tem_juros() {
            ResumoResponse resumo = calculadora.calcular(pedido(List.of(item("Jaqueta", "100.00",
                    1, "1.00")), "RETIRADA_LOJA", null, "CARTAO", 3, "BRONZE", "SUDESTE"));

            assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("101.00"));
            assertThat(resumo.ajustePagamento()).isEqualTo(new BigDecimal("0.00"));
            assertThat(resumo.valorParcela()).isEqualTo(new BigDecimal("33.67"));
        }

        @Test
        void cartao_de_quatro_a_doze_vezes_tem_juros_e_o_total_e_a_parcela_vezes_as_parcelas() {
            ResumoResponse resumo = calculadora.calcular(pedido(List.of(item("Jaqueta", "100.00",
                    1, "1.00")), "RETIRADA_LOJA", null, "CARTAO", 4, "BRONZE", "SUDESTE"));

            assertThat(resumo.valorParcela()).isEqualTo(new BigDecimal("26.52"));
            assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("106.08"));
            assertThat(resumo.ajustePagamento()).isEqualTo(new BigDecimal("5.08"));
        }

        @Test
        void boleto_soma_a_tarifa_do_banco() {
            ResumoResponse resumo = calculadora.calcular(pedido(List.of(item("Jaqueta", "100.00",
                    1, "1.00")), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.ajustePagamento()).isEqualTo(new BigDecimal("3.49"));
            assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("104.49"));
        }

        @Test
        void boleto_atende_total_de_exatamente_mil_reais() {
            ResumoResponse resumo = calculadora.calcular(pedido(List.of(item("Jaqueta", "990.10",
                    1, "1.00")), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("1003.49"));
        }

        @Test
        void boleto_nao_atende_total_acima_de_mil_reais() {
            assertThatThrownBy(() -> calculadora.calcular(pedido(List.of(item("Jaqueta",
                    "990.11", 1, "1.00")), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE",
                    "SUDESTE")))
                    .extracting(erro -> ((PedidoRecusadoException) erro).codigo())
                    .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        @Test
        void pix_e_boleto_sao_sempre_a_vista() {
            assertThatThrownBy(() -> calculadora.calcular(pedido(List.of(TENIS), "RETIRADA_LOJA",
                    null, "PIX", 2, "BRONZE", "SUDESTE")))
                    .extracting(erro -> ((PedidoRecusadoException) erro).codigo())
                    .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
            assertThatThrownBy(() -> calculadora.calcular(pedido(List.of(TENIS), "RETIRADA_LOJA",
                    null, "BOLETO", 2, "BRONZE", "SUDESTE")))
                    .extracting(erro -> ((PedidoRecusadoException) erro).codigo())
                    .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void cartao_aceita_de_uma_a_doze_parcelas() {
            assertThat(calculadora.calcular(pedido(List.of(TENIS), "RETIRADA_LOJA", null,
                    "CARTAO", 12, "BRONZE", "SUDESTE")).parcelas()).isEqualTo(12);
            assertThatThrownBy(() -> calculadora.calcular(pedido(List.of(TENIS), "RETIRADA_LOJA",
                    null, "CARTAO", 13, "BRONZE", "SUDESTE")))
                    .extracting(erro -> ((PedidoRecusadoException) erro).codigo())
                    .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
            assertThatThrownBy(() -> calculadora.calcular(pedido(List.of(TENIS), "RETIRADA_LOJA",
                    null, "CARTAO", 0, "BRONZE", "SUDESTE")))
                    .extracting(erro -> ((PedidoRecusadoException) erro).codigo())
                    .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void sem_parcelas_informadas_a_compra_e_a_vista() {
            assertThat(calculadora.calcular(pedido(List.of(TENIS), "RETIRADA_LOJA", null,
                    "CARTAO", null, "BRONZE", "SUDESTE")).parcelas()).isEqualTo(1);
        }
    }

    @Nested
    class Seguro {

        @Test
        void a_taxa_do_seguro_muda_por_regiao_sobre_o_valor_dos_produtos() {
            assertThat(seguroEm("SUDESTE")).isEqualTo(new BigDecimal("2.00"));
            assertThat(seguroEm("SUL")).isEqualTo(new BigDecimal("2.00"));
            assertThat(seguroEm("CENTRO_OESTE")).isEqualTo(new BigDecimal("3.00"));
            assertThat(seguroEm("NORTE")).isEqualTo(new BigDecimal("5.00"));
            assertThat(seguroEm("NORDESTE")).isEqualTo(new BigDecimal("4.00"));
        }

        @Test
        void o_seguro_ignora_desconto_e_frete() {
            ResumoResponse resumo = calculadora.calcular(pedido(List.of(item("Jaqueta", "400.00",
                    1, "1.00")), "EXPRESSA", "MENOS50", "CARTAO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.seguro()).isEqualTo(new BigDecimal("4.00"));
        }

        private BigDecimal seguroEm(String regiao) {
            return calculadora.calcular(pedido(List.of(item("Jaqueta", "200.00", 1, "1.00")),
                    "RETIRADA_LOJA", null, "CARTAO", 1, "BRONZE", regiao)).seguro();
        }
    }

    @Nested
    class OrdemDaRecusa {

        @Test
        void carrinho_vazio_ou_ausente() {
            assertThat(recusa(pedido(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
            assertThat(recusa(pedido(null, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void item_com_preco_quantidade_ou_peso_invalido() {
            assertThat(recusa(pedido(List.of(item("X", "0.00", 1, "1.00")), "EXPRESSA", null,
                    "PIX", 1, "BRONZE", "SUDESTE"))).isEqualTo(CodigoErro.PEDIDO_INVALIDO);
            assertThat(recusa(pedido(List.of(item("X", "10.00", 0, "1.00")), "EXPRESSA", null,
                    "PIX", 1, "BRONZE", "SUDESTE"))).isEqualTo(CodigoErro.PEDIDO_INVALIDO);
            assertThat(recusa(pedido(List.of(item("X", "10.00", 1, "-1.00")), "EXPRESSA", null,
                    "PIX", 1, "BRONZE", "SUDESTE"))).isEqualTo(CodigoErro.PEDIDO_INVALIDO);
            assertThat(recusa(pedido(List.of(new ItemRequest("X", null, null, null)), "EXPRESSA",
                    null, "PIX", 1, "BRONZE", "SUDESTE"))).isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void pedido_invalido_vem_antes_de_tudo() {
            assertThat(recusa(pedido(List.of(), "NAVIO", "XPTO", "CHEQUE", 99, "DIAMANTE",
                    "LUA"))).isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void nivel_do_clube_antes_da_regiao() {
            assertThat(recusa(pedido(List.of(TENIS), "NAVIO", "XPTO", "CHEQUE", 99, "DIAMANTE",
                    "LUA"))).isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
            assertThat(recusa(pedido(List.of(TENIS), "EXPRESSA", null, "PIX", 1, null,
                    "SUDESTE"))).isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        @Test
        void regiao_antes_da_modalidade() {
            assertThat(recusa(pedido(List.of(TENIS), "NAVIO", "XPTO", "CHEQUE", 99, "BRONZE",
                    "LUA"))).isEqualTo(CodigoErro.REGIAO_INVALIDA);
            assertThat(recusa(pedido(List.of(TENIS), "EXPRESSA", null, "PIX", 1, "BRONZE", null)))
                    .isEqualTo(CodigoErro.REGIAO_INVALIDA);
        }

        @Test
        void modalidade_inexistente_antes_de_indisponivel() {
            assertThat(recusa(pedido(List.of(TENIS), "NAVIO", "XPTO", "CHEQUE", 99, "BRONZE",
                    "SUDESTE"))).isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
            assertThat(recusa(pedido(List.of(TENIS), null, null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
        }

        @Test
        void modalidade_indisponivel_antes_do_cupom() {
            assertThat(recusa(pedido(List.of(item("Mala", "10.00", 6, "1.00")), "MOTOBOY", "XPTO",
                    "CHEQUE", 99, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        @Test
        void cupom_inexistente_antes_de_nao_aplicavel() {
            assertThat(recusa(pedido(List.of(item("Meia", "19.90", 1, "0.10")), "EXPRESSA",
                    "XPTO", "CHEQUE", 99, "BRONZE", "SUDESTE"))).isEqualTo(CodigoErro.CUPOM_INVALIDO);
        }

        @Test
        void cupom_nao_aplicavel_antes_da_forma_de_pagamento() {
            assertThat(recusa(pedido(List.of(item("Meia", "19.90", 1, "0.10")), "EXPRESSA",
                    "MENOS50", "CHEQUE", 99, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        @Test
        void forma_de_pagamento_inexistente_antes_do_parcelamento() {
            assertThat(recusa(pedido(List.of(TENIS), "EXPRESSA", null, "CHEQUE", 99, "BRONZE",
                    "SUDESTE"))).isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
            assertThat(recusa(pedido(List.of(TENIS), "EXPRESSA", null, null, 1, "BRONZE",
                    "SUDESTE"))).isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        @Test
        void parcelamento_antes_da_forma_de_pagamento_indisponivel() {
            assertThat(recusa(pedido(List.of(item("Jaqueta", "2000.00", 1, "1.00")),
                    "RETIRADA_LOJA", null, "BOLETO", 2, "BRONZE", "SUDESTE")))
                    .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        private CodigoErro recusa(ResumoRequest pedido) {
            try {
                calculadora.calcular(pedido);
            } catch (PedidoRecusadoException recusado) {
                return recusado.codigo();
            }
            throw new AssertionError("o pedido deveria ter sido recusado");
        }
    }

    @Nested
    class Arredondamento {

        @Test
        void o_arredondamento_e_meio_para_o_par() {
            // 1% de 298,50 = 2,985 -> 2,98 (desce para o par)
            assertThat(seguroDe("298.50")).isEqualTo(new BigDecimal("2.98"));
            // 1% de 299,50 = 2,995 -> 3,00 (sobe para o par)
            assertThat(seguroDe("299.50")).isEqualTo(new BigDecimal("3.00"));
        }

        private BigDecimal seguroDe(String preco) {
            return calculadora.calcular(pedido(List.of(item("Camisa", preco, 1, "0.10")),
                    "RETIRADA_LOJA", null, "CARTAO", 1, "BRONZE", "SUDESTE")).seguro();
        }
    }
}
