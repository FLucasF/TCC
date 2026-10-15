package com.loja.checkout;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.ItemSolicitado;
import com.loja.checkout.dominio.MotivoRecusa;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.dominio.ResumoCompra;
import com.loja.checkout.dominio.SolicitacaoResumo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class CalculadoraResumoTest {

    private static final ItemSolicitado CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemSolicitado TENIS = item("Tenis", "249.90", 1, "1.20");

    @Autowired
    private CalculadoraResumo calculadora;

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        ResumoCompra resumo = calculadora.calcular(new SolicitacaoResumo(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

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
    void exemplo2_economica_sem_cupom_cartao_6x_prata_centro_oeste() {
        ResumoCompra resumo = calculadora.calcular(new SolicitacaoResumo(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

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
        ResumoCompra resumo = calculadora.calcular(new SolicitacaoResumo(
                List.of(item("Fone", "199.90", 2, "0.25")),
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
    void exemplo4_retirada_leve3pague2_cartao_3x_prata_sul() {
        ResumoCompra resumo = calculadora.calcular(new SolicitacaoResumo(
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
        ResumoCompra resumo = calculadora.calcular(new SolicitacaoResumo(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualByComparingTo("4.10");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.69");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("393.11");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("393.11");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void fretegratis_desconta_o_valor_do_frete_e_o_frete_aparece_no_resumo() {
        ResumoCompra resumo = calculadora.calcular(new SolicitacaoResumo(
                List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("413.80");
    }

    @Test
    void ouro_com_produtos_acima_de_500_ganha_brinde() {
        ResumoCompra resumo = calculadora.calcular(new SolicitacaoResumo(
                List.of(item("Jaqueta", "299.90", 2, "0.80")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.brinde()).isTrue();
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("29.99");
    }

    @ParameterizedTest
    @CsvSource(nullValues = "-", value = {
            // itens, modalidade, cupom, pagamento, parcelas, nivel, regiao, motivo esperado
            "vazio,    ECONOMICA, -,          PIX,    1, BRONZE, SUDESTE, PEDIDO_INVALIDO",
            "preco0,   ECONOMICA, -,          PIX,    1, BRONZE, SUDESTE, PEDIDO_INVALIDO",
            "semPeso,  ECONOMICA, -,          PIX,    1, BRONZE, SUDESTE, PEDIDO_INVALIDO",
            "normal,   ECONOMICA, -,          PIX,    1, DIAMANTE, SUDESTE, NIVEL_CLUBE_INVALIDO",
            "normal,   ECONOMICA, -,          PIX,    1, -,      SUDESTE, NIVEL_CLUBE_INVALIDO",
            "normal,   ECONOMICA, -,          PIX,    1, BRONZE, LESTE,   REGIAO_INVALIDA",
            "normal,   DRONE,     -,          PIX,    1, BRONZE, SUDESTE, MODALIDADE_INVALIDA",
            "pesado,   MOTOBOY,   -,          PIX,    1, BRONZE, SUDESTE, MODALIDADE_INDISPONIVEL",
            "normal,   ECONOMICA, PROMO99,    PIX,    1, BRONZE, SUDESTE, CUPOM_INVALIDO",
            "normal,   ECONOMICA, bemvindo10, PIX,    1, BRONZE, SUDESTE, CUPOM_INVALIDO",
            "barato,   ECONOMICA, MENOS50,    PIX,    1, BRONZE, SUDESTE, CUPOM_NAO_APLICAVEL",
            "normal,   ECONOMICA, -,          CHEQUE, 1, BRONZE, SUDESTE, FORMA_PAGAMENTO_INVALIDA",
            "normal,   ECONOMICA, -,          -,      1, BRONZE, SUDESTE, FORMA_PAGAMENTO_INVALIDA",
            "normal,   ECONOMICA, -,          PIX,    2, BRONZE, SUDESTE, PARCELAMENTO_INVALIDO",
            "normal,   ECONOMICA, -,          BOLETO, 3, BRONZE, SUDESTE, PARCELAMENTO_INVALIDO",
            "normal,   ECONOMICA, -,          CARTAO, 13, BRONZE, SUDESTE, PARCELAMENTO_INVALIDO",
            "caro,     ECONOMICA, -,          BOLETO, 1, BRONZE, SUDESTE, FORMA_PAGAMENTO_INDISPONIVEL"
    })
    void recusa_o_pedido_com_o_primeiro_problema_encontrado(String carrinho, String modalidade, String cupom,
                                                            String pagamento, Integer parcelas, String nivel,
                                                            String regiao, MotivoRecusa esperado) {
        SolicitacaoResumo solicitacao = new SolicitacaoResumo(
                carrinho(carrinho), modalidade, cupom, pagamento, parcelas, nivel, regiao);

        assertThatThrownBy(() -> calculadora.calcular(solicitacao))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(excecao -> ((PedidoRecusadoException) excecao).motivo())
                .isEqualTo(esperado);
    }

    @Test
    void carrinho_ausente_e_pedido_invalido() {
        SolicitacaoResumo solicitacao =
                new SolicitacaoResumo(null, "ECONOMICA", null, "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> calculadora.calcular(solicitacao))
                .isInstanceOf(PedidoRecusadoException.class);
    }

    private static List<ItemSolicitado> carrinho(String nome) {
        return switch (nome) {
            case "vazio" -> List.of();
            case "preco0" -> List.of(item("Brinde", "0.00", 1, "0.10"));
            case "semPeso" -> Arrays.asList(new ItemSolicitado("Meia", new BigDecimal("19.90"), 1, null));
            case "pesado" -> List.of(item("Mala", "399.00", 1, "6.00"));
            case "barato" -> List.of(item("Meia", "19.90", 1, "0.10"));
            case "caro" -> List.of(item("Sofa", "1500.00", 1, "0.50"));
            default -> List.of(CAMISETA, TENIS);
        };
    }

    private static ItemSolicitado item(String nome, String preco, int quantidade, String peso) {
        return new ItemSolicitado(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }
}
