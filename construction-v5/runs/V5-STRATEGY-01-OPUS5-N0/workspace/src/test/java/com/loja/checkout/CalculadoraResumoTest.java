package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.aplicacao.CalculadoraResumo;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.PedidoRecusadoException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CalculadoraResumoTest {

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    @Autowired
    private CalculadoraResumo calculadora;

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private ResumoResponse calcular(List<ItemRequest> itens, String entrega, String cupom,
                                    String pagamento, Integer parcelas, String clube, String regiao) {
        return calculadora.calcular(
                new ResumoRequest(itens, entrega, cupom, pagamento, parcelas, clube, regiao));
    }

    private void esperarErro(ResumoRequest pedido, CodigoErro codigo) {
        assertThatThrownBy(() -> calculadora.calcular(pedido))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(e -> ((PedidoRecusadoException) e).getCodigo())
                .isEqualTo(codigo);
    }

    private static void assertValor(BigDecimal valor, String esperado) {
        assertThat(valor).isEqualByComparingTo(new BigDecimal(esperado));
        assertThat(valor.scale()).as("valor em dinheiro com 2 casas").isEqualTo(2);
    }

    @Nested
    @DisplayName("Exemplos conferidos pelo financeiro")
    class Exemplos {

        @Test
        @DisplayName("1: EXPRESSA, BEMVINDO10, PIX, BRONZE, NORTE")
        void exemplo1() {
            ResumoResponse r = calcular(List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10",
                    "PIX", 1, "BRONZE", "NORTE");

            assertValor(r.subtotalProdutos(), "409.70");
            assertValor(r.descontoCupom(), "40.97");
            assertValor(r.frete(), "33.10");
            assertThat(r.prazoEntregaDias()).isEqualTo(2);
            assertValor(r.seguro(), "10.24");
            assertValor(r.ajustePagamento(), "-20.60");
            assertValor(r.totalFinal(), "391.47");
            assertThat(r.parcelas()).isEqualTo(1);
            assertValor(r.valorParcela(), "391.47");
            assertValor(r.creditoProximaCompra(), "0.00");
            assertThat(r.brinde()).isFalse();
        }

        @Test
        @DisplayName("2: ECONOMICA, sem cupom, CARTAO 6x, PRATA, CENTRO_OESTE")
        void exemplo2() {
            ResumoResponse r = calcular(List.of(CAMISETA, TENIS), "ECONOMICA", null,
                    "CARTAO", 6, "PRATA", "CENTRO_OESTE");

            assertValor(r.subtotalProdutos(), "409.70");
            assertValor(r.descontoCupom(), "0.00");
            assertValor(r.frete(), "15.60");
            assertThat(r.prazoEntregaDias()).isEqualTo(7);
            assertValor(r.seguro(), "6.15");
            assertValor(r.ajustePagamento(), "30.55");
            assertValor(r.totalFinal(), "462.00");
            assertThat(r.parcelas()).isEqualTo(6);
            assertValor(r.valorParcela(), "77.00");
            assertValor(r.creditoProximaCompra(), "8.19");
            assertThat(r.brinde()).isFalse();
        }

        @Test
        @DisplayName("3: MOTOBOY, MENOS50, BOLETO, BRONZE, NORDESTE")
        void exemplo3() {
            ResumoResponse r = calcular(List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY",
                    "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");

            assertValor(r.subtotalProdutos(), "399.80");
            assertValor(r.descontoCupom(), "50.00");
            assertValor(r.frete(), "18.00");
            assertThat(r.prazoEntregaDias()).isZero();
            assertValor(r.seguro(), "8.00");
            assertValor(r.ajustePagamento(), "3.49");
            assertValor(r.totalFinal(), "379.29");
            assertValor(r.valorParcela(), "379.29");
            assertValor(r.creditoProximaCompra(), "0.00");
            assertThat(r.brinde()).isFalse();
        }

        @Test
        @DisplayName("4: RETIRADA_LOJA, LEVE3PAGUE2, CARTAO 3x, PRATA, SUL")
        void exemplo4() {
            ResumoResponse r = calcular(
                    List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA), "RETIRADA_LOJA",
                    "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

            assertValor(r.subtotalProdutos(), "299.10");
            assertValor(r.descontoCupom(), "39.80");
            assertValor(r.frete(), "0.00");
            assertThat(r.prazoEntregaDias()).isEqualTo(1);
            assertValor(r.seguro(), "2.99");
            assertValor(r.ajustePagamento(), "0.00");
            assertValor(r.totalFinal(), "262.29");
            assertThat(r.parcelas()).isEqualTo(3);
            assertValor(r.valorParcela(), "87.43");
            assertValor(r.creditoProximaCompra(), "5.98");
            assertThat(r.brinde()).isFalse();
        }

        @Test
        @DisplayName("5: EXPRESSA, sem cupom, PIX, OURO, SUDESTE")
        void exemplo5() {
            ResumoResponse r = calcular(List.of(CAMISETA, TENIS), "EXPRESSA", null,
                    "PIX", 1, "OURO", "SUDESTE");

            assertValor(r.subtotalProdutos(), "409.70");
            assertValor(r.descontoCupom(), "0.00");
            assertValor(r.frete(), "0.00");
            assertThat(r.prazoEntregaDias()).isEqualTo(2);
            assertValor(r.seguro(), "4.10");
            assertValor(r.ajustePagamento(), "-20.69");
            assertValor(r.totalFinal(), "393.11");
            assertValor(r.valorParcela(), "393.11");
            assertValor(r.creditoProximaCompra(), "20.48");
            assertThat(r.brinde()).isFalse();
        }

        @Test
        @DisplayName("Anexo: EXPRESSA, BEMVINDO10, PIX, OURO, SUDESTE")
        void exemploDoAnexo() {
            ResumoResponse r = calcular(List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10",
                    "PIX", 1, "OURO", "SUDESTE");

            assertValor(r.subtotalProdutos(), "409.70");
            assertValor(r.descontoCupom(), "40.97");
            assertValor(r.frete(), "0.00");
            assertValor(r.seguro(), "4.10");
            assertValor(r.ajustePagamento(), "-18.64");
            assertValor(r.totalFinal(), "354.19");
            assertValor(r.valorParcela(), "354.19");
            assertValor(r.creditoProximaCompra(), "20.48");
            assertThat(r.brinde()).isFalse();
        }
    }

    @Nested
    @DisplayName("Entrega")
    class Entrega {

        @Test
        @DisplayName("Frete por peso: ECONOMICA cobra R$ 2,00 por kg")
        void freteEconomicaPorPeso() {
            ResumoResponse r = calcular(List.of(item("Mochila", "100.00", 2, "1.50")),
                    "ECONOMICA", null, "PIX", 1, "BRONZE", "SUDESTE");

            assertValor(r.frete(), "18.00");
        }

        @Test
        @DisplayName("Motoboy leva pedidos de exatamente 5 kg")
        void motoboyNoLimite() {
            ResumoResponse r = calcular(List.of(item("Caixa", "10.00", 5, "1.00")),
                    "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");

            assertValor(r.frete(), "18.00");
        }

        @Test
        @DisplayName("Motoboy recusa pedidos acima de 5 kg")
        void motoboyAcimaDoLimite() {
            esperarErro(new ResumoRequest(List.of(item("Caixa", "10.00", 6, "1.00")),
                            "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.MODALIDADE_INDISPONIVEL);
        }
    }

    @Nested
    @DisplayName("Cupons")
    class Cupons {

        @Test
        @DisplayName("FRETEGRATIS: o desconto fica igual ao valor do frete")
        void freteGratis() {
            ResumoResponse r = calcular(List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS",
                    "PIX", 1, "BRONZE", "SUDESTE");

            assertValor(r.frete(), "33.10");
            assertValor(r.descontoCupom(), "33.10");
            // 409,70 - 33,10 + 33,10 + 4,10 = 413,80 menos 5% do Pix
            assertValor(r.totalFinal(), "393.11");
        }

        @Test
        @DisplayName("FRETEGRATIS no OURO: o frete ja e zero, o desconto tambem")
        void freteGratisComOuro() {
            ResumoResponse r = calcular(List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS",
                    "PIX", 1, "OURO", "SUDESTE");

            assertValor(r.frete(), "0.00");
            assertValor(r.descontoCupom(), "0.00");
        }

        @Test
        @DisplayName("MENOS50 vale a partir de R$ 300,00 em produtos")
        void menos50NoLimite() {
            ResumoResponse r = calcular(List.of(item("Calca", "150.00", 2, "0.40")),
                    "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");

            assertValor(r.descontoCupom(), "50.00");
        }

        @Test
        @DisplayName("MENOS50 abaixo de R$ 300,00 nao e aplicavel")
        void menos50AbaixoDoMinimo() {
            esperarErro(new ResumoRequest(List.of(item("Calca", "149.99", 2, "0.40")),
                            "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        @Test
        @DisplayName("LEVE3PAGUE2 conta as unidades gratis item por item")
        void leve3Pague2() {
            ResumoResponse r = calcular(
                    List.of(item("Meia", "10.00", 6, "0.10"), item("Bone", "30.00", 2, "0.20")),
                    "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", 1, "BRONZE", "SUDESTE");

            // 6 meias dao 2 gratis; 2 bones nao dao nenhum
            assertValor(r.descontoCupom(), "20.00");
        }

        @Test
        @DisplayName("Cupom em letras minusculas nao existe")
        void cupomMinusculo() {
            esperarErro(new ResumoRequest(List.of(CAMISETA), "RETIRADA_LOJA", "bemvindo10",
                    "PIX", 1, "BRONZE", "SUDESTE"), CodigoErro.CUPOM_INVALIDO);
        }
    }

    @Nested
    @DisplayName("Clube da loja")
    class Clube {

        @Test
        @DisplayName("OURO acima de R$ 500,00 em produtos leva brinde")
        void brindeDoOuro() {
            ResumoResponse r = calcular(List.of(item("Jaqueta", "300.00", 2, "1.00")),
                    "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE");

            assertThat(r.brinde()).isTrue();
            assertValor(r.creditoProximaCompra(), "30.00");
        }

        @Test
        @DisplayName("OURO com exatamente R$ 500,00 em produtos nao leva brinde")
        void semBrindeNoLimite() {
            ResumoResponse r = calcular(List.of(item("Jaqueta", "250.00", 2, "1.00")),
                    "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE");

            assertThat(r.brinde()).isFalse();
        }

        @Test
        @DisplayName("O credito e sobre os produtos, sem desconto e sem frete")
        void creditoSobreProdutos() {
            ResumoResponse r = calcular(List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10",
                    "PIX", 1, "PRATA", "SUDESTE");

            assertValor(r.creditoProximaCompra(), "8.19");
        }
    }

    @Nested
    @DisplayName("Pagamento")
    class Pagamento {

        @Test
        @DisplayName("Cartao em 1x nao tem juros")
        void cartaoAVista() {
            ResumoResponse r = calcular(List.of(CAMISETA), "RETIRADA_LOJA", null,
                    "CARTAO", 1, "BRONZE", "SUDESTE");

            // 159,80 + 1,60 de seguro
            assertValor(r.totalFinal(), "161.40");
            assertValor(r.ajustePagamento(), "0.00");
            assertValor(r.valorParcela(), "161.40");
        }

        @Test
        @DisplayName("Cartao em 12x cobra juros de 1,99% ao mes")
        void cartaoComJuros() {
            ResumoResponse r = calcular(List.of(CAMISETA, TENIS), "RETIRADA_LOJA", null,
                    "CARTAO", 12, "BRONZE", "SUDESTE");

            // total do pedido 413,80; parcela pela tabela Price
            assertValor(r.valorParcela(), "39.10");
            assertValor(r.totalFinal(), "469.20");
            assertValor(r.ajustePagamento(), "55.40");
        }

        @Test
        @DisplayName("Cartao acima de 12x nao e permitido")
        void cartaoAcimaDe12x() {
            esperarErro(new ResumoRequest(List.of(CAMISETA), "RETIRADA_LOJA", null,
                    "CARTAO", 13, "BRONZE", "SUDESTE"), CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        @DisplayName("Pix nao parcela")
        void pixNaoParcela() {
            esperarErro(new ResumoRequest(List.of(CAMISETA), "RETIRADA_LOJA", null,
                    "PIX", 2, "BRONZE", "SUDESTE"), CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        @DisplayName("Boleto nao parcela")
        void boletoNaoParcela() {
            esperarErro(new ResumoRequest(List.of(CAMISETA), "RETIRADA_LOJA", null,
                    "BOLETO", 3, "BRONZE", "SUDESTE"), CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        @DisplayName("Parcelas ausentes valem 1")
        void parcelasAusentes() {
            ResumoResponse r = calcular(List.of(CAMISETA), "RETIRADA_LOJA", null,
                    "CARTAO", null, "BRONZE", "SUDESTE");

            assertThat(r.parcelas()).isEqualTo(1);
        }

        @Test
        @DisplayName("Boleto nao atende pedido acima de R$ 1.000,00")
        void boletoAcimaDoLimite() {
            esperarErro(new ResumoRequest(List.of(item("Sofa", "1000.00", 1, "1.00")),
                            "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        @Test
        @DisplayName("Boleto atende pedido de exatamente R$ 1.000,00")
        void boletoNoLimite() {
            ResumoResponse r = calcular(List.of(item("Sofa", "990.10", 1, "1.00")),
                    "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE");

            // 990,10 + 9,90 de seguro = 1.000,00
            assertValor(r.totalFinal(), "1003.49");
        }
    }

    @Nested
    @DisplayName("Seguro por regiao")
    class Seguro {

        @Test
        @DisplayName("Cada regiao tem seu percentual sobre os produtos")
        void percentuaisPorRegiao() {
            String[][] casos = {
                {"SUDESTE", "4.10"}, {"SUL", "4.10"}, {"CENTRO_OESTE", "6.15"},
                {"NORTE", "10.24"}, {"NORDESTE", "8.19"}
            };
            for (String[] caso : casos) {
                ResumoResponse r = calcular(List.of(CAMISETA, TENIS), "RETIRADA_LOJA", null,
                        "CARTAO", 1, "BRONZE", caso[0]);
                assertThat(r.seguro())
                        .as("seguro da regiao %s", caso[0])
                        .isEqualByComparingTo(new BigDecimal(caso[1]));
            }
        }
    }

    @Nested
    @DisplayName("Recusa do pedido, na ordem de conferencia")
    class Recusas {

        @Test
        @DisplayName("Carrinho vazio ou ausente")
        void carrinhoVazio() {
            esperarErro(new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.PEDIDO_INVALIDO);
            esperarErro(new ResumoRequest(null, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                    CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        @DisplayName("Item com preco, quantidade ou peso invalido")
        void itemInvalido() {
            List<ItemRequest> invalidos = Arrays.asList(
                    item("Camiseta", "0.00", 1, "0.30"),
                    item("Camiseta", "-10.00", 1, "0.30"),
                    new ItemRequest("Camiseta", null, 1, new BigDecimal("0.30")),
                    item("Camiseta", "79.90", 0, "0.30"),
                    item("Camiseta", "79.90", -1, "0.30"),
                    new ItemRequest("Camiseta", new BigDecimal("79.90"), null, new BigDecimal("0.30")),
                    item("Camiseta", "79.90", 1, "0.00"),
                    item("Camiseta", "79.90", 1, "-0.30"),
                    new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, null));

            for (ItemRequest invalido : invalidos) {
                esperarErro(new ResumoRequest(List.of(invalido), "EXPRESSA", null, "PIX", 1,
                        "BRONZE", "SUDESTE"), CodigoErro.PEDIDO_INVALIDO);
            }
        }

        @Test
        @DisplayName("Pedido invalido vem antes de qualquer outro problema")
        void pedidoInvalidoTemPrioridade() {
            esperarErro(new ResumoRequest(List.of(), "NAVIO", "NAOEXISTE", "CHEQUE", 99,
                    "DIAMANTE", "EUROPA"), CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        @DisplayName("Nivel do clube invalido vem antes da regiao")
        void nivelClubeInvalido() {
            esperarErro(new ResumoRequest(List.of(CAMISETA), "NAVIO", "NAOEXISTE", "CHEQUE", 99,
                    "DIAMANTE", "EUROPA"), CodigoErro.NIVEL_CLUBE_INVALIDO);
            esperarErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1,
                    null, "SUDESTE"), CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        @Test
        @DisplayName("Regiao invalida vem antes da modalidade")
        void regiaoInvalida() {
            esperarErro(new ResumoRequest(List.of(CAMISETA), "NAVIO", "NAOEXISTE", "CHEQUE", 99,
                    "BRONZE", "EUROPA"), CodigoErro.REGIAO_INVALIDA);
            esperarErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1,
                    "BRONZE", null), CodigoErro.REGIAO_INVALIDA);
        }

        @Test
        @DisplayName("Modalidade invalida vem antes do cupom")
        void modalidadeInvalida() {
            esperarErro(new ResumoRequest(List.of(CAMISETA), "NAVIO", "NAOEXISTE", "CHEQUE", 99,
                    "BRONZE", "SUDESTE"), CodigoErro.MODALIDADE_INVALIDA);
            esperarErro(new ResumoRequest(List.of(CAMISETA), null, null, "PIX", 1,
                    "BRONZE", "SUDESTE"), CodigoErro.MODALIDADE_INVALIDA);
        }

        @Test
        @DisplayName("Modalidade indisponivel vem antes do cupom")
        void modalidadeIndisponivel() {
            esperarErro(new ResumoRequest(List.of(item("Caixa", "10.00", 6, "1.00")),
                            "MOTOBOY", "NAOEXISTE", "CHEQUE", 99, "BRONZE", "SUDESTE"),
                    CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        @Test
        @DisplayName("Cupom invalido vem antes da forma de pagamento")
        void cupomInvalido() {
            esperarErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", "NAOEXISTE", "CHEQUE", 99,
                    "BRONZE", "SUDESTE"), CodigoErro.CUPOM_INVALIDO);
        }

        @Test
        @DisplayName("Cupom nao aplicavel vem antes da forma de pagamento")
        void cupomNaoAplicavel() {
            esperarErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", "MENOS50", "CHEQUE", 99,
                    "BRONZE", "SUDESTE"), CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        @Test
        @DisplayName("Forma de pagamento invalida vem antes do parcelamento")
        void formaPagamentoInvalida() {
            esperarErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, "CHEQUE", 99,
                    "BRONZE", "SUDESTE"), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
            esperarErro(new ResumoRequest(List.of(CAMISETA), "EXPRESSA", null, null, 1,
                    "BRONZE", "SUDESTE"), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        @Test
        @DisplayName("Parcelamento invalido vem antes da disponibilidade do pagamento")
        void parcelamentoInvalido() {
            esperarErro(new ResumoRequest(List.of(item("Sofa", "1000.00", 1, "1.00")),
                            "RETIRADA_LOJA", null, "BOLETO", 2, "BRONZE", "SUDESTE"),
                    CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }
}
