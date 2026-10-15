package com.loja.checkout;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.comum.CheckoutException;
import com.loja.checkout.comum.CodigoErro;
import com.loja.checkout.dominio.clube.CatalogoNiveisClube;
import com.loja.checkout.dominio.clube.ClubeBronze;
import com.loja.checkout.dominio.clube.ClubeOuro;
import com.loja.checkout.dominio.clube.ClubePrata;
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
import com.loja.checkout.dominio.pagamento.CatalogoFormasPagamento;
import com.loja.checkout.dominio.pagamento.PagamentoBoleto;
import com.loja.checkout.dominio.pagamento.PagamentoCartao;
import com.loja.checkout.dominio.pagamento.PagamentoPix;
import com.loja.checkout.servico.CheckoutService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutServiceTest {

    private final CheckoutService servico = new CheckoutService(
            new CatalogoEntregas(List.of(new EntregaEconomica(), new EntregaExpressa(),
                    new RetiradaLoja(), new EntregaMotoboy())),
            new CatalogoCupons(List.of(new CupomBemvindo10(), new CupomMenos50(),
                    new CupomFreteGratis(), new CupomLeve3Pague2())),
            new CatalogoNiveisClube(List.of(new ClubeBronze(), new ClubePrata(), new ClubeOuro())),
            new CatalogoFormasPagamento(List.of(new PagamentoPix(), new PagamentoCartao(),
                    new PagamentoBoleto())));

    private static final ItemRequest CAMISETA =
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    private static final ItemRequest TENIS =
            new ItemRequest("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));

    private static ResumoRequest pedido(List<ItemRequest> itens, String entrega, String cupom,
                                        String pagamento, Integer parcelas, String clube,
                                        String regiao) {
        return new ResumoRequest(itens, entrega, cupom, pagamento, parcelas, clube, regiao);
    }

    private static void assertValores(ResumoResponse resumo, String subtotal, String cupom,
                                      String frete, int prazo, String seguro, String ajuste,
                                      String total, int parcelas, String valorParcela,
                                      String credito, boolean brinde) {
        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo(subtotal);
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(cupom);
        assertThat(resumo.frete()).isEqualByComparingTo(frete);
        assertThat(resumo.prazoEntregaDias()).isEqualTo(prazo);
        assertThat(resumo.seguro()).isEqualByComparingTo(seguro);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo(ajuste);
        assertThat(resumo.totalFinal()).isEqualByComparingTo(total);
        assertThat(resumo.parcelas()).isEqualTo(parcelas);
        assertThat(resumo.valorParcela()).isEqualByComparingTo(valorParcela);
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo(credito);
        assertThat(resumo.brinde()).isEqualTo(brinde);
    }

    @Nested
    @DisplayName("Exemplos conferidos pelo financeiro")
    class ExemplosDoFinanceiro {

        @Test
        @DisplayName("Exemplo da tabela: EXPRESSA, BEMVINDO10, PIX, OURO, SUDESTE")
        void exemploDaTabela() {
            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(CAMISETA, TENIS),
                    "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"));

            assertValores(resumo, "409.70", "40.97", "0.00", 2, "4.10", "-18.64",
                    "354.19", 1, "354.19", "20.48", false);
        }

        @Test
        @DisplayName("Exemplo 1: EXPRESSA, BEMVINDO10, PIX, BRONZE, NORTE")
        void exemplo1() {
            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(CAMISETA, TENIS),
                    "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

            assertValores(resumo, "409.70", "40.97", "33.10", 2, "10.24", "-20.60",
                    "391.47", 1, "391.47", "0.00", false);
        }

        @Test
        @DisplayName("Exemplo 2: ECONOMICA, sem cupom, CARTAO 6x, PRATA, CENTRO_OESTE")
        void exemplo2() {
            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(CAMISETA, TENIS),
                    "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

            assertValores(resumo, "409.70", "0.00", "15.60", 7, "6.15", "30.55",
                    "462.00", 6, "77.00", "8.19", false);
        }

        @Test
        @DisplayName("Exemplo 3: MOTOBOY, MENOS50, BOLETO, BRONZE, NORDESTE")
        void exemplo3() {
            ItemRequest fone = new ItemRequest("Fone", new BigDecimal("199.90"), 2,
                    new BigDecimal("0.25"));

            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(fone),
                    "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));

            assertValores(resumo, "399.80", "50.00", "18.00", 0, "8.00", "3.49",
                    "379.29", 1, "379.29", "0.00", false);
        }

        @Test
        @DisplayName("Exemplo 4: RETIRADA_LOJA, LEVE3PAGUE2, CARTAO 3x, PRATA, SUL")
        void exemplo4() {
            ItemRequest meia = new ItemRequest("Meia", new BigDecimal("19.90"), 7,
                    new BigDecimal("0.10"));

            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(meia, CAMISETA),
                    "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

            assertValores(resumo, "299.10", "39.80", "0.00", 1, "2.99", "0.00",
                    "262.29", 3, "87.43", "5.98", false);
        }

        @Test
        @DisplayName("Exemplo 5: EXPRESSA, sem cupom, PIX, OURO, SUDESTE")
        void exemplo5() {
            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(CAMISETA, TENIS),
                    "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

            assertValores(resumo, "409.70", "0.00", "0.00", 2, "4.10", "-20.69",
                    "393.11", 1, "393.11", "20.48", false);
        }
    }

    @Nested
    @DisplayName("Entrega")
    class Entrega {

        @Test
        @DisplayName("economica cobra fixo mais o peso do pedido")
        void economica() {
            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(TENIS),
                    "ECONOMICA", null, "CARTAO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.frete()).isEqualByComparingTo("14.40");
            assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        }

        @Test
        @DisplayName("retirada na loja e gratis e sai em 1 dia")
        void retiradaLoja() {
            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(TENIS),
                    "RETIRADA_LOJA", null, "CARTAO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.frete()).isEqualByComparingTo("0.00");
            assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        }

        @Test
        @DisplayName("motoboy atende pedido de exatamente 5 kg")
        void motoboyNoLimite() {
            ItemRequest pesado = new ItemRequest("Mala", new BigDecimal("100.00"), 5,
                    new BigDecimal("1.00"));

            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(pesado),
                    "MOTOBOY", null, "CARTAO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.frete()).isEqualByComparingTo("18.00");
            assertThat(resumo.prazoEntregaDias()).isZero();
        }

        @Test
        @DisplayName("motoboy nao atende pedido acima de 5 kg")
        void motoboyAcimaDoLimite() {
            ItemRequest pesado = new ItemRequest("Mala", new BigDecimal("100.00"), 6,
                    new BigDecimal("1.00"));

            assertThatThrownBy(() -> servico.calcularResumo(pedido(List.of(pesado),
                    "MOTOBOY", null, "CARTAO", 1, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting(erro -> ((CheckoutException) erro).getCodigo())
                    .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
    }

    @Nested
    @DisplayName("Cupons")
    class Cupons {

        @Test
        @DisplayName("FRETEGRATIS deixa o desconto igual ao frete")
        void freteGratis() {
            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(CAMISETA, TENIS),
                    "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.frete()).isEqualByComparingTo("33.10");
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
            // 409.70 - 33.10 + 33.10 + 4.10 de seguro
            assertThat(resumo.totalFinal()).isEqualByComparingTo("413.80");
        }

        @Test
        @DisplayName("FRETEGRATIS para OURO nao tem frete para descontar")
        void freteGratisComOuro() {
            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(CAMISETA, TENIS),
                    "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "OURO", "SUDESTE"));

            assertThat(resumo.frete()).isEqualByComparingTo("0.00");
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("LEVE3PAGUE2 conta os grupos de 3 item por item")
        void leve3Pague2() {
            ItemRequest meia = new ItemRequest("Meia", new BigDecimal("10.00"), 6,
                    new BigDecimal("0.10"));
            ItemRequest bone = new ItemRequest("Bone", new BigDecimal("30.00"), 3,
                    new BigDecimal("0.20"));

            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(meia, bone),
                    "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 1, "BRONZE", "SUDESTE"));

            // 2 meias de graca (20,00) e 1 bone de graca (30,00)
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("MENOS50 vale a partir de R$ 300,00 em produtos")
        void menos50NoLimite() {
            ItemRequest item = new ItemRequest("Jaqueta", new BigDecimal("300.00"), 1,
                    new BigDecimal("1.00"));

            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(item),
                    "RETIRADA_LOJA", "MENOS50", "CARTAO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("MENOS50 nao vale abaixo de R$ 300,00 em produtos")
        void menos50AbaixoDoMinimo() {
            ItemRequest item = new ItemRequest("Jaqueta", new BigDecimal("299.99"), 1,
                    new BigDecimal("1.00"));

            assertThatThrownBy(() -> servico.calcularResumo(pedido(List.of(item),
                    "RETIRADA_LOJA", "MENOS50", "CARTAO", 1, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting(erro -> ((CheckoutException) erro).getCodigo())
                    .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        @Test
        @DisplayName("cupom em minusculas nao existe")
        void cupomMinusculo() {
            assertThatThrownBy(() -> servico.calcularResumo(pedido(List.of(CAMISETA),
                    "RETIRADA_LOJA", "bemvindo10", "CARTAO", 1, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting(erro -> ((CheckoutException) erro).getCodigo())
                    .isEqualTo(CodigoErro.CUPOM_INVALIDO);
        }
    }

    @Nested
    @DisplayName("Clube da loja")
    class ClubeDaLoja {

        @Test
        @DisplayName("BRONZE nao ganha credito nem brinde")
        void bronze() {
            ItemRequest item = new ItemRequest("Casaco", new BigDecimal("600.00"), 1,
                    new BigDecimal("1.00"));

            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(item),
                    "RETIRADA_LOJA", null, "CARTAO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        @DisplayName("OURO acima de R$ 500,00 em produtos ganha brinde")
        void ouroComBrinde() {
            ItemRequest item = new ItemRequest("Casaco", new BigDecimal("600.00"), 1,
                    new BigDecimal("1.00"));

            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(item),
                    "EXPRESSA", null, "CARTAO", 1, "OURO", "SUDESTE"));

            assertThat(resumo.brinde()).isTrue();
            assertThat(resumo.frete()).isEqualByComparingTo("0.00");
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("30.00");
        }

        @Test
        @DisplayName("OURO com exatamente R$ 500,00 em produtos nao ganha brinde")
        void ouroNoLimiteDoBrinde() {
            ItemRequest item = new ItemRequest("Casaco", new BigDecimal("500.00"), 1,
                    new BigDecimal("1.00"));

            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(item),
                    "EXPRESSA", null, "CARTAO", 1, "OURO", "SUDESTE"));

            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        @DisplayName("o credito nao abate nada nesta compra")
        void creditoNaoAbate() {
            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(CAMISETA),
                    "RETIRADA_LOJA", null, "CARTAO", 1, "PRATA", "SUDESTE"));

            // 159,80 de produtos + 1,60 de seguro
            assertThat(resumo.totalFinal()).isEqualByComparingTo("161.40");
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("3.20");
        }
    }

    @Nested
    @DisplayName("Seguro por regiao")
    class SeguroPorRegiao {

        @Test
        @DisplayName("o percentual muda por regiao e incide so sobre os produtos")
        void percentualPorRegiao() {
            assertThat(seguroDe("SUDESTE")).isEqualByComparingTo("1.00");
            assertThat(seguroDe("SUL")).isEqualByComparingTo("1.00");
            assertThat(seguroDe("CENTRO_OESTE")).isEqualByComparingTo("1.50");
            assertThat(seguroDe("NORTE")).isEqualByComparingTo("2.50");
            assertThat(seguroDe("NORDESTE")).isEqualByComparingTo("2.00");
        }

        private BigDecimal seguroDe(String regiao) {
            ItemRequest item = new ItemRequest("Cinto", new BigDecimal("100.00"), 1,
                    new BigDecimal("0.20"));
            // com cupom e frete no pedido, para conferir que nao entram na base
            return servico.calcularResumo(pedido(List.of(item), "EXPRESSA", "BEMVINDO10",
                    "CARTAO", 1, "BRONZE", regiao)).seguro();
        }
    }

    @Nested
    @DisplayName("Pagamento")
    class Pagamento {

        @Test
        @DisplayName("cartao em 12x usa a tabela Price")
        void cartaoComJuros() {
            ItemRequest item = new ItemRequest("Relogio", new BigDecimal("1000.00"), 1,
                    new BigDecimal("0.50"));

            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(item),
                    "RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE"));

            // total do pedido: 1000,00 + 10,00 de seguro = 1010,00
            assertThat(resumo.valorParcela()).isEqualByComparingTo("95.45");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("1145.40");
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("135.40");
        }

        @Test
        @DisplayName("cartao em 3x nao tem juros")
        void cartaoSemJuros() {
            ItemRequest item = new ItemRequest("Relogio", new BigDecimal("1000.00"), 1,
                    new BigDecimal("0.50"));

            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(item),
                    "RETIRADA_LOJA", null, "CARTAO", 3, "BRONZE", "SUDESTE"));

            assertThat(resumo.totalFinal()).isEqualByComparingTo("1010.00");
            assertThat(resumo.valorParcela()).isEqualByComparingTo("336.67");
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("sem parcelas informadas o pedido e a vista")
        void parcelasAusentes() {
            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(CAMISETA),
                    "RETIRADA_LOJA", null, "CARTAO", null, "BRONZE", "SUDESTE"));

            assertThat(resumo.parcelas()).isEqualTo(1);
            assertThat(resumo.valorParcela()).isEqualByComparingTo(resumo.totalFinal());
        }

        @Test
        @DisplayName("boleto nao e aceito acima de R$ 1.000,00 de total")
        void boletoAcimaDoLimite() {
            ItemRequest item = new ItemRequest("Relogio", new BigDecimal("1000.00"), 1,
                    new BigDecimal("0.50"));

            assertThatThrownBy(() -> servico.calcularResumo(pedido(List.of(item),
                    "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting(erro -> ((CheckoutException) erro).getCodigo())
                    .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        @Test
        @DisplayName("boleto e aceito com total de exatamente R$ 1.000,00")
        void boletoNoLimite() {
            ItemRequest item = new ItemRequest("Relogio", new BigDecimal("990.10"), 1,
                    new BigDecimal("0.50"));

            ResumoResponse resumo = servico.calcularResumo(pedido(List.of(item),
                    "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"));

            assertThat(resumo.totalFinal()).isEqualByComparingTo("1003.49");
        }

        @Test
        @DisplayName("pix e boleto so a vista")
        void pixEBoletoSoAVista() {
            assertThatThrownBy(() -> servico.calcularResumo(pedido(List.of(CAMISETA),
                    "RETIRADA_LOJA", null, "PIX", 2, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting(erro -> ((CheckoutException) erro).getCodigo())
                    .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);

            assertThatThrownBy(() -> servico.calcularResumo(pedido(List.of(CAMISETA),
                    "RETIRADA_LOJA", null, "BOLETO", 3, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting(erro -> ((CheckoutException) erro).getCodigo())
                    .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        @DisplayName("cartao aceita de 1x a 12x")
        void cartaoAceitaDeUmADoze() {
            assertThatThrownBy(() -> servico.calcularResumo(pedido(List.of(CAMISETA),
                    "RETIRADA_LOJA", null, "CARTAO", 13, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting(erro -> ((CheckoutException) erro).getCodigo())
                    .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);

            assertThatThrownBy(() -> servico.calcularResumo(pedido(List.of(CAMISETA),
                    "RETIRADA_LOJA", null, "CARTAO", 0, "BRONZE", "SUDESTE")))
                    .isInstanceOf(CheckoutException.class)
                    .extracting(erro -> ((CheckoutException) erro).getCodigo())
                    .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    @Nested
    @DisplayName("Recusas, na ordem de conferencia")
    class Recusas {

        @Test
        @DisplayName("carrinho vazio ou ausente")
        void carrinhoVazio() {
            esperaErro(pedido(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.PEDIDO_INVALIDO);
            esperaErro(pedido(null, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        @DisplayName("item com preco, quantidade ou peso invalido")
        void itemInvalido() {
            esperaErro(pedido(List.of(new ItemRequest("X", new BigDecimal("0.00"), 1,
                            new BigDecimal("0.10"))),
                    "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
            esperaErro(pedido(List.of(new ItemRequest("X", new BigDecimal("10.00"), 0,
                            new BigDecimal("0.10"))),
                    "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
            esperaErro(pedido(List.of(new ItemRequest("X", new BigDecimal("10.00"), -1,
                            new BigDecimal("0.10"))),
                    "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
            esperaErro(pedido(List.of(new ItemRequest("X", new BigDecimal("10.00"), 1,
                            new BigDecimal("-0.10"))),
                    "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
            esperaErro(pedido(List.of(new ItemRequest("X", null, 1, null)),
                    "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
            esperaErro(pedido(Arrays.asList(CAMISETA, null),
                    "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        @DisplayName("nivel do clube inexistente ou ausente")
        void nivelClubeInvalido() {
            esperaErro(pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE"),
                    CodigoErro.NIVEL_CLUBE_INVALIDO);
            esperaErro(pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, null, "SUDESTE"),
                    CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        @Test
        @DisplayName("regiao inexistente ou ausente")
        void regiaoInvalida() {
            esperaErro(pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "BRONZE", "EUROPA"),
                    CodigoErro.REGIAO_INVALIDA);
            esperaErro(pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "BRONZE", null),
                    CodigoErro.REGIAO_INVALIDA);
        }

        @Test
        @DisplayName("modalidade de entrega inexistente ou ausente")
        void modalidadeInvalida() {
            esperaErro(pedido(List.of(CAMISETA), "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.MODALIDADE_INVALIDA);
            esperaErro(pedido(List.of(CAMISETA), null, null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.MODALIDADE_INVALIDA);
        }

        @Test
        @DisplayName("forma de pagamento inexistente ou ausente")
        void formaPagamentoInvalida() {
            esperaErro(pedido(List.of(CAMISETA), "EXPRESSA", null, "CHEQUE", 1, "BRONZE",
                    "SUDESTE"), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
            esperaErro(pedido(List.of(CAMISETA), "EXPRESSA", null, null, 1, "BRONZE",
                    "SUDESTE"), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        @Test
        @DisplayName("o carrinho e conferido antes do nivel do clube")
        void ordemCarrinhoAntesDoClube() {
            esperaErro(pedido(List.of(), "DRONE", "XPTO", "CHEQUE", 9, "DIAMANTE", "EUROPA"),
                    CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        @DisplayName("o nivel do clube e conferido antes da regiao")
        void ordemClubeAntesDaRegiao() {
            esperaErro(pedido(List.of(CAMISETA), "DRONE", "XPTO", "CHEQUE", 9, "DIAMANTE",
                    "EUROPA"), CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        @Test
        @DisplayName("a regiao e conferida antes da modalidade")
        void ordemRegiaoAntesDaModalidade() {
            esperaErro(pedido(List.of(CAMISETA), "DRONE", "XPTO", "CHEQUE", 9, "BRONZE",
                    "EUROPA"), CodigoErro.REGIAO_INVALIDA);
        }

        @Test
        @DisplayName("a modalidade inexistente vem antes da indisponivel e do cupom")
        void ordemModalidadeAntesDoCupom() {
            esperaErro(pedido(List.of(CAMISETA), "DRONE", "XPTO", "CHEQUE", 9, "BRONZE",
                    "SUDESTE"), CodigoErro.MODALIDADE_INVALIDA);
        }

        @Test
        @DisplayName("a modalidade indisponivel vem antes do cupom")
        void ordemModalidadeIndisponivelAntesDoCupom() {
            ItemRequest pesado = new ItemRequest("Mala", new BigDecimal("100.00"), 6,
                    new BigDecimal("1.00"));

            esperaErro(pedido(List.of(pesado), "MOTOBOY", "XPTO", "CHEQUE", 9, "BRONZE",
                    "SUDESTE"), CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        @Test
        @DisplayName("o cupom inexistente vem antes do cupom nao aplicavel e do pagamento")
        void ordemCupomAntesDoPagamento() {
            esperaErro(pedido(List.of(CAMISETA), "EXPRESSA", "XPTO", "CHEQUE", 9, "BRONZE",
                    "SUDESTE"), CodigoErro.CUPOM_INVALIDO);
        }

        @Test
        @DisplayName("o cupom nao aplicavel vem antes do pagamento")
        void ordemCupomNaoAplicavelAntesDoPagamento() {
            esperaErro(pedido(List.of(CAMISETA), "EXPRESSA", "MENOS50", "CHEQUE", 9, "BRONZE",
                    "SUDESTE"), CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        @Test
        @DisplayName("a forma de pagamento inexistente vem antes do parcelamento")
        void ordemPagamentoAntesDoParcelamento() {
            esperaErro(pedido(List.of(CAMISETA), "EXPRESSA", null, "CHEQUE", 99, "BRONZE",
                    "SUDESTE"), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        @Test
        @DisplayName("o parcelamento vem antes da forma de pagamento indisponivel")
        void ordemParcelamentoAntesDaIndisponibilidade() {
            ItemRequest item = new ItemRequest("Relogio", new BigDecimal("1000.00"), 1,
                    new BigDecimal("0.50"));

            esperaErro(pedido(List.of(item), "RETIRADA_LOJA", null, "BOLETO", 2, "BRONZE",
                    "SUDESTE"), CodigoErro.PARCELAMENTO_INVALIDO);
        }

        private void esperaErro(ResumoRequest requisicao, CodigoErro esperado) {
            assertThatThrownBy(() -> servico.calcularResumo(requisicao))
                    .isInstanceOf(CheckoutException.class)
                    .extracting(erro -> ((CheckoutException) erro).getCodigo())
                    .isEqualTo(esperado);
        }
    }

    @Nested
    @DisplayName("Arredondamento")
    class Arredondamento {

        @Test
        @DisplayName("valores em dinheiro saem com 2 casas, meio para o par")
        void meioParaOPar() {
            // produtos 409,70: seguro SUDESTE 1% = 4,097 -> 4,10
            ResumoResponse paraCima = servico.calcularResumo(pedido(List.of(CAMISETA, TENIS),
                    "RETIRADA_LOJA", null, "CARTAO", 1, "OURO", "SUDESTE"));
            assertThat(paraCima.seguro()).isEqualByComparingTo("4.10");
            // credito OURO 5% = 20,485 -> 20,48 (meio para o par)
            assertThat(paraCima.creditoProximaCompra()).isEqualByComparingTo("20.48");
            assertThat(paraCima.totalFinal().scale()).isEqualTo(2);
        }
    }
}
