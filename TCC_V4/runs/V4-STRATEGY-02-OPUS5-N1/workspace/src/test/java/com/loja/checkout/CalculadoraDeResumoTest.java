package com.loja.checkout;

import static com.loja.checkout.CompraDeExemplo.CAMISETA;
import static com.loja.checkout.CompraDeExemplo.FONE;
import static com.loja.checkout.CompraDeExemplo.MEIA;
import static com.loja.checkout.CompraDeExemplo.TENIS;
import static com.loja.checkout.CompraDeExemplo.compra;
import static com.loja.checkout.CompraDeExemplo.item;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.loja.checkout.api.CalculadoraDeResumo;
import com.loja.checkout.api.CompraRequest;
import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.erro.Codigo;
import com.loja.checkout.erro.PedidoRecusado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

class CalculadoraDeResumoTest {

    private final CalculadoraDeResumo calculadora = new CalculadoraDeResumo();

    @Nested
    @DisplayName("exemplos conferidos pelo financeiro")
    class Exemplos {

        @Test
        void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
            ResumoResponse resumo = calculadora.calcular(compra(
                    List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

            assertThat(resumo).isEqualTo(new ResumoResponse(
                    valor("409.70"), valor("40.97"), valor("33.10"), 2, valor("10.24"),
                    valor("-20.60"), valor("391.47"), 1, valor("391.47"), valor("0.00"), false));
        }

        @Test
        void exemplo2_economica_sem_cupom_cartao_6x_prata_centro_oeste() {
            ResumoResponse resumo = calculadora.calcular(compra(
                    List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

            assertThat(resumo).isEqualTo(new ResumoResponse(
                    valor("409.70"), valor("0.00"), valor("15.60"), 7, valor("6.15"),
                    valor("30.55"), valor("462.00"), 6, valor("77.00"), valor("8.19"), false));
        }

        @Test
        void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
            ResumoResponse resumo = calculadora.calcular(compra(
                    List.of(FONE), "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));

            assertThat(resumo).isEqualTo(new ResumoResponse(
                    valor("399.80"), valor("50.00"), valor("18.00"), 0, valor("8.00"),
                    valor("3.49"), valor("379.29"), 1, valor("379.29"), valor("0.00"), false));
        }

        @Test
        void exemplo4_retirada_leve3pague2_cartao_3x_prata_sul() {
            ResumoResponse resumo = calculadora.calcular(compra(
                    List.of(MEIA, CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

            assertThat(resumo).isEqualTo(new ResumoResponse(
                    valor("299.10"), valor("39.80"), valor("0.00"), 1, valor("2.99"),
                    valor("0.00"), valor("262.29"), 3, valor("87.43"), valor("5.98"), false));
        }

        @Test
        void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
            ResumoResponse resumo = calculadora.calcular(compra(
                    List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

            assertThat(resumo).isEqualTo(new ResumoResponse(
                    valor("409.70"), valor("0.00"), valor("0.00"), 2, valor("4.10"),
                    valor("-20.69"), valor("393.11"), 1, valor("393.11"), valor("20.48"), false));
        }

        @Test
        void exemplo_do_anexo_expressa_bemvindo10_pix_ouro_sudeste() {
            ResumoResponse resumo = calculadora.calcular(compra(
                    List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"));

            assertThat(resumo).isEqualTo(new ResumoResponse(
                    valor("409.70"), valor("40.97"), valor("0.00"), 2, valor("4.10"),
                    valor("-18.64"), valor("354.19"), 1, valor("354.19"), valor("20.48"), false));
        }
    }

    @Nested
    @DisplayName("entrega")
    class Entrega {

        @Test
        void retirada_na_loja_nao_cobra_frete_e_entrega_em_um_dia() {
            ResumoResponse resumo = calcular("RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUDESTE");

            assertThat(resumo.frete()).isEqualTo(valor("0.00"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        }

        @Test
        void motoboy_entrega_no_mesmo_dia_ate_cinco_quilos() {
            ResumoResponse resumo = calcular("MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");

            assertThat(resumo.frete()).isEqualTo(valor("18.00"));
            assertThat(resumo.prazoEntregaDias()).isZero();
        }

        @Test
        void motoboy_nao_leva_pedido_acima_de_cinco_quilos() {
            assertThat(recusaDe(compra(List.of(item("Tapete", "100.00", 1, "5.01")),
                    "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(Codigo.MODALIDADE_INDISPONIVEL);
        }

        @Test
        void motoboy_leva_pedido_de_exatamente_cinco_quilos() {
            ResumoResponse resumo = calculadora.calcular(compra(List.of(item("Tapete", "100.00", 5, "1.00")),
                    "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.frete()).isEqualTo(valor("18.00"));
        }

        @Test
        void frete_cobra_por_quilo_do_pedido_inteiro() {
            ResumoResponse economica = calcular("ECONOMICA", null, "PIX", 1, "BRONZE", "SUDESTE");
            ResumoResponse expressa = calcular("EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");

            assertThat(economica.frete()).isEqualTo(valor("15.60"));
            assertThat(expressa.frete()).isEqualTo(valor("33.10"));
        }
    }

    @Nested
    @DisplayName("cupons")
    class Cupons {
        @Test
        void sem_cupom_nao_tem_desconto() {
            assertThat(calcular("RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUDESTE").descontoCupom())
                    .isEqualTo(valor("0.00"));
        }

        @Test
        void menos50_vale_a_partir_de_trezentos_reais_em_produtos() {
            ResumoResponse resumo = calculadora.calcular(compra(List.of(item("Jaqueta", "300.00", 1, "0.50")),
                    "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.descontoCupom()).isEqualTo(valor("50.00"));
        }

        @Test
        void menos50_nao_vale_abaixo_de_trezentos_reais_em_produtos() {
            assertThat(recusaDe(compra(List.of(item("Jaqueta", "299.99", 1, "0.50")),
                    "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(Codigo.CUPOM_NAO_APLICAVEL);
        }

        @Test
        void fretegratis_desconta_o_frete_que_aparece_no_resumo() {
            ResumoResponse resumo = calcular("EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE");

            assertThat(resumo.frete()).isEqualTo(valor("33.10"));
            assertThat(resumo.descontoCupom()).isEqualTo(valor("33.10"));
        }

        @Test
        void fretegratis_para_quem_ja_nao_paga_frete_nao_desconta_nada() {
            ResumoResponse resumo = calcular("EXPRESSA", "FRETEGRATIS", "PIX", 1, "OURO", "SUDESTE");

            assertThat(resumo.frete()).isEqualTo(valor("0.00"));
            assertThat(resumo.descontoCupom()).isEqualTo(valor("0.00"));
        }

        @Test
        void leve3pague2_da_uma_unidade_de_graca_a_cada_tres_do_mesmo_item() {
            ResumoResponse resumo = calculadora.calcular(compra(List.of(item("Meia", "10.00", 6, "0.10")),
                    "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.descontoCupom()).isEqualTo(valor("20.00"));
        }

        @Test
        void leve3pague2_nao_da_nada_quando_nenhum_item_chega_a_tres_unidades() {
            ResumoResponse resumo = calculadora.calcular(compra(List.of(item("Meia", "10.00", 2, "0.10")),
                    "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.descontoCupom()).isEqualTo(valor("0.00"));
        }

        @Test
        void bemvindo10_desconta_dez_por_cento_dos_produtos() {
            assertThat(calcular("RETIRADA_LOJA", "BEMVINDO10", "PIX", 1, "BRONZE", "SUDESTE").descontoCupom())
                    .isEqualTo(valor("40.97"));
        }
    }

    @Nested
    @DisplayName("clube da loja")
    class Clube {

        @Test
        void bronze_nao_ganha_nada() {
            ResumoResponse resumo = calcular("EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");

            assertThat(resumo.creditoProximaCompra()).isEqualTo(valor("0.00"));
            assertThat(resumo.frete()).isEqualTo(valor("33.10"));
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void prata_ganha_dois_por_cento_dos_produtos_em_credito() {
            assertThat(calcular("EXPRESSA", null, "PIX", 1, "PRATA", "SUDESTE").creditoProximaCompra())
                    .isEqualTo(valor("8.19"));
        }

        @Test
        void ouro_ganha_cinco_por_cento_em_credito_e_nao_paga_frete() {
            ResumoResponse resumo = calcular("EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

            assertThat(resumo.creditoProximaCompra()).isEqualTo(valor("20.48"));
            assertThat(resumo.frete()).isEqualTo(valor("0.00"));
        }

        @Test
        void ouro_leva_brinde_quando_os_produtos_passam_de_quinhentos_reais() {
            ResumoResponse resumo = calculadora.calcular(compra(List.of(item("Casaco", "500.01", 1, "1.00")),
                    "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

            assertThat(resumo.brinde()).isTrue();
        }

        @Test
        void ouro_nao_leva_brinde_com_exatamente_quinhentos_reais_em_produtos() {
            ResumoResponse resumo = calculadora.calcular(compra(List.of(item("Casaco", "500.00", 1, "1.00")),
                    "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void o_credito_nao_abate_nada_nesta_compra() {
            ResumoResponse bronze = calcular("RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUDESTE");
            ResumoResponse prata = calcular("RETIRADA_LOJA", null, "PIX", 1, "PRATA", "SUDESTE");

            assertThat(prata.totalFinal()).isEqualTo(bronze.totalFinal());
        }
    }

    @Nested
    @DisplayName("seguro por regiao")
    class SeguroDoEnvio {

        @Test
        void a_porcentagem_da_regiao_incide_so_sobre_os_produtos() {
            assertThat(calcular("EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "SUDESTE").seguro())
                    .isEqualTo(valor("4.10"));
            assertThat(calcular("EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "SUL").seguro())
                    .isEqualTo(valor("4.10"));
            assertThat(calcular("EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "CENTRO_OESTE").seguro())
                    .isEqualTo(valor("6.15"));
            assertThat(calcular("EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE").seguro())
                    .isEqualTo(valor("10.24"));
            assertThat(calcular("EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORDESTE").seguro())
                    .isEqualTo(valor("8.19"));
        }
    }

    @Nested
    @DisplayName("formas de pagamento")
    class Pagamento {

        @Test
        void pix_desconta_cinco_por_cento_do_total_do_pedido() {
            ResumoResponse resumo = calcular("RETIRADA_LOJA", null, "PIX", null, "BRONZE", "SUDESTE");

            assertThat(resumo.ajustePagamento()).isEqualTo(valor("-20.69"));
            assertThat(resumo.totalFinal()).isEqualTo(valor("393.11"));
        }

        @Test
        void boleto_soma_a_tarifa_do_banco() {
            ResumoResponse resumo = calcular("RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE");

            assertThat(resumo.ajustePagamento()).isEqualTo(valor("3.49"));
            assertThat(resumo.totalFinal()).isEqualTo(valor("417.29"));
        }

        @Test
        void cartao_ate_tres_vezes_nao_tem_juros() {
            ResumoResponse resumo = calcular("RETIRADA_LOJA", null, "CARTAO", 2, "BRONZE", "SUDESTE");

            assertThat(resumo.ajustePagamento()).isEqualTo(valor("0.00"));
            assertThat(resumo.totalFinal()).isEqualTo(valor("413.80"));
            assertThat(resumo.valorParcela()).isEqualTo(valor("206.90"));
        }

        @Test
        void cartao_de_quatro_vezes_para_cima_tem_juros_pela_tabela_price() {
            ResumoResponse resumo = calcular("RETIRADA_LOJA", null, "CARTAO", 4, "BRONZE", "SUDESTE");

            assertThat(resumo.valorParcela()).isEqualTo(valor("108.65"));
            assertThat(resumo.totalFinal()).isEqualTo(valor("434.60"));
            assertThat(resumo.ajustePagamento()).isEqualTo(valor("20.80"));
        }

        @Test
        void cartao_em_doze_vezes() {
            ResumoResponse resumo = calcular("RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE");

            assertThat(resumo.parcelas()).isEqualTo(12);
            assertThat(resumo.valorParcela()).isEqualTo(valor("39.10"));
            assertThat(resumo.totalFinal()).isEqualTo(valor("469.20"));
        }

        @Test
        void sem_parcelas_informadas_a_compra_e_em_uma_vez() {
            ResumoResponse resumo = calcular("RETIRADA_LOJA", null, "CARTAO", null, "BRONZE", "SUDESTE");

            assertThat(resumo.parcelas()).isEqualTo(1);
            assertThat(resumo.valorParcela()).isEqualTo(resumo.totalFinal());
        }

        @Test
        void boleto_nao_e_aceito_acima_de_mil_reais_no_total_do_pedido() {
            assertThat(recusaDe(compra(List.of(item("Casaco", "1000.00", 1, "1.00")),
                    "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(Codigo.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        @Test
        void boleto_e_aceito_com_exatamente_mil_reais_no_total_do_pedido() {
            ResumoResponse resumo = calculadora.calcular(compra(List.of(item("Casaco", "990.10", 1, "1.00")),
                    "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.totalFinal()).isEqualTo(valor("1003.49"));
        }

        @Test
        void pix_e_boleto_sao_sempre_a_vista() {
            assertThat(recusaDe(compra(List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", 2, "BRONZE", "SUDESTE")))
                    .isEqualTo(Codigo.PARCELAMENTO_INVALIDO);
            assertThat(recusaDe(compra(List.of(CAMISETA), "RETIRADA_LOJA", null, "BOLETO", 2, "BRONZE", "SUDESTE")))
                    .isEqualTo(Codigo.PARCELAMENTO_INVALIDO);
        }

        @Test
        void cartao_parcela_de_uma_a_doze_vezes() {
            assertThat(recusaDe(compra(List.of(CAMISETA), "RETIRADA_LOJA", null, "CARTAO", 13, "BRONZE", "SUDESTE")))
                    .isEqualTo(Codigo.PARCELAMENTO_INVALIDO);
            assertThat(recusaDe(compra(List.of(CAMISETA), "RETIRADA_LOJA", null, "CARTAO", 0, "BRONZE", "SUDESTE")))
                    .isEqualTo(Codigo.PARCELAMENTO_INVALIDO);
        }
    }

    @Nested
    @DisplayName("arredondamento")
    class Arredondamento {

        @Test
        void centavos_sao_arredondados_meio_para_o_par() {
            ResumoResponse paraCima = calculadora.calcular(compra(List.of(item("Brinco", "299.50", 1, "0.10")),
                    "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"));
            ResumoResponse paraBaixo = calculadora.calcular(compra(List.of(item("Brinco", "298.50", 1, "0.10")),
                    "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"));

            assertThat(paraCima.seguro()).isEqualTo(valor("3.00"));
            assertThat(paraBaixo.seguro()).isEqualTo(valor("2.98"));
        }

        @Test
        void todo_valor_em_dinheiro_vem_com_duas_casas() {
            ResumoResponse resumo = calcular("RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUDESTE");

            assertThat(List.of(resumo.subtotalProdutos(), resumo.descontoCupom(), resumo.frete(), resumo.seguro(),
                            resumo.ajustePagamento(), resumo.totalFinal(), resumo.valorParcela(),
                            resumo.creditoProximaCompra()))
                    .allSatisfy(dinheiro -> assertThat(dinheiro.scale()).isEqualTo(2));
        }
    }

    @Nested
    @DisplayName("pedidos recusados, na ordem em que os problemas sao conferidos")
    class Recusas {

        @Test
        void carrinho_vazio_ou_ausente() {
            assertThat(recusaDe(compra(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(Codigo.PEDIDO_INVALIDO);
            assertThat(recusaDe(compra(null, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(Codigo.PEDIDO_INVALIDO);
        }

        @Test
        void item_com_preco_quantidade_ou_peso_invalido() {
            List<ItemRequest> invalidos = Arrays.asList(
                    item("Camiseta", "0.00", 1, "0.30"),
                    item("Camiseta", "-1.00", 1, "0.30"),
                    new ItemRequest("Camiseta", null, 1, new BigDecimal("0.30")),
                    item("Camiseta", "79.90", 0, "0.30"),
                    item("Camiseta", "79.90", -1, "0.30"),
                    new ItemRequest("Camiseta", new BigDecimal("79.90"), null, new BigDecimal("0.30")),
                    item("Camiseta", "79.90", 1, "0.00"),
                    item("Camiseta", "79.90", 1, "-0.30"),
                    new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, null));

            assertThat(invalidos).allSatisfy(invalido -> assertThat(
                    recusaDe(compra(List.of(invalido), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(Codigo.PEDIDO_INVALIDO));
        }

        @Test
        void nivel_do_clube_que_nao_existe_ou_nao_informado() {
            assertThat(recusaDe(compra(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE")))
                    .isEqualTo(Codigo.NIVEL_CLUBE_INVALIDO);
            assertThat(recusaDe(compra(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, null, "SUDESTE")))
                    .isEqualTo(Codigo.NIVEL_CLUBE_INVALIDO);
            assertThat(recusaDe(compra(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "ouro", "SUDESTE")))
                    .isEqualTo(Codigo.NIVEL_CLUBE_INVALIDO);
        }

        @Test
        void regiao_que_nao_existe_ou_nao_informada() {
            assertThat(recusaDe(compra(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDOESTE")))
                    .isEqualTo(Codigo.REGIAO_INVALIDA);
            assertThat(recusaDe(compra(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "BRONZE", null)))
                    .isEqualTo(Codigo.REGIAO_INVALIDA);
        }

        @Test
        void modalidade_de_entrega_que_nao_existe_ou_nao_informada() {
            assertThat(recusaDe(compra(List.of(CAMISETA), "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(Codigo.MODALIDADE_INVALIDA);
            assertThat(recusaDe(compra(List.of(CAMISETA), null, null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(Codigo.MODALIDADE_INVALIDA);
        }

        @Test
        void cupom_que_nao_existe() {
            assertThat(recusaDe(compra(List.of(CAMISETA), "EXPRESSA", "PROMO99", "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(Codigo.CUPOM_INVALIDO);
            assertThat(recusaDe(compra(List.of(CAMISETA), "EXPRESSA", "bemvindo10", "PIX", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(Codigo.CUPOM_INVALIDO);
        }

        @Test
        void forma_de_pagamento_que_nao_existe_ou_nao_informada() {
            assertThat(recusaDe(compra(List.of(CAMISETA), "EXPRESSA", null, "DINHEIRO", 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(Codigo.FORMA_PAGAMENTO_INVALIDA);
            assertThat(recusaDe(compra(List.of(CAMISETA), "EXPRESSA", null, null, 1, "BRONZE", "SUDESTE")))
                    .isEqualTo(Codigo.FORMA_PAGAMENTO_INVALIDA);
        }

        @Test
        void o_primeiro_problema_encontrado_e_o_que_vale() {
            CompraRequest tudoErrado = compra(List.of(), "DRONE", "PROMO99", "DINHEIRO", 9, "DIAMANTE", "SUDOESTE");

            assertThat(recusaDe(tudoErrado)).isEqualTo(Codigo.PEDIDO_INVALIDO);
        }

        @Test
        void modalidade_indisponivel_vem_antes_do_cupom_invalido() {
            CompraRequest pesado = compra(List.of(item("Tapete", "100.00", 1, "9.00")),
                    "MOTOBOY", "PROMO99", "PIX", 1, "BRONZE", "SUDESTE");

            assertThat(recusaDe(pesado)).isEqualTo(Codigo.MODALIDADE_INDISPONIVEL);
        }

        @Test
        void cupom_nao_aplicavel_vem_antes_da_forma_de_pagamento_invalida() {
            CompraRequest barata = compra(List.of(item("Meia", "10.00", 1, "0.10")),
                    "RETIRADA_LOJA", "MENOS50", "DINHEIRO", 1, "BRONZE", "SUDESTE");

            assertThat(recusaDe(barata)).isEqualTo(Codigo.CUPOM_NAO_APLICAVEL);
        }

        @Test
        void parcelamento_invalido_vem_antes_da_forma_de_pagamento_indisponivel() {
            CompraRequest caraEParcelada = compra(List.of(item("Casaco", "2000.00", 1, "1.00")),
                    "RETIRADA_LOJA", null, "BOLETO", 3, "BRONZE", "SUDESTE");

            assertThat(recusaDe(caraEParcelada)).isEqualTo(Codigo.PARCELAMENTO_INVALIDO);
        }
    }

    private ResumoResponse calcular(String entrega, String cupom, String pagamento,
                                    Integer parcelas, String clube, String regiao) {
        return calculadora.calcular(
                compra(List.of(CAMISETA, TENIS), entrega, cupom, pagamento, parcelas, clube, regiao));
    }

    private Codigo recusaDe(CompraRequest compra) {
        return catchRecusa(compra).codigo();
    }

    private PedidoRecusado catchRecusa(CompraRequest compra) {
        return catchThrowableOfType(PedidoRecusado.class, () -> calculadora.calcular(compra));
    }

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }
}
