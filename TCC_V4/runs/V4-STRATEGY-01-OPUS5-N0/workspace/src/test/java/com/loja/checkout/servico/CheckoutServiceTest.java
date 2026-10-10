package com.loja.checkout.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.api.dto.ItemRequest;
import com.loja.checkout.api.dto.ResumoCompraRequest;
import com.loja.checkout.api.dto.ResumoCompraResponse;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.clube.ClubeBronze;
import com.loja.checkout.dominio.clube.ClubeOuro;
import com.loja.checkout.dominio.clube.ClubePrata;
import com.loja.checkout.dominio.cupom.CupomBemvindo10;
import com.loja.checkout.dominio.cupom.CupomFreteGratis;
import com.loja.checkout.dominio.cupom.CupomLeve3Pague2;
import com.loja.checkout.dominio.cupom.CupomMenos50;
import com.loja.checkout.dominio.entrega.EntregaEconomica;
import com.loja.checkout.dominio.entrega.EntregaExpressa;
import com.loja.checkout.dominio.entrega.EntregaMotoboy;
import com.loja.checkout.dominio.entrega.RetiradaLoja;
import com.loja.checkout.dominio.pagamento.PagamentoBoleto;
import com.loja.checkout.dominio.pagamento.PagamentoCartao;
import com.loja.checkout.dominio.pagamento.PagamentoPix;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CheckoutServiceTest {

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    private final CheckoutService servico = new CheckoutService(
            new Catalogo<>(List.of(new EntregaEconomica(), new EntregaExpressa(),
                    new RetiradaLoja(), new EntregaMotoboy())),
            new Catalogo<>(List.of(new CupomBemvindo10(), new CupomMenos50(),
                    new CupomFreteGratis(), new CupomLeve3Pague2())),
            new Catalogo<>(List.of(new ClubeBronze(), new ClubePrata(), new ClubeOuro())),
            new Catalogo<>(List.of(new PagamentoPix(), new PagamentoCartao(), new PagamentoBoleto())));

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private static ResumoCompraRequest pedido(List<ItemRequest> itens, String entrega, String cupom,
                                              String pagamento, Integer parcelas, String clube, String regiao) {
        return new ResumoCompraRequest(itens, entrega, cupom, pagamento, parcelas, clube, regiao);
    }

    private static BigDecimal reais(String valor) {
        return new BigDecimal(valor);
    }

    @Nested
    class ExemplosConferidosPeloFinanceiro {

        @Test
        void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
            ResumoCompraResponse resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                    "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

            assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
            assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("40.97"));
            assertThat(resumo.frete()).isEqualByComparingTo(reais("33.10"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
            assertThat(resumo.seguro()).isEqualByComparingTo(reais("10.24"));
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("-20.60"));
            assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("391.47"));
            assertThat(resumo.parcelas()).isEqualTo(1);
            assertThat(resumo.valorParcela()).isEqualByComparingTo(reais("391.47"));
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(reais("0.00"));
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void exemplo2_economica_sem_cupom_cartao6x_prata_centro_oeste() {
            ResumoCompraResponse resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                    "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

            assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
            assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("0.00"));
            assertThat(resumo.frete()).isEqualByComparingTo(reais("15.60"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
            assertThat(resumo.seguro()).isEqualByComparingTo(reais("6.15"));
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("30.55"));
            assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("462.00"));
            assertThat(resumo.parcelas()).isEqualTo(6);
            assertThat(resumo.valorParcela()).isEqualByComparingTo(reais("77.00"));
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(reais("8.19"));
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
            ResumoCompraResponse resumo = servico.calcular(pedido(List.of(item("Fone", "199.90", 2, "0.25")),
                    "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));

            assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(reais("399.80"));
            assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("50.00"));
            assertThat(resumo.frete()).isEqualByComparingTo(reais("18.00"));
            assertThat(resumo.prazoEntregaDias()).isZero();
            assertThat(resumo.seguro()).isEqualByComparingTo(reais("8.00"));
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("3.49"));
            assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("379.29"));
            assertThat(resumo.valorParcela()).isEqualByComparingTo(reais("379.29"));
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(reais("0.00"));
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void exemplo4_retirada_leve3pague2_cartao3x_prata_sul() {
            ResumoCompraResponse resumo = servico.calcular(pedido(
                    List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA),
                    "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

            assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(reais("299.10"));
            assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("39.80"));
            assertThat(resumo.frete()).isEqualByComparingTo(reais("0.00"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
            assertThat(resumo.seguro()).isEqualByComparingTo(reais("2.99"));
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("0.00"));
            assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("262.29"));
            assertThat(resumo.parcelas()).isEqualTo(3);
            assertThat(resumo.valorParcela()).isEqualByComparingTo(reais("87.43"));
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(reais("5.98"));
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
            ResumoCompraResponse resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                    "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

            assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
            assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("0.00"));
            assertThat(resumo.frete()).isEqualByComparingTo(reais("0.00"));
            assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
            assertThat(resumo.seguro()).isEqualByComparingTo(reais("4.10"));
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("-20.69"));
            assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("393.11"));
            assertThat(resumo.valorParcela()).isEqualByComparingTo(reais("393.11"));
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(reais("20.48"));
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void exemplo_do_anexo_expressa_bemvindo10_pix_ouro_sudeste() {
            ResumoCompraResponse resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                    "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"));

            assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
            assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("40.97"));
            assertThat(resumo.frete()).isEqualByComparingTo(reais("0.00"));
            assertThat(resumo.seguro()).isEqualByComparingTo(reais("4.10"));
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("-18.64"));
            assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("354.19"));
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(reais("20.48"));
            assertThat(resumo.brinde()).isFalse();
        }
    }

    @Nested
    class Entrega {

        @Test
        void motoboy_nao_atende_acima_de_5kg() {
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(item("Mala", "300.00", 2, "3.00")),
                    "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        @Test
        void motoboy_atende_exatamente_5kg() {
            ResumoCompraResponse resumo = servico.calcular(pedido(List.of(item("Mala", "100.00", 2, "2.50")),
                    "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.frete()).isEqualByComparingTo(reais("18.00"));
        }

        @Test
        void ouro_nao_paga_frete_mas_ainda_respeita_limite_do_motoboy() {
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(item("Mala", "300.00", 2, "3.00")),
                    "MOTOBOY", null, "PIX", 1, "OURO", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
    }

    @Nested
    class Cupons {

        @Test
        void fretegratis_desconta_exatamente_o_valor_do_frete() {
            ResumoCompraResponse resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                    "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.frete()).isEqualByComparingTo(reais("33.10"));
            assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("33.10"));
            // 409,70 - 33,10 + 33,10 + 4,10
            assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("413.80"));
        }

        @Test
        void fretegratis_com_ouro_nao_desconta_nada_porque_o_frete_ja_e_zero() {
            ResumoCompraResponse resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                    "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "OURO", "SUDESTE"));

            assertThat(resumo.frete()).isEqualByComparingTo(reais("0.00"));
            assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("0.00"));
        }

        @Test
        void menos50_abaixo_de_300_nao_e_aplicavel() {
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(CAMISETA),
                    "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        @Test
        void menos50_vale_a_partir_de_300_exatos() {
            ResumoCompraResponse resumo = servico.calcular(pedido(List.of(item("Vestido", "300.00", 1, "0.40")),
                    "RETIRADA_LOJA", "MENOS50", "CARTAO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("50.00"));
        }

        @Test
        void leve3pague2_sem_nenhum_trio_no_carrinho_nao_e_aplicavel() {
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(CAMISETA, TENIS),
                    "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", 1, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        @Test
        void leve3pague2_conta_um_gratis_a_cada_tres_do_mesmo_item() {
            ResumoCompraResponse resumo = servico.calcular(pedido(List.of(item("Meia", "10.00", 9, "0.10")),
                    "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.descontoCupom()).isEqualByComparingTo(reais("30.00"));
        }

        @Test
        void cupom_que_nao_existe_e_recusado() {
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(CAMISETA),
                    "EXPRESSA", "PROMOCAOQUALQUER", "PIX", 1, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.CUPOM_INVALIDO);
        }

        @Test
        void cupom_em_minusculas_nao_vale() {
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(CAMISETA),
                    "EXPRESSA", "bemvindo10", "PIX", 1, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.CUPOM_INVALIDO);
        }
    }

    @Nested
    class Clube {

        @Test
        void ouro_ganha_brinde_acima_de_500_em_produtos() {
            ResumoCompraResponse resumo = servico.calcular(pedido(List.of(item("Jaqueta", "600.00", 1, "0.80")),
                    "RETIRADA_LOJA", null, "CARTAO", 1, "OURO", "SUDESTE"));

            assertThat(resumo.brinde()).isTrue();
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(reais("30.00"));
        }

        @Test
        void ouro_com_exatamente_500_em_produtos_nao_ganha_brinde() {
            ResumoCompraResponse resumo = servico.calcular(pedido(List.of(item("Jaqueta", "500.00", 1, "0.80")),
                    "RETIRADA_LOJA", null, "CARTAO", 1, "OURO", "SUDESTE"));

            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void credito_e_sobre_os_produtos_sem_desconto_e_sem_frete() {
            ResumoCompraResponse resumo = servico.calcular(pedido(List.of(CAMISETA, TENIS),
                    "EXPRESSA", "BEMVINDO10", "CARTAO", 1, "PRATA", "SUDESTE"));

            // 2% de 409,70 = 8,194 -> 8,19
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(reais("8.19"));
        }
    }

    @Nested
    class Pagamento {

        @Test
        void cartao_sem_juros_divide_o_total_em_duas_vezes() {
            ResumoCompraResponse resumo = servico.calcular(pedido(List.of(item("Bolsa", "100.00", 1, "0.50")),
                    "RETIRADA_LOJA", null, "CARTAO", 2, "BRONZE", "SUDESTE"));

            // 100,00 + 1,00 de seguro = 101,00
            assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("101.00"));
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo(reais("0.00"));
            assertThat(resumo.valorParcela()).isEqualByComparingTo(reais("50.50"));
        }

        @Test
        void cartao_com_juros_cobra_parcela_price_e_total_e_parcela_vezes_parcelas() {
            ResumoCompraResponse resumo = servico.calcular(pedido(List.of(item("Bolsa", "1000.00", 1, "0.50")),
                    "RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE"));

            BigDecimal totalPedido = reais("1010.00");
            assertThat(resumo.valorParcela()).isEqualByComparingTo(reais("95.45"));
            assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("1145.40"));
            assertThat(resumo.ajustePagamento())
                    .isEqualByComparingTo(reais("1145.40").subtract(totalPedido));
        }

        @Test
        void pix_nao_parcela() {
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(CAMISETA),
                    "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void boleto_nao_parcela() {
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(CAMISETA),
                    "EXPRESSA", null, "BOLETO", 3, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void cartao_acima_de_12x_nao_e_permitido() {
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(CAMISETA),
                    "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void parcelas_zero_ou_negativo_nao_e_permitido() {
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(CAMISETA),
                    "EXPRESSA", null, "CARTAO", 0, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void parcelas_ausente_vale_1() {
            ResumoCompraResponse resumo = servico.calcular(pedido(List.of(CAMISETA),
                    "RETIRADA_LOJA", null, "CARTAO", null, "BRONZE", "SUDESTE"));

            assertThat(resumo.parcelas()).isEqualTo(1);
            assertThat(resumo.valorParcela()).isEqualByComparingTo(resumo.totalFinal());
        }

        @Test
        void boleto_acima_de_1000_de_total_nao_esta_disponivel() {
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(item("Sofa", "1200.00", 1, "1.00")),
                    "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        @Test
        void boleto_com_total_de_exatamente_1000_ainda_vale() {
            // 990,10 de produtos + 9,90 de seguro = 1.000,00 de total do pedido
            ResumoCompraResponse resumo = servico.calcular(pedido(List.of(item("Sofa", "990.10", 1, "1.00")),
                    "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.totalFinal()).isEqualByComparingTo(reais("1003.49"));
        }
    }

    @Nested
    class Seguro {

        @Test
        void arredondamento_e_meio_para_o_par() {
            // 1% de 299,50 = 2,995 -> 3,00 (meio para o par)
            ResumoCompraResponse acima = servico.calcular(pedido(List.of(item("Calca", "299.50", 1, "0.50")),
                    "RETIRADA_LOJA", null, "CARTAO", 1, "BRONZE", "SUDESTE"));
            assertThat(acima.seguro()).isEqualByComparingTo(reais("3.00"));

            // 1% de 298,50 = 2,985 -> 2,98 (meio para o par)
            ResumoCompraResponse abaixo = servico.calcular(pedido(List.of(item("Calca", "298.50", 1, "0.50")),
                    "RETIRADA_LOJA", null, "CARTAO", 1, "BRONZE", "SUDESTE"));
            assertThat(abaixo.seguro()).isEqualByComparingTo(reais("2.98"));
        }

        @Test
        void percentual_muda_por_regiao_e_a_conta_e_a_mesma() {
            assertThat(seguroDa("SUDESTE")).isEqualByComparingTo(reais("4.10"));
            assertThat(seguroDa("SUL")).isEqualByComparingTo(reais("4.10"));
            assertThat(seguroDa("CENTRO_OESTE")).isEqualByComparingTo(reais("6.15"));
            assertThat(seguroDa("NORTE")).isEqualByComparingTo(reais("10.24"));
            assertThat(seguroDa("NORDESTE")).isEqualByComparingTo(reais("8.19"));
        }

        private BigDecimal seguroDa(String regiao) {
            return servico.calcular(pedido(List.of(CAMISETA, TENIS),
                    "RETIRADA_LOJA", null, "CARTAO", 1, "BRONZE", regiao)).seguro();
        }
    }

    @Nested
    class Recusas {

        @Test
        void carrinho_vazio() {
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(),
                    "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void carrinho_ausente() {
            assertThatThrownBy(() -> servico.calcular(pedido(null,
                    "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void item_com_preco_zerado() {
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(item("Brinde", "0.00", 1, "0.10")),
                    "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void item_com_quantidade_negativa() {
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(item("Camiseta", "79.90", -1, "0.30")),
                    "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void item_sem_peso() {
            assertThatThrownBy(() -> servico.calcular(pedido(
                    List.of(new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, null)),
                    "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void nivel_de_clube_que_nao_existe() {
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(CAMISETA),
                    "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        @Test
        void regiao_ausente() {
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(CAMISETA),
                    "EXPRESSA", null, "PIX", 1, "BRONZE", null)))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.REGIAO_INVALIDA);
        }

        @Test
        void modalidade_de_entrega_que_nao_existe() {
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(CAMISETA),
                    "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
        }

        @Test
        void forma_de_pagamento_que_nao_existe() {
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(CAMISETA),
                    "EXPRESSA", null, "CRIPTO", 1, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        @Test
        void a_ordem_de_verificacao_e_respeitada_quando_tem_varios_problemas() {
            // carrinho vazio, clube e regiao invalidos, modalidade invalida, cupom invalido,
            // pagamento invalido: vale o primeiro da lista.
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(),
                    "DRONE", "XPTO", "CRIPTO", 99, "DIAMANTE", "MARTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.PEDIDO_INVALIDO);

            // clube antes da regiao
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(CAMISETA),
                    "DRONE", "XPTO", "CRIPTO", 99, "DIAMANTE", "MARTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);

            // regiao antes da modalidade
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(CAMISETA),
                    "DRONE", "XPTO", "CRIPTO", 99, "BRONZE", "MARTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.REGIAO_INVALIDA);

            // modalidade indisponivel antes de cupom invalido
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(item("Mala", "300.00", 2, "3.00")),
                    "MOTOBOY", "XPTO", "CRIPTO", 99, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);

            // cupom nao aplicavel antes de forma de pagamento invalida
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(CAMISETA),
                    "EXPRESSA", "MENOS50", "CRIPTO", 99, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);

            // parcelamento invalido antes de forma de pagamento indisponivel
            assertThatThrownBy(() -> servico.calcular(pedido(List.of(item("Sofa", "1200.00", 1, "1.00")),
                    "RETIRADA_LOJA", null, "BOLETO", 2, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo").isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }
}
