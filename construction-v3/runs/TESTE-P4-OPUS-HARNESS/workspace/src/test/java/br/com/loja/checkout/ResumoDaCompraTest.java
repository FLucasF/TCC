package br.com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.loja.checkout.api.ItemRequest;
import br.com.loja.checkout.api.ResumoRequest;
import br.com.loja.checkout.api.ResumoResponse;
import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.ErroCheckout;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ResumoDaCompraTest {

    private static final ItemRequest CAMISETA = new ItemRequest("Camiseta", valor("79.90"), 2, valor("0.30"));
    private static final ItemRequest TENIS = new ItemRequest("Tenis", valor("249.90"), 1, valor("1.20"));
    private static final ItemRequest FONE = new ItemRequest("Fone", valor("199.90"), 2, valor("0.25"));
    private static final ItemRequest MEIA = new ItemRequest("Meia", valor("19.90"), 7, valor("0.10"));

    @Autowired
    private CalculadoraResumo calculadora;

    private static BigDecimal valor(String valor) {
        return new BigDecimal(valor);
    }

    private static Pedido pedido(ItemRequest... itens) {
        return new Pedido(List.of(itens));
    }

    /** Requisicao de exemplo, com os campos obrigatorios preenchidos. */
    private record Pedido(List<ItemRequest> itens) {

        ResumoRequest comEntrega(String modalidade, String cupom, String pagamento, Integer parcelas) {
            return new ResumoRequest(itens, modalidade, cupom, pagamento, parcelas, "BRONZE", "SUDESTE");
        }
    }

    @Nested
    @DisplayName("exemplos conferidos pelo financeiro")
    class Exemplos {

        @Test
        void exemplo1_expressa_com_bemvindo10_no_pix() {
            ResumoResponse resumo = calculadora.calcular(
                    pedido(CAMISETA, TENIS).comEntrega("EXPRESSA", "BEMVINDO10", "PIX", 1));

            assertThat(resumo.subtotalProdutos()).isEqualTo(valor("409.70"));
            assertThat(resumo.descontoCupom()).isEqualTo(valor("40.97"));
            assertThat(resumo.frete()).isEqualTo(valor("33.10"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
            assertThat(resumo.imposto()).isEqualTo(valor("44.25"));
            assertThat(resumo.ajustePagamento()).isEqualTo(valor("-22.30"));
            assertThat(resumo.totalFinal()).isEqualTo(valor("423.78"));
            assertThat(resumo.parcelas()).isEqualTo(1);
            assertThat(resumo.valorParcela()).isEqualTo(valor("423.78"));
            assertThat(resumo.creditoProximaCompra()).isEqualTo(valor("0.00"));
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void exemplo2_economica_sem_cupom_no_cartao_em_6x() {
            ResumoResponse resumo = calculadora.calcular(
                    pedido(CAMISETA, TENIS).comEntrega("ECONOMICA", null, "CARTAO", 6));

            assertThat(resumo.subtotalProdutos()).isEqualTo(valor("409.70"));
            assertThat(resumo.descontoCupom()).isEqualTo(valor("0.00"));
            assertThat(resumo.frete()).isEqualTo(valor("15.60"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
            assertThat(resumo.imposto()).isEqualTo(valor("49.16"));
            assertThat(resumo.ajustePagamento()).isEqualTo(valor("33.56"));
            assertThat(resumo.totalFinal()).isEqualTo(valor("508.02"));
            assertThat(resumo.parcelas()).isEqualTo(6);
            assertThat(resumo.valorParcela()).isEqualTo(valor("84.67"));
        }

        @Test
        void exemplo3_motoboy_com_menos50_no_boleto() {
            ResumoResponse resumo = calculadora.calcular(
                    pedido(FONE).comEntrega("MOTOBOY", "MENOS50", "BOLETO", null));

            assertThat(resumo.subtotalProdutos()).isEqualTo(valor("399.80"));
            assertThat(resumo.descontoCupom()).isEqualTo(valor("50.00"));
            assertThat(resumo.frete()).isEqualTo(valor("18.00"));
            assertThat(resumo.prazoEntregaDias()).isZero();
            assertThat(resumo.imposto()).isEqualTo(valor("41.98"));
            assertThat(resumo.ajustePagamento()).isEqualTo(valor("3.49"));
            assertThat(resumo.totalFinal()).isEqualTo(valor("413.27"));
            assertThat(resumo.parcelas()).isEqualTo(1);
            assertThat(resumo.valorParcela()).isEqualTo(valor("413.27"));
        }

        @Test
        void exemplo4_retirada_com_leve3pague2_no_cartao_em_3x() {
            ResumoResponse resumo = calculadora.calcular(
                    pedido(MEIA, CAMISETA).comEntrega("RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3));

            assertThat(resumo.subtotalProdutos()).isEqualTo(valor("299.10"));
            assertThat(resumo.descontoCupom()).isEqualTo(valor("39.80"));
            assertThat(resumo.frete()).isEqualTo(valor("0.00"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
            assertThat(resumo.imposto()).isEqualTo(valor("31.12"));
            assertThat(resumo.ajustePagamento()).isEqualTo(valor("0.00"));
            assertThat(resumo.totalFinal()).isEqualTo(valor("290.42"));
            assertThat(resumo.parcelas()).isEqualTo(3);
            assertThat(resumo.valorParcela()).isEqualTo(valor("96.81"));
        }

        @Test
        void exemplo5_expressa_no_pix_para_cliente_ouro_do_sudeste() {
            ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                    List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

            assertThat(resumo.subtotalProdutos()).isEqualTo(valor("409.70"));
            assertThat(resumo.descontoCupom()).isEqualTo(valor("0.00"));
            assertThat(resumo.frete()).isEqualTo(valor("0.00"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
            assertThat(resumo.imposto()).isEqualTo(valor("49.16"));
            assertThat(resumo.ajustePagamento()).isEqualTo(valor("-22.94"));
            assertThat(resumo.totalFinal()).isEqualTo(valor("435.92"));
            assertThat(resumo.valorParcela()).isEqualTo(valor("435.92"));
            assertThat(resumo.creditoProximaCompra()).isEqualTo(valor("20.48"));
            assertThat(resumo.brinde()).isFalse();
        }
    }

    @Nested
    @DisplayName("entrega")
    class Entrega {

        @ParameterizedTest(name = "{0} cobra {1} com prazo de {2} dias")
        @CsvSource({
                "ECONOMICA,15.60,7",
                "EXPRESSA,33.10,2",
                "RETIRADA_LOJA,0.00,1",
                "MOTOBOY,18.00,0"
        })
        void cobra_e_prometa_o_prazo_de_cada_modalidade(String modalidade, String frete, int prazo) {
            ResumoResponse resumo = calculadora.calcular(
                    pedido(CAMISETA, TENIS).comEntrega(modalidade, null, "PIX", 1));

            assertThat(resumo.frete()).isEqualTo(valor(frete));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(prazo);
        }

        @Test
        void motoboy_atende_exatamente_5kg() {
            ItemRequest tijolo = new ItemRequest("Tijolo", valor("10.00"), 5, valor("1.00"));

            ResumoResponse resumo = calculadora.calcular(pedido(tijolo).comEntrega("MOTOBOY", null, "PIX", 1));

            assertThat(resumo.frete()).isEqualTo(valor("18.00"));
        }

        @Test
        void motoboy_nao_atende_acima_de_5kg() {
            ItemRequest tijolo = new ItemRequest("Tijolo", valor("10.00"), 5, valor("1.01"));

            assertThatThrownBy(() -> calculadora.calcular(pedido(tijolo).comEntrega("MOTOBOY", null, "PIX", 1)))
                    .isInstanceOf(ErroCheckout.class)
                    .extracting(erro -> ((ErroCheckout) erro).codigo())
                    .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
    }

    @Nested
    @DisplayName("cupons")
    class Cupons {

        @Test
        void bemvindo10_desconta_10_por_cento_dos_produtos() {
            ResumoResponse resumo = calculadora.calcular(
                    pedido(CAMISETA).comEntrega("RETIRADA_LOJA", "BEMVINDO10", "CARTAO", 1));

            assertThat(resumo.descontoCupom()).isEqualTo(valor("15.98"));
        }

        @Test
        void menos50_exige_300_em_produtos() {
            assertThatThrownBy(() -> calculadora.calcular(
                    pedido(CAMISETA).comEntrega("RETIRADA_LOJA", "MENOS50", "CARTAO", 1)))
                    .isInstanceOf(ErroCheckout.class)
                    .extracting(erro -> ((ErroCheckout) erro).codigo())
                    .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        @Test
        void menos50_vale_a_partir_de_300_em_produtos() {
            ItemRequest bolsa = new ItemRequest("Bolsa", valor("300.00"), 1, valor("0.50"));

            ResumoResponse resumo = calculadora.calcular(
                    pedido(bolsa).comEntrega("RETIRADA_LOJA", "MENOS50", "CARTAO", 1));

            assertThat(resumo.descontoCupom()).isEqualTo(valor("50.00"));
        }

        @Test
        void fretegratis_mostra_o_frete_e_desconta_o_mesmo_valor() {
            ResumoResponse resumo = calculadora.calcular(
                    pedido(CAMISETA, TENIS).comEntrega("EXPRESSA", "FRETEGRATIS", "CARTAO", 1));

            assertThat(resumo.frete()).isEqualTo(valor("33.10"));
            assertThat(resumo.descontoCupom()).isEqualTo(valor("33.10"));
        }

        @Test
        void leve3pague2_libera_uma_unidade_a_cada_tres_do_mesmo_item() {
            ResumoResponse resumo = calculadora.calcular(
                    pedido(MEIA, CAMISETA).comEntrega("RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 1));

            assertThat(resumo.descontoCupom()).isEqualTo(valor("39.80"));
        }
    }

    @Nested
    @DisplayName("clube da loja")
    class Clube {

        private ResumoResponse resumoDoNivel(String nivel, ItemRequest... itens) {
            return calculadora.calcular(
                    new ResumoRequest(List.of(itens), "EXPRESSA", null, "CARTAO", 1, nivel, "SUDESTE"));
        }

        @Test
        void bronze_nao_ganha_nada() {
            ResumoResponse resumo = resumoDoNivel("BRONZE", CAMISETA, TENIS);

            assertThat(resumo.creditoProximaCompra()).isEqualTo(valor("0.00"));
            assertThat(resumo.frete()).isEqualTo(valor("33.10"));
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void prata_ganha_2_por_cento_em_credito_e_paga_frete() {
            ResumoResponse resumo = resumoDoNivel("PRATA", CAMISETA, TENIS);

            assertThat(resumo.creditoProximaCompra()).isEqualTo(valor("8.19"));
            assertThat(resumo.frete()).isEqualTo(valor("33.10"));
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void ouro_ganha_5_por_cento_em_credito_e_nao_paga_frete() {
            ResumoResponse resumo = resumoDoNivel("OURO", CAMISETA, TENIS);

            assertThat(resumo.creditoProximaCompra()).isEqualTo(valor("20.48"));
            assertThat(resumo.frete()).isEqualTo(valor("0.00"));
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void ouro_ganha_brinde_acima_de_500_em_produtos() {
            ItemRequest casaco = new ItemRequest("Casaco", valor("250.05"), 2, valor("0.80"));

            ResumoResponse resumo = resumoDoNivel("OURO", casaco);

            assertThat(resumo.subtotalProdutos()).isEqualTo(valor("500.10"));
            assertThat(resumo.brinde()).isTrue();
        }

        @Test
        void ouro_nao_ganha_brinde_com_exatamente_500_em_produtos() {
            ItemRequest casaco = new ItemRequest("Casaco", valor("250.00"), 2, valor("0.80"));

            ResumoResponse resumo = resumoDoNivel("OURO", casaco);

            assertThat(resumo.subtotalProdutos()).isEqualTo(valor("500.00"));
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void fretegratis_com_ouro_nao_desconta_nada_porque_o_frete_ja_e_zero() {
            ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                    List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "OURO", "SUDESTE"));

            assertThat(resumo.frete()).isEqualTo(valor("0.00"));
            assertThat(resumo.descontoCupom()).isEqualTo(valor("0.00"));
        }
    }

    @Nested
    @DisplayName("imposto por regiao")
    class Imposto {

        @ParameterizedTest(name = "{0} cobra {1} de imposto")
        @CsvSource({
                "SUDESTE,44.25",
                "SUL,40.56",
                "CENTRO_OESTE,33.19",
                "NORTE,25.81",
                "NORDESTE,25.81"
        })
        void incide_sobre_os_produtos_ja_com_desconto(String regiao, String imposto) {
            ResumoResponse resumo = calculadora.calcular(new ResumoRequest(
                    List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", regiao));

            assertThat(resumo.imposto()).isEqualTo(valor(imposto));
        }
    }

    @Nested
    @DisplayName("pagamento")
    class Pagamento {

        private ResumoResponse resumoPagando(String forma, Integer parcelas) {
            return calculadora.calcular(pedido(CAMISETA).comEntrega("RETIRADA_LOJA", null, forma, parcelas));
        }

        @Test
        void pix_desconta_5_por_cento_do_total_do_pedido() {
            ResumoResponse resumo = resumoPagando("PIX", 1);

            assertThat(resumo.imposto()).isEqualTo(valor("19.18"));
            assertThat(resumo.ajustePagamento()).isEqualTo(valor("-8.95"));
            assertThat(resumo.totalFinal()).isEqualTo(valor("170.03"));
        }

        @Test
        void boleto_soma_a_tarifa_do_banco() {
            ResumoResponse resumo = resumoPagando("BOLETO", null);

            assertThat(resumo.ajustePagamento()).isEqualTo(valor("3.49"));
            assertThat(resumo.totalFinal()).isEqualTo(valor("182.47"));
            assertThat(resumo.parcelas()).isEqualTo(1);
        }

        @Test
        void cartao_em_ate_3x_nao_tem_juros() {
            ResumoResponse resumo = resumoPagando("CARTAO", 3);

            assertThat(resumo.ajustePagamento()).isEqualTo(valor("0.00"));
            assertThat(resumo.totalFinal()).isEqualTo(valor("178.98"));
            assertThat(resumo.valorParcela()).isEqualTo(valor("59.66"));
        }

        @Test
        void cartao_acima_de_3x_tem_juros_de_1_99_ao_mes() {
            ResumoResponse resumo = resumoPagando("CARTAO", 4);

            assertThat(resumo.valorParcela()).isEqualTo(valor("46.99"));
            assertThat(resumo.totalFinal()).isEqualTo(valor("187.96"));
            assertThat(resumo.ajustePagamento()).isEqualTo(valor("8.98"));
        }

        @Test
        void boleto_nao_atende_acima_de_1000_no_total_do_pedido() {
            ItemRequest sofa = new ItemRequest("Sofa", valor("1000.01"), 1, valor("1.00"));

            assertThatThrownBy(() -> calculadora.calcular(
                    pedido(sofa).comEntrega("RETIRADA_LOJA", null, "BOLETO", 1)))
                    .isInstanceOf(ErroCheckout.class)
                    .extracting(erro -> ((ErroCheckout) erro).codigo())
                    .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        @Test
        void boleto_atende_exatamente_1000_no_total_do_pedido() {
            ItemRequest sofa = new ItemRequest("Sofa", valor("1000.00"), 1, valor("1.00"));

            ResumoResponse resumo = calculadora.calcular(
                    pedido(sofa).comEntrega("RETIRADA_LOJA", null, "BOLETO", 1));

            assertThat(resumo.totalFinal()).isEqualTo(valor("1123.49"));
        }

        @Test
        void parcelas_ausente_conta_como_1() {
            ResumoResponse resumo = resumoPagando("CARTAO", null);

            assertThat(resumo.parcelas()).isEqualTo(1);
            assertThat(resumo.valorParcela()).isEqualTo(resumo.totalFinal());
        }
    }
}
