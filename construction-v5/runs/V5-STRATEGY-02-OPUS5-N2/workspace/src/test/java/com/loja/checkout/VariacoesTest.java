package com.loja.checkout;

import com.loja.checkout.aplicacao.CalculadoraResumo;
import com.loja.checkout.aplicacao.ResumoCompra;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.loja.checkout.PedidoDeTeste.item;
import static com.loja.checkout.PedidoDeTeste.um;
import static org.assertj.core.api.Assertions.assertThat;

class VariacoesTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    @Nested
    class Entrega {

        @ParameterizedTest
        @CsvSource({
                "ECONOMICA,15.60,7",
                "EXPRESSA,33.10,2",
                "RETIRADA_LOJA,0.00,1",
                "MOTOBOY,18.00,0"
        })
        void cobra_e_promete_por_modalidade(String modalidade, String frete, int prazo) {
            ResumoCompra resumo = calculadora.calcular(um()
                    .com(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20"))
                    .entrega(modalidade)
                    .pedido());

            assertThat(resumo.frete()).isEqualByComparingTo(frete);
            assertThat(resumo.prazoEntregaDias()).isEqualTo(prazo);
        }

        @Test
        void motoboy_atende_ate_cinco_quilos() {
            ResumoCompra resumo = calculadora.calcular(um()
                    .com(item("Mochila", "100.00", 2, "2.50"))
                    .entrega("MOTOBOY")
                    .pedido());

            assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        }
    }

    @Nested
    class Cupons {

        @Test
        void bemvindo10_desconta_dez_por_cento_dos_produtos() {
            assertThat(desconto(um().com(item("Camiseta", "79.90", 2, "0.30")).cupom("BEMVINDO10")))
                    .isEqualByComparingTo("15.98");
        }

        @Test
        void menos50_vale_a_partir_de_trezentos_em_produtos() {
            assertThat(desconto(um().com(item("Casaco", "300.00", 1, "0.80")).cupom("MENOS50")))
                    .isEqualByComparingTo("50.00");
        }

        @Test
        void fretegratis_desconta_o_proprio_frete() {
            ResumoCompra resumo = calculadora.calcular(um()
                    .com(item("Camiseta", "79.90", 2, "0.30"))
                    .entrega("ECONOMICA").cupom("FRETEGRATIS")
                    .pedido());

            assertThat(resumo.frete()).isEqualByComparingTo("13.20");
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("13.20");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("153.33");
        }

        @Test
        void fretegratis_para_ouro_nao_desconta_nada_porque_o_frete_ja_e_zero() {
            ResumoCompra resumo = calculadora.calcular(um()
                    .com(item("Camiseta", "79.90", 2, "0.30"))
                    .entrega("ECONOMICA").cupom("FRETEGRATIS").clube("OURO")
                    .pedido());

            assertThat(resumo.frete()).isEqualByComparingTo("0.00");
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        }

        @Test
        void leve3pague2_libera_uma_unidade_a_cada_tres_do_mesmo_item() {
            assertThat(desconto(um()
                    .com(item("Meia", "19.90", 6, "0.10"), item("Camiseta", "79.90", 2, "0.30"))
                    .cupom("LEVE3PAGUE2")))
                    .isEqualByComparingTo("39.80");
        }

        private java.math.BigDecimal desconto(PedidoDeTeste pedido) {
            return calculadora.calcular(pedido.pedido()).descontoCupom();
        }
    }

    @Nested
    class Clube {

        @ParameterizedTest
        @CsvSource({"BRONZE,0.00", "PRATA,3.20", "OURO,7.99"})
        void credita_de_volta_conforme_o_nivel(String nivel, String credito) {
            ResumoCompra resumo = calculadora.calcular(um()
                    .com(item("Camiseta", "79.90", 2, "0.30"))
                    .clube(nivel)
                    .pedido());

            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(credito);
        }

        @Test
        void ouro_nao_paga_frete() {
            ResumoCompra resumo = calculadora.calcular(um()
                    .com(item("Tenis", "249.90", 1, "1.20"))
                    .entrega("EXPRESSA").clube("OURO")
                    .pedido());

            assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        }

        @ParameterizedTest
        @CsvSource({"500.00,false", "500.01,true"})
        void ouro_ganha_brinde_quando_os_produtos_passam_de_quinhentos(String preco, boolean brinde) {
            ResumoCompra resumo = calculadora.calcular(um()
                    .com(item("Casaco", preco, 1, "0.80"))
                    .clube("OURO")
                    .pedido());

            assertThat(resumo.brinde()).isEqualTo(brinde);
        }

        @Test
        void prata_nao_ganha_brinde_nem_passando_de_quinhentos() {
            ResumoCompra resumo = calculadora.calcular(um()
                    .com(item("Casaco", "900.00", 1, "0.80"))
                    .clube("PRATA")
                    .pedido());

            assertThat(resumo.brinde()).isFalse();
        }
    }

    @Nested
    class Seguro {

        @ParameterizedTest
        @CsvSource({
                "SUDESTE,4.10",
                "SUL,4.10",
                "CENTRO_OESTE,6.15",
                "NORTE,10.24",
                "NORDESTE,8.19"
        })
        void cobra_por_regiao_sobre_os_produtos(String regiao, String seguro) {
            ResumoCompra resumo = calculadora.calcular(um()
                    .com(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20"))
                    .entrega("EXPRESSA").cupom("BEMVINDO10").regiao(regiao)
                    .pedido());

            assertThat(resumo.seguro()).isEqualByComparingTo(seguro);
        }
    }

    @Nested
    class Pagamento {

        @Test
        void pix_desconta_cinco_por_cento_do_total() {
            ResumoCompra resumo = pedidoDeMilEDez("PIX", null);

            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-50.50");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("959.50");
            assertThat(resumo.parcelas()).isEqualTo(1);
            assertThat(resumo.valorParcela()).isEqualByComparingTo("959.50");
        }

        @Test
        void boleto_soma_a_tarifa_do_banco() {
            ResumoCompra resumo = calculadora.calcular(um()
                    .com(item("Casaco", "500.00", 1, "1.00"))
                    .entrega("RETIRADA_LOJA").pagamento("BOLETO")
                    .pedido());

            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("508.49");
        }

        @ParameterizedTest
        @CsvSource({"1,1010.00", "2,505.00", "3,336.67"})
        void cartao_ate_tres_vezes_nao_tem_juros(int parcelas, String valorParcela) {
            ResumoCompra resumo = pedidoDeMilEDez("CARTAO", parcelas);

            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("1010.00");
            assertThat(resumo.parcelas()).isEqualTo(parcelas);
            assertThat(resumo.valorParcela()).isEqualByComparingTo(valorParcela);
        }

        @Test
        void cartao_de_quatro_vezes_em_diante_tem_juros_pela_tabela_price() {
            ResumoCompra resumo = pedidoDeMilEDez("CARTAO", 4);

            assertThat(resumo.valorParcela()).isEqualByComparingTo("265.19");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("1060.76");
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("50.76");
        }

        @Test
        void parcelas_ausentes_valem_uma_vez() {
            ResumoCompra resumo = pedidoDeMilEDez("CARTAO", null);

            assertThat(resumo.parcelas()).isEqualTo(1);
            assertThat(resumo.valorParcela()).isEqualByComparingTo("1010.00");
        }

        private ResumoCompra pedidoDeMilEDez(String formaPagamento, Integer parcelas) {
            return calculadora.calcular(um()
                    .com(item("Notebook", "1000.00", 1, "2.00"))
                    .entrega("RETIRADA_LOJA").pagamento(formaPagamento).parcelas(parcelas)
                    .pedido());
        }
    }
}
