package br.com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.com.loja.checkout.dominio.CalculadoraResumo;
import br.com.loja.checkout.dominio.Compra;
import br.com.loja.checkout.dominio.Item;
import br.com.loja.checkout.dominio.MotivoRecusa;
import br.com.loja.checkout.dominio.PedidoRecusadoException;
import br.com.loja.checkout.dominio.Resumo;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private static Item item(String nome, String preco, int quantidade, String peso) {
        return new Item(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private static final Item CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final Item TENIS = item("Tenis", "249.90", 1, "1.20");

    private static Compra compra(List<Item> itens, String entrega, String cupom,
                                 String pagamento, Integer parcelas, String nivel, String regiao) {
        return new Compra(itens, entrega, cupom, pagamento, parcelas, nivel, regiao);
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        Resumo resumo = calculadora.calcular(compra(List.of(CAMISETA, TENIS),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

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
    void exemplo2_economica_sem_cupom_cartao6x_prata_centro_oeste() {
        Resumo resumo = calculadora.calcular(compra(List.of(CAMISETA, TENIS),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

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
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        Resumo resumo = calculadora.calcular(compra(List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isZero();
        assertThat(resumo.seguro()).isEqualByComparingTo("8.00");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("379.29");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("379.29");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao3x_prata_sul() {
        Resumo resumo = calculadora.calcular(compra(
                List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

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
    void exemplo5_ouro_nao_paga_frete() {
        Resumo resumo = calculadora.calcular(compra(List.of(CAMISETA, TENIS),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.seguro()).isEqualByComparingTo("4.10");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.69");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("393.11");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void ouro_com_cupom_de_percentual() {
        Resumo resumo = calculadora.calcular(compra(List.of(CAMISETA, TENIS),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-18.64");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("354.19");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
    }

    @Test
    void fretegratis_desconta_o_valor_do_frete_e_mostra_o_frete_no_resumo() {
        Resumo resumo = calculadora.calcular(compra(List.of(CAMISETA, TENIS),
                "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
        // 409.70 - 33.10 + 33.10 + 4.10 = 413.80, menos 5% do Pix
        assertThat(resumo.totalFinal()).isEqualByComparingTo("393.11");
    }

    @Test
    void ouro_ganha_brinde_acima_de_500_em_produtos() {
        Resumo resumo = calculadora.calcular(compra(List.of(item("Jaqueta", "600.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.brinde()).isTrue();
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("30.00");
    }

    @Test
    void cartao_com_juros_cobra_a_parcela_arredondada_vezes_o_numero_de_parcelas() {
        Resumo resumo = calculadora.calcular(compra(List.of(item("Fone", "100.00", 1, "0.50")),
                "RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE"));

        // total do pedido: 100.00 + 0.00 + 1.00 = 101.00
        assertThat(resumo.valorParcela()).isEqualByComparingTo("9.54");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("114.48");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("13.48");
    }

    @Test
    void cartao_sem_juros_mantem_o_total_do_pedido() {
        Resumo resumo = calculadora.calcular(compra(List.of(item("Fone", "100.00", 1, "0.50")),
                "RETIRADA_LOJA", null, "CARTAO", 3, "BRONZE", "SUDESTE"));

        assertThat(resumo.totalFinal()).isEqualByComparingTo("101.00");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("33.67");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
    }

    @Test
    void motoboy_leva_pedido_de_exatamente_5kg() {
        Resumo resumo = calculadora.calcular(compra(List.of(item("Tapete", "100.00", 5, "1.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULO", value = {
            // itens inválidos
            "PEDIDO_INVALIDO,        VAZIO,    EXPRESSA, NULO,        PIX,    1, BRONZE, SUDESTE",
            "PEDIDO_INVALIDO,        NULO,     EXPRESSA, NULO,        PIX,    1, BRONZE, SUDESTE",
            "PEDIDO_INVALIDO,        PRECO_0,  EXPRESSA, NULO,        PIX,    1, BRONZE, SUDESTE",
            "PEDIDO_INVALIDO,        QTD_0,    EXPRESSA, NULO,        PIX,    1, BRONZE, SUDESTE",
            "PEDIDO_INVALIDO,        PESO_0,   EXPRESSA, NULO,        PIX,    1, BRONZE, SUDESTE",
            "PEDIDO_INVALIDO,        SEM_PESO, EXPRESSA, NULO,        PIX,    1, BRONZE, SUDESTE",
            // pedido inválido vem antes de qualquer outra conferência
            "PEDIDO_INVALIDO,        VAZIO,    NULO,     XPTO,        NULO,   9, NULO,   NULO",
            // clube, região, modalidade
            "NIVEL_CLUBE_INVALIDO,   OK,       NULO,     NULO,        NULO,   9, DIAMANTE, NULO",
            "NIVEL_CLUBE_INVALIDO,   OK,       EXPRESSA, NULO,        PIX,    1, NULO,   SUDESTE",
            "REGIAO_INVALIDA,        OK,       XPTO,     NULO,        NULO,   1, BRONZE, NULO",
            "REGIAO_INVALIDA,        OK,       EXPRESSA, NULO,        PIX,    1, BRONZE, EUROPA",
            "MODALIDADE_INVALIDA,    OK,       NULO,     NULO,        PIX,    1, BRONZE, SUDESTE",
            "MODALIDADE_INVALIDA,    OK,       DRONE,    XPTO,        PIX,    1, BRONZE, SUDESTE",
            "MODALIDADE_INDISPONIVEL,PESADO,   MOTOBOY,  XPTO,        PIX,    1, BRONZE, SUDESTE",
            // cupons
            "CUPOM_INVALIDO,         OK,       EXPRESSA, BEMVINDO99,  PIX,    1, BRONZE, SUDESTE",
            "CUPOM_INVALIDO,         OK,       EXPRESSA, bemvindo10,  PIX,    1, BRONZE, SUDESTE",
            "CUPOM_NAO_APLICAVEL,    BARATO,   EXPRESSA, MENOS50,     XPTO,   1, BRONZE, SUDESTE",
            // pagamento
            "FORMA_PAGAMENTO_INVALIDA, OK,     EXPRESSA, NULO,        NULO,   1, BRONZE, SUDESTE",
            "FORMA_PAGAMENTO_INVALIDA, OK,     EXPRESSA, NULO,        CHEQUE, 1, BRONZE, SUDESTE",
            "PARCELAMENTO_INVALIDO,  OK,       EXPRESSA, NULO,        PIX,    2, BRONZE, SUDESTE",
            "PARCELAMENTO_INVALIDO,  OK,       EXPRESSA, NULO,        BOLETO, 3, BRONZE, SUDESTE",
            "PARCELAMENTO_INVALIDO,  OK,       EXPRESSA, NULO,        CARTAO, 13, BRONZE, SUDESTE",
            "PARCELAMENTO_INVALIDO,  OK,       EXPRESSA, NULO,        CARTAO, 0, BRONZE, SUDESTE",
            "FORMA_PAGAMENTO_INDISPONIVEL, CARO, EXPRESSA, NULO,      BOLETO, 1, BRONZE, SUDESTE",
    })
    void recusa_na_ordem_combinada(MotivoRecusa motivo, String carrinho, String entrega,
                                   String cupom, String pagamento, Integer parcelas,
                                   String nivel, String regiao) {
        Compra compra = compra(carrinho(carrinho), entrega, cupom, pagamento, parcelas, nivel, regiao);

        assertThatThrownBy(() -> calculadora.calcular(compra))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(excecao -> ((PedidoRecusadoException) excecao).motivo())
                .isEqualTo(motivo);
    }

    private static List<Item> carrinho(String nome) {
        if (nome == null) {
            return null;
        }
        return switch (nome) {
            case "OK" -> List.of(CAMISETA, TENIS);
            case "VAZIO" -> List.of();
            case "PRECO_0" -> List.of(item("Brinde", "0.00", 1, "0.10"));
            case "QTD_0" -> List.of(item("Camiseta", "79.90", 0, "0.30"));
            case "PESO_0" -> List.of(item("Camiseta", "79.90", 1, "0"));
            case "SEM_PESO" -> List.of(new Item("Camiseta", new BigDecimal("79.90"), 1, null));
            case "PESADO" -> List.of(item("Tapete", "100.00", 6, "1.00"));
            case "BARATO" -> List.of(item("Meia", "19.90", 1, "0.10"));
            case "CARO" -> List.of(item("Sofa", "2000.00", 1, "1.00"));
            default -> throw new IllegalArgumentException(nome);
        };
    }
}
