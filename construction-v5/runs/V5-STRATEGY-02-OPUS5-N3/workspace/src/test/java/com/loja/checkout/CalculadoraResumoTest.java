package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.aplicacao.CalculadoraResumo;
import com.loja.checkout.aplicacao.PedidoRequest;
import com.loja.checkout.aplicacao.PedidoRequest.ItemRequest;
import com.loja.checkout.aplicacao.ResumoResponse;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class CalculadoraResumoTest {

    private static final ItemRequest CAMISETA = item("Camiseta", "79.90", 2, "0.30");
    private static final ItemRequest TENIS = item("Tenis", "249.90", 1, "1.20");

    @Autowired
    private CalculadoraResumo calculadora;

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        ResumoResponse resumo = calculadora.calcular(new PedidoRequest(
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
        ResumoResponse resumo = calculadora.calcular(new PedidoRequest(
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
        ResumoResponse resumo = calculadora.calcular(new PedidoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")), "MOTOBOY", "MENOS50", "BOLETO", null,
                "BRONZE", "NORDESTE"));

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
        ResumoResponse resumo = calculadora.calcular(new PedidoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2",
                "CARTAO", 3, "PRATA", "SUL"));

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
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        ResumoResponse resumo = calculadora.calcular(new PedidoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

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
    void ouro_acima_de_500_em_produtos_ganha_brinde() {
        ResumoResponse resumo = calculadora.calcular(new PedidoRequest(
                List.of(item("Jaqueta", "600.00", 1, "1.00")), "RETIRADA_LOJA", null, "PIX", 1,
                "OURO", "SUDESTE"));

        assertThat(resumo.brinde()).isTrue();
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("30.00");
    }

    @Test
    void fretegratis_desconta_exatamente_o_frete() {
        ResumoResponse resumo = calculadora.calcular(new PedidoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "BRONZE", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("413.80");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
    }

    @Test
    void ouro_com_fretegratis_nao_desconta_nada_porque_o_frete_ja_e_zero() {
        ResumoResponse resumo = calculadora.calcular(new PedidoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "OURO", "SUDESTE"));

        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
    }

    @Test
    void carrinho_vazio_ou_item_invalido_recusa_o_pedido() {
        assertThat(erroDe(new PedidoRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(new PedidoRequest(null, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(pedidoCom(item("Brinde", "0.00", 1, "0.10"))))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(pedidoCom(item("Camiseta", "79.90", 0, "0.10"))))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(pedidoCom(item("Camiseta", "79.90", 1, "-0.10"))))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(pedidoCom(new ItemRequest("Camiseta", null, null, null))))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void valida_na_ordem_combinada() {
        // Tudo errado de uma vez: vale o primeiro problema da lista.
        assertThat(erroDe(new PedidoRequest(List.of(CAMISETA), "AVIAO", "NAOEXISTE", "CHEQUE", 99,
                "DIAMANTE", "MARTE"))).isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
        assertThat(erroDe(new PedidoRequest(List.of(CAMISETA), "AVIAO", "NAOEXISTE", "CHEQUE", 99,
                "BRONZE", "MARTE"))).isEqualTo(CodigoErro.REGIAO_INVALIDA);
        assertThat(erroDe(new PedidoRequest(List.of(CAMISETA), "AVIAO", "NAOEXISTE", "CHEQUE", 99,
                "BRONZE", "SUL"))).isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
        assertThat(erroDe(new PedidoRequest(List.of(CAMISETA), null, null, null, null,
                "BRONZE", "SUL"))).isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
        assertThat(erroDe(new PedidoRequest(List.of(CAMISETA), "EXPRESSA", "NAOEXISTE", "CHEQUE", 99,
                "BRONZE", "SUL"))).isEqualTo(CodigoErro.CUPOM_INVALIDO);
        assertThat(erroDe(new PedidoRequest(List.of(CAMISETA), "EXPRESSA", "MENOS50", "CHEQUE", 99,
                "BRONZE", "SUL"))).isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
        assertThat(erroDe(new PedidoRequest(List.of(CAMISETA), "EXPRESSA", null, "CHEQUE", 99,
                "BRONZE", "SUL"))).isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        assertThat(erroDe(new PedidoRequest(List.of(CAMISETA), "EXPRESSA", null, "PIX", 2,
                "BRONZE", "SUL"))).isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void motoboy_nao_leva_acima_de_cinco_quilos() {
        assertThat(erroDe(new PedidoRequest(List.of(item("Mala", "100.00", 1, "5.01")), "MOTOBOY",
                null, "PIX", 1, "BRONZE", "SUL"))).isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);

        ResumoResponse noLimite = calculadora.calcular(new PedidoRequest(
                List.of(item("Mala", "100.00", 1, "5.00")), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUL"));
        assertThat(noLimite.frete()).isEqualByComparingTo("18.00");
    }

    @Test
    void cartao_aceita_de_1_a_12_parcelas() {
        assertThat(erroDe(new PedidoRequest(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13,
                "BRONZE", "SUL"))).isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(new PedidoRequest(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 0,
                "BRONZE", "SUL"))).isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        assertThat(calculadora.calcular(new PedidoRequest(List.of(CAMISETA), "EXPRESSA", null,
                "CARTAO", 12, "BRONZE", "SUL")).parcelas()).isEqualTo(12);
    }

    @Test
    void boleto_recusa_total_acima_de_mil_reais() {
        assertThat(erroDe(new PedidoRequest(List.of(item("Sofa", "1200.00", 1, "0.50")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUL")))
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void os_limites_valem_para_quem_esta_exatamente_neles() {
        // MENOS50 com exatamente 300,00 em produtos: aplica.
        ResumoResponse menos50 = calculadora.calcular(new PedidoRequest(
                List.of(item("Vestido", "300.00", 1, "0.40")), "RETIRADA_LOJA", "MENOS50", "PIX", 1,
                "BRONZE", "SUL"));
        assertThat(menos50.descontoCupom()).isEqualByComparingTo("50.00");

        // OURO com exatamente 500,00 em produtos: sem brinde (só acima de 500,00).
        ResumoResponse ouroNoLimite = calculadora.calcular(new PedidoRequest(
                List.of(item("Casaco", "500.00", 1, "0.80")), "RETIRADA_LOJA", null, "PIX", 1,
                "OURO", "SUL"));
        assertThat(ouroNoLimite.brinde()).isFalse();

        // Boleto com total do pedido de exatamente 1.000,00: aceita.
        ResumoResponse boletoNoLimite = calculadora.calcular(new PedidoRequest(
                List.of(item("Bolsa", "990.10", 1, "0.60")), "RETIRADA_LOJA", null, "BOLETO", 1,
                "BRONZE", "SUL"));
        assertThat(boletoNoLimite.totalFinal()).isEqualByComparingTo("1003.49");
    }

    @Test
    void cartao_cobra_juros_a_partir_da_quarta_parcela() {
        // Total do pedido: 299,10 + 2,99 de seguro = 302,09.
        ResumoResponse semJuros = calculadora.calcular(pedidoDeTrezentos(3));
        assertThat(semJuros.totalFinal()).isEqualByComparingTo("302.09");
        assertThat(semJuros.valorParcela()).isEqualByComparingTo("100.70");
        assertThat(semJuros.ajustePagamento()).isEqualByComparingTo("0.00");

        // 4x pela tabela Price a 1,99%: parcela 79,32, total 317,28.
        ResumoResponse comJuros = calculadora.calcular(pedidoDeTrezentos(4));
        assertThat(comJuros.valorParcela()).isEqualByComparingTo("79.32");
        assertThat(comJuros.totalFinal()).isEqualByComparingTo("317.28");
        assertThat(comJuros.ajustePagamento()).isEqualByComparingTo("15.19");
    }

    @Test
    void parcelamento_invalido_vem_antes_da_forma_indisponivel() {
        // Boleto acima de 1.000,00 e parcelado: o parcelamento é o primeiro problema.
        assertThat(erroDe(new PedidoRequest(List.of(item("Sofa", "1200.00", 1, "0.50")),
                "RETIRADA_LOJA", null, "BOLETO", 3, "BRONZE", "SUL")))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void modalidade_indisponivel_vem_antes_do_cupom_invalido() {
        assertThat(erroDe(new PedidoRequest(List.of(item("Mala", "100.00", 1, "9.00")), "MOTOBOY",
                "NAOEXISTE", "PIX", 1, "BRONZE", "SUL"))).isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    private PedidoRequest pedidoDeTrezentos(int parcelas) {
        return new PedidoRequest(List.of(item("Calca", "299.10", 1, "0.50")), "RETIRADA_LOJA", null,
                "CARTAO", parcelas, "BRONZE", "SUL");
    }

    private PedidoRequest pedidoCom(ItemRequest... itens) {
        return new PedidoRequest(Arrays.asList(itens), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
    }

    private CodigoErro erroDe(PedidoRequest pedido) {
        try {
            calculadora.calcular(pedido);
            throw new AssertionError("era esperada uma recusa");
        } catch (PedidoRecusadoException excecao) {
            return excecao.codigo();
        }
    }

    private static ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }
}
