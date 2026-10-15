package loja.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import loja.checkout.dominio.ErroCheckout;
import loja.checkout.servico.CheckoutServico;
import loja.checkout.web.ItemRequisicao;
import loja.checkout.web.RequisicaoResumo;
import loja.checkout.web.RespostaResumo;

class CheckoutServicoTest {

    private final CheckoutServico servico = new CheckoutServico();

    private static ItemRequisicao item(String nome, String preco, int qtd, String peso) {
        return new ItemRequisicao(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static BigDecimal reais(String v) {
        return new BigDecimal(v);
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        RespostaResumo r = servico.calcular(new RequisicaoResumo(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"));

        assertEquals(reais("409.70"), r.subtotalProdutos());
        assertEquals(reais("40.97"), r.descontoCupom());
        assertEquals(reais("33.10"), r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(reais("10.24"), r.seguro());
        assertEquals(reais("-20.60"), r.ajustePagamento());
        assertEquals(reais("391.47"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(reais("391.47"), r.valorParcela());
        assertEquals(reais("0.00"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao6x_prata_centro_oeste() {
        RespostaResumo r = servico.calcular(new RequisicaoResumo(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertEquals(reais("409.70"), r.subtotalProdutos());
        assertEquals(reais("0.00"), r.descontoCupom());
        assertEquals(reais("15.60"), r.frete());
        assertEquals(7, r.prazoEntregaDias());
        assertEquals(reais("6.15"), r.seguro());
        assertEquals(reais("30.55"), r.ajustePagamento());
        assertEquals(reais("462.00"), r.totalFinal());
        assertEquals(6, r.parcelas());
        assertEquals(reais("77.00"), r.valorParcela());
        assertEquals(reais("8.19"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        RespostaResumo r = servico.calcular(new RequisicaoResumo(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        assertEquals(reais("399.80"), r.subtotalProdutos());
        assertEquals(reais("50.00"), r.descontoCupom());
        assertEquals(reais("18.00"), r.frete());
        assertEquals(0, r.prazoEntregaDias());
        assertEquals(reais("8.00"), r.seguro());
        assertEquals(reais("3.49"), r.ajustePagamento());
        assertEquals(reais("379.29"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(reais("379.29"), r.valorParcela());
        assertEquals(reais("0.00"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao3x_prata_sul() {
        RespostaResumo r = servico.calcular(new RequisicaoResumo(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertEquals(reais("299.10"), r.subtotalProdutos());
        assertEquals(reais("39.80"), r.descontoCupom());
        assertEquals(reais("0.00"), r.frete());
        assertEquals(1, r.prazoEntregaDias());
        assertEquals(reais("2.99"), r.seguro());
        assertEquals(reais("0.00"), r.ajustePagamento());
        assertEquals(reais("262.29"), r.totalFinal());
        assertEquals(3, r.parcelas());
        assertEquals(reais("87.43"), r.valorParcela());
        assertEquals(reais("5.98"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo5_ouro_expressa_pix_sudeste_frete_zerado() {
        RespostaResumo r = servico.calcular(new RequisicaoResumo(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"));

        assertEquals(reais("409.70"), r.subtotalProdutos());
        assertEquals(reais("0.00"), r.descontoCupom());
        assertEquals(reais("0.00"), r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(reais("4.10"), r.seguro());
        assertEquals(reais("-20.69"), r.ajustePagamento());
        assertEquals(reais("393.11"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(reais("393.11"), r.valorParcela());
        assertEquals(reais("20.48"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void erro_carrinho_vazio() {
        ErroCheckout e = assertThrows(ErroCheckout.class, () -> servico.calcular(new RequisicaoResumo(
                List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE")));
        assertEquals("PEDIDO_INVALIDO", e.codigo());
    }

    @Test
    void erro_item_com_quantidade_zero() {
        ErroCheckout e = assertThrows(ErroCheckout.class, () -> servico.calcular(new RequisicaoResumo(
                List.of(item("X", "10.00", 0, "0.10")),
                "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE")));
        assertEquals("PEDIDO_INVALIDO", e.codigo());
    }

    @Test
    void erro_nivel_clube_invalido() {
        ErroCheckout e = assertThrows(ErroCheckout.class, () -> servico.calcular(new RequisicaoResumo(
                List.of(item("X", "10.00", 1, "0.10")),
                "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUDESTE")));
        assertEquals("NIVEL_CLUBE_INVALIDO", e.codigo());
    }

    @Test
    void erro_motoboy_acima_de_5kg() {
        ErroCheckout e = assertThrows(ErroCheckout.class, () -> servico.calcular(new RequisicaoResumo(
                List.of(item("Peso", "10.00", 1, "6.00")),
                "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE")));
        assertEquals("MODALIDADE_INDISPONIVEL", e.codigo());
    }

    @Test
    void erro_cupom_invalido() {
        ErroCheckout e = assertThrows(ErroCheckout.class, () -> servico.calcular(new RequisicaoResumo(
                List.of(item("X", "10.00", 1, "0.10")),
                "EXPRESSA", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE")));
        assertEquals("CUPOM_INVALIDO", e.codigo());
    }

    @Test
    void erro_menos50_abaixo_de_300() {
        ErroCheckout e = assertThrows(ErroCheckout.class, () -> servico.calcular(new RequisicaoResumo(
                List.of(item("X", "100.00", 1, "0.10")),
                "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE")));
        assertEquals("CUPOM_NAO_APLICAVEL", e.codigo());
    }

    @Test
    void erro_parcelas_invalidas_pix() {
        ErroCheckout e = assertThrows(ErroCheckout.class, () -> servico.calcular(new RequisicaoResumo(
                List.of(item("X", "10.00", 1, "0.10")),
                "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE")));
        assertEquals("PARCELAMENTO_INVALIDO", e.codigo());
    }

    @Test
    void erro_boleto_acima_de_1000() {
        ErroCheckout e = assertThrows(ErroCheckout.class, () -> servico.calcular(new RequisicaoResumo(
                List.of(item("Caro", "600.00", 2, "0.10")),
                "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE")));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", e.codigo());
    }
}
