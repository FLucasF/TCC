package com.loja.checkout;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.CheckoutInvalidoException;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.ResumoCompra;
import com.loja.checkout.dominio.cupom.CatalogoCupons;
import com.loja.checkout.dominio.cupom.CupomBemvindo10;
import com.loja.checkout.dominio.cupom.CupomFreteGratis;
import com.loja.checkout.dominio.cupom.CupomLeve3Pague2;
import com.loja.checkout.dominio.cupom.CupomMenos50;
import com.loja.checkout.dominio.entrega.CatalogoEntregas;
import com.loja.checkout.dominio.entrega.EntregaEconomica;
import com.loja.checkout.dominio.entrega.EntregaExpressa;
import com.loja.checkout.dominio.entrega.EntregaMotoboy;
import com.loja.checkout.dominio.entrega.RetiradaLoja;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static com.loja.checkout.Cenarios.CAMISETA;
import static com.loja.checkout.Cenarios.FONE;
import static com.loja.checkout.Cenarios.MEIA;
import static com.loja.checkout.Cenarios.TENIS;
import static com.loja.checkout.Cenarios.item;
import static com.loja.checkout.Cenarios.pedido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo(
            new CatalogoEntregas(List.of(new EntregaEconomica(), new EntregaExpressa(),
                    new RetiradaLoja(), new EntregaMotoboy())),
            new CatalogoCupons(List.of(new CupomBemvindo10(), new CupomMenos50(),
                    new CupomFreteGratis(), new CupomLeve3Pague2())));

    private ResumoCompra calcular(Cenarios.Pedido pedido) {
        return calculadora.calcular(pedido.solicitacao());
    }

    private void esperaErro(Cenarios.Pedido pedido, ErroCheckout erro) {
        assertThatThrownBy(() -> calcular(pedido))
                .isInstanceOf(CheckoutInvalidoException.class)
                .extracting(excecao -> ((CheckoutInvalidoException) excecao).erro())
                .isEqualTo(erro);
    }

    private static BigDecimal reais(String valor) {
        return new BigDecimal(valor);
    }

    @Nested
    class ExemplosDoFinanceiro {

        /**
         * Os exemplos 1 a 4 nao informam regiao e foram conferidos sem o imposto.
         * Aqui as partes conferidas por eles (produtos, cupom, frete e prazo) valem
         * exatamente, e o total segue a regra do enunciado, com o imposto do Sudeste.
         */
        @Test
        void exemplo1_expressa_bemvindo10_pix() {
            ResumoCompra resumo = calcular(pedido(CAMISETA, TENIS)
                    .entrega("EXPRESSA").cupom("BEMVINDO10").pagamento("PIX"));

            assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
            assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("40.97"));
            assertThat(resumo.frete()).isEqualByComparingTo(reais("33.10"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
            assertThat(resumo.imposto()).isEqualByComparingTo(reais("44.25"));
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("-22.30"));
            assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("423.78"));
            assertThat(resumo.parcelas()).isEqualTo(1);
            assertThat(resumo.valorParcela()).isEqualByComparingTo(reais("423.78"));
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(reais("0.00"));
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void exemplo2_economica_sem_cupom_cartao_6x() {
            ResumoCompra resumo = calcular(pedido(CAMISETA, TENIS)
                    .entrega("ECONOMICA").pagamento("CARTAO").parcelas(6));

            assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
            assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("0.00"));
            assertThat(resumo.frete()).isEqualByComparingTo(reais("15.60"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
            assertThat(resumo.imposto()).isEqualByComparingTo(reais("49.16"));
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("33.56"));
            assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("508.02"));
            assertThat(resumo.parcelas()).isEqualTo(6);
            assertThat(resumo.valorParcela()).isEqualByComparingTo(reais("84.67"));
        }

        @Test
        void exemplo3_motoboy_menos50_boleto() {
            ResumoCompra resumo = calcular(pedido(FONE)
                    .entrega("MOTOBOY").cupom("MENOS50").pagamento("BOLETO"));

            assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(reais("399.80"));
            assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("50.00"));
            assertThat(resumo.frete()).isEqualByComparingTo(reais("18.00"));
            assertThat(resumo.prazoEntregaDias()).isZero();
            assertThat(resumo.imposto()).isEqualByComparingTo(reais("41.98"));
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("3.49"));
            assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("413.27"));
            assertThat(resumo.valorParcela()).isEqualByComparingTo(reais("413.27"));
        }

        @Test
        void exemplo4_retirada_leve3pague2_cartao_3x() {
            ResumoCompra resumo = calcular(pedido(MEIA, CAMISETA)
                    .entrega("RETIRADA_LOJA").cupom("LEVE3PAGUE2").pagamento("CARTAO").parcelas(3));

            assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(reais("299.10"));
            assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("39.80"));
            assertThat(resumo.frete()).isEqualByComparingTo(reais("0.00"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
            assertThat(resumo.imposto()).isEqualByComparingTo(reais("31.12"));
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("0.00"));
            assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("290.42"));
            assertThat(resumo.parcelas()).isEqualTo(3);
            assertThat(resumo.valorParcela()).isEqualByComparingTo(reais("96.81"));
        }

        /** Unico exemplo com regiao e clube; confere com o enunciado em todos os campos. */
        @Test
        void exemplo5_ouro_sudeste_expressa_pix() {
            ResumoCompra resumo = calcular(pedido(CAMISETA, TENIS)
                    .entrega("EXPRESSA").pagamento("PIX").clube("OURO").regiao("SUDESTE"));

            assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
            assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("0.00"));
            assertThat(resumo.frete()).isEqualByComparingTo(reais("0.00"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
            assertThat(resumo.imposto()).isEqualByComparingTo(reais("49.16"));
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("-22.94"));
            assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("435.92"));
            assertThat(resumo.parcelas()).isEqualTo(1);
            assertThat(resumo.valorParcela()).isEqualByComparingTo(reais("435.92"));
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(reais("20.48"));
            assertThat(resumo.brinde()).isFalse();
        }
    }

    @Nested
    class Entrega {

        @Test
        void economica_cobra_por_quilo_do_pedido() {
            ResumoCompra resumo = calcular(pedido(CAMISETA, TENIS).entrega("ECONOMICA"));

            assertThat(resumo.frete()).isEqualByComparingTo(reais("15.60"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        }

        @Test
        void retirada_na_loja_e_gratis() {
            ResumoCompra resumo = calcular(pedido(CAMISETA).entrega("RETIRADA_LOJA"));

            assertThat(resumo.frete()).isEqualByComparingTo(reais("0.00"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        }

        @Test
        void motoboy_atende_ate_cinco_quilos() {
            ResumoCompra resumo = calcular(pedido(item("Halter", "100.00", 5, "1.00")).entrega("MOTOBOY"));

            assertThat(resumo.frete()).isEqualByComparingTo(reais("18.00"));
        }

        @Test
        void motoboy_nao_atende_acima_de_cinco_quilos() {
            esperaErro(pedido(item("Halter", "100.00", 5, "1.01")).entrega("MOTOBOY"),
                    ErroCheckout.MODALIDADE_INDISPONIVEL);
        }
    }

    @Nested
    class Cupons {

        @Test
        void menos50_exige_trezentos_reais_em_produtos() {
            esperaErro(pedido(item("Meia", "299.99", 1, "0.10")).cupom("MENOS50"),
                    ErroCheckout.CUPOM_NAO_APLICAVEL);
            assertThat(calcular(pedido(item("Meia", "300.00", 1, "0.10")).cupom("MENOS50")).descontoCupom())
                    .isEqualByComparingTo(reais("50.00"));
        }

        @Test
        void fretegratis_mostra_o_frete_e_desconta_o_mesmo_valor() {
            ResumoCompra resumo = calcular(pedido(CAMISETA, TENIS)
                    .entrega("EXPRESSA").cupom("FRETEGRATIS"));

            assertThat(resumo.frete()).isEqualByComparingTo(reais("33.10"));
            assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("33.10"));
        }

        @Test
        void leve3pague2_conta_por_item_do_carrinho() {
            ResumoCompra resumo = calcular(pedido(MEIA, CAMISETA).cupom("LEVE3PAGUE2"));

            assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("39.80"));
        }

        @Test
        void so_vale_cupom_conhecido() {
            esperaErro(pedido(CAMISETA).cupom("bemvindo10"), ErroCheckout.CUPOM_INVALIDO);
            esperaErro(pedido(CAMISETA).cupom("NATAL99"), ErroCheckout.CUPOM_INVALIDO);
        }

        @Test
        void pedido_sem_cupom_nao_tem_desconto() {
            assertThat(calcular(pedido(CAMISETA)).descontoCupom()).isEqualByComparingTo(reais("0.00"));
        }
    }

    @Nested
    class Clube {

        @Test
        void bronze_nao_ganha_nada() {
            ResumoCompra resumo = calcular(pedido(CAMISETA, TENIS).entrega("EXPRESSA").clube("BRONZE"));

            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(reais("0.00"));
            assertThat(resumo.frete()).isEqualByComparingTo(reais("33.10"));
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void prata_ganha_dois_por_cento_em_credito_e_paga_frete() {
            ResumoCompra resumo = calcular(pedido(CAMISETA, TENIS).entrega("EXPRESSA").clube("PRATA"));

            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(reais("8.19"));
            assertThat(resumo.frete()).isEqualByComparingTo(reais("33.10"));
        }

        @Test
        void ouro_ganha_brinde_acima_de_quinhentos_reais_em_produtos() {
            assertThat(calcular(pedido(item("Casaco", "500.00", 1, "0.80")).clube("OURO")).brinde()).isFalse();
            assertThat(calcular(pedido(item("Casaco", "500.01", 1, "0.80")).clube("OURO")).brinde()).isTrue();
        }

        @Test
        void credito_e_sobre_os_produtos_sem_desconto_e_sem_frete() {
            ResumoCompra resumo = calcular(pedido(CAMISETA, TENIS)
                    .entrega("EXPRESSA").cupom("BEMVINDO10").clube("OURO"));

            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(reais("20.48"));
        }
    }

    @Nested
    class Pagamento {

        @Test
        void pix_desconta_cinco_por_cento_do_total_do_pedido() {
            ResumoCompra resumo = calcular(pedido(item("Bone", "100.00", 1, "0.10")).pagamento("PIX"));

            assertThat(resumo.imposto()).isEqualByComparingTo(reais("12.00"));
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("-5.60"));
            assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("106.40"));
        }

        @Test
        void cartao_ate_tres_vezes_nao_tem_juros() {
            ResumoCompra resumo = calcular(pedido(item("Bone", "100.00", 1, "0.10"))
                    .pagamento("CARTAO").parcelas(3));

            assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("112.00"));
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("0.00"));
            assertThat(resumo.valorParcela()).isEqualByComparingTo(reais("37.33"));
        }

        @Test
        void cartao_de_quatro_a_doze_vezes_tem_juros() {
            ResumoCompra resumo = calcular(pedido(item("Bone", "100.00", 1, "0.10"))
                    .pagamento("CARTAO").parcelas(4));

            assertThat(resumo.valorParcela()).isEqualByComparingTo(reais("29.41"));
            assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("117.64"));
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("5.64"));
        }

        @Test
        void boleto_soma_a_tarifa_do_banco() {
            ResumoCompra resumo = calcular(pedido(item("Bone", "100.00", 1, "0.10")).pagamento("BOLETO"));

            assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("3.49"));
            assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("115.49"));
        }

        @Test
        void boleto_nao_vale_acima_de_mil_reais_sem_o_imposto() {
            esperaErro(pedido(item("Casaco", "1000.01", 1, "0.80")).pagamento("BOLETO"),
                    ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
            assertThat(calcular(pedido(item("Casaco", "1000.00", 1, "0.80")).pagamento("BOLETO")).totalFinal())
                    .isEqualByComparingTo(reais("1123.49"));
        }

        @Test
        void sem_parcelas_informadas_considera_uma() {
            assertThat(calcular(pedido(CAMISETA).pagamento("CARTAO")).parcelas()).isEqualTo(1);
        }

        @Test
        void pix_e_boleto_so_a_vista_e_cartao_de_um_a_doze() {
            esperaErro(pedido(CAMISETA).pagamento("PIX").parcelas(2), ErroCheckout.PARCELAMENTO_INVALIDO);
            esperaErro(pedido(CAMISETA).pagamento("BOLETO").parcelas(2), ErroCheckout.PARCELAMENTO_INVALIDO);
            esperaErro(pedido(CAMISETA).pagamento("CARTAO").parcelas(13), ErroCheckout.PARCELAMENTO_INVALIDO);
            esperaErro(pedido(CAMISETA).pagamento("CARTAO").parcelas(0), ErroCheckout.PARCELAMENTO_INVALIDO);
        }
    }

    @Nested
    class Imposto {

        @Test
        void cada_regiao_tem_sua_porcentagem_sobre_os_produtos_com_desconto() {
            assertThat(calcular(pedido(item("Bone", "200.00", 1, "0.10")).regiao("SUL")).imposto())
                    .isEqualByComparingTo(reais("22.00"));
            assertThat(calcular(pedido(item("Bone", "200.00", 1, "0.10")).regiao("CENTRO_OESTE")).imposto())
                    .isEqualByComparingTo(reais("18.00"));
            assertThat(calcular(pedido(item("Bone", "200.00", 1, "0.10")).regiao("NORTE")).imposto())
                    .isEqualByComparingTo(reais("14.00"));
            assertThat(calcular(pedido(item("Bone", "200.00", 1, "0.10")).regiao("NORDESTE")).imposto())
                    .isEqualByComparingTo(reais("14.00"));
        }

        @Test
        void o_frete_nao_entra_na_base_do_imposto() {
            ResumoCompra resumo = calcular(pedido(item("Bone", "200.00", 1, "0.10")).entrega("MOTOBOY"));

            assertThat(resumo.imposto()).isEqualByComparingTo(reais("24.00"));
        }
    }

    @Nested
    class Erros {

        @Test
        void erros_saem_na_ordem_combinada() {
            Cenarios.Pedido tudoErrado = new Cenarios.Pedido(List.of(item("X", "0.00", 0, "0.00")))
                    .clube("VIP").regiao("LESTE").entrega("DRONE").cupom("NATAL99")
                    .pagamento("CHEQUE").parcelas(99);

            esperaErro(tudoErrado, ErroCheckout.PEDIDO_INVALIDO);
            esperaErro(tudoErrado.itens(CAMISETA), ErroCheckout.NIVEL_CLUBE_INVALIDO);
            esperaErro(tudoErrado.itens(CAMISETA).clube("BRONZE"), ErroCheckout.REGIAO_INVALIDA);
            esperaErro(tudoErrado.itens(CAMISETA).clube("BRONZE").regiao("SUL"),
                    ErroCheckout.MODALIDADE_INVALIDA);
            esperaErro(tudoErrado.itens(CAMISETA).clube("BRONZE").regiao("SUL").entrega("RETIRADA_LOJA"),
                    ErroCheckout.CUPOM_INVALIDO);
            esperaErro(tudoErrado.itens(CAMISETA).clube("BRONZE").regiao("SUL")
                            .entrega("RETIRADA_LOJA").cupom("MENOS50"),
                    ErroCheckout.CUPOM_NAO_APLICAVEL);
            esperaErro(tudoErrado.itens(CAMISETA).clube("BRONZE").regiao("SUL")
                            .entrega("RETIRADA_LOJA").cupom("BEMVINDO10"),
                    ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
            esperaErro(tudoErrado.itens(CAMISETA).clube("BRONZE").regiao("SUL")
                            .entrega("RETIRADA_LOJA").cupom("BEMVINDO10").pagamento("CARTAO"),
                    ErroCheckout.PARCELAMENTO_INVALIDO);
        }

        @Test
        void modalidade_indisponivel_vem_antes_do_cupom_invalido() {
            esperaErro(pedido(item("Halter", "100.00", 6, "1.00")).entrega("MOTOBOY").cupom("NATAL99"),
                    ErroCheckout.MODALIDADE_INDISPONIVEL);
        }

        @Test
        void carrinho_vazio_ou_ausente_e_pedido_invalido() {
            esperaErro(new Cenarios.Pedido(List.of()), ErroCheckout.PEDIDO_INVALIDO);
            esperaErro(new Cenarios.Pedido(null), ErroCheckout.PEDIDO_INVALIDO);
        }

        @Test
        void item_com_valor_ausente_ou_nao_positivo_e_pedido_invalido() {
            List<Cenarios.Pedido> invalidos = Arrays.asList(
                    pedido(item("X", null, 1, "0.10")),
                    pedido(item("X", "0.00", 1, "0.10")),
                    pedido(item("X", "-1.00", 1, "0.10")),
                    pedido(item("X", "10.00", null, "0.10")),
                    pedido(item("X", "10.00", 0, "0.10")),
                    pedido(item("X", "10.00", -2, "0.10")),
                    pedido(item("X", "10.00", 1, null)),
                    pedido(item("X", "10.00", 1, "0.00")),
                    pedido(item("X", "10.00", 1, "-0.50")),
                    pedido(CAMISETA, item("X", "10.00", 1, "0.00")));

            invalidos.forEach(invalido -> esperaErro(invalido, ErroCheckout.PEDIDO_INVALIDO));
        }

        @Test
        void campo_ausente_tem_o_mesmo_erro_de_campo_desconhecido() {
            esperaErro(pedido(CAMISETA).clube(null), ErroCheckout.NIVEL_CLUBE_INVALIDO);
            esperaErro(pedido(CAMISETA).regiao(null), ErroCheckout.REGIAO_INVALIDA);
            esperaErro(pedido(CAMISETA).entrega(null), ErroCheckout.MODALIDADE_INVALIDA);
            esperaErro(pedido(CAMISETA).pagamento(null), ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
        }
    }

    @Test
    void todo_valor_do_resumo_sai_em_centavos() {
        ResumoCompra resumo = calcular(pedido(CAMISETA, TENIS)
                .entrega("EXPRESSA").cupom("BEMVINDO10").pagamento("CARTAO").parcelas(5).clube("OURO"));

        assertThat(List.of(resumo.subtotalProdutos(), resumo.descontoCupom(), resumo.frete(),
                        resumo.imposto(), resumo.ajustePagamento(), resumo.totalFinal(),
                        resumo.valorParcela(), resumo.creditoProximaCompra()))
                .allMatch(valor -> valor.scale() == 2);
    }
}
