package com.loja.checkout;

import com.loja.checkout.calculo.ErroPedido;
import com.loja.checkout.calculo.ServicoResumo;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServicoResumoTest {

    private final ServicoResumo servico = new ServicoResumo();

    private static ItemRequest item(String nome, String preco, int qtd, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static BigDecimal reais(String v) { return new BigDecimal(v); }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        ResumoRequest req = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        ResumoResponse r = servico.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("40.97"));
        assertThat(r.frete()).isEqualByComparingTo(reais("33.10"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo(reais("10.24"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("-20.60"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("391.47"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("391.47"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao6x_prata_centroeste() {
        ResumoRequest req = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse r = servico.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.frete()).isEqualByComparingTo(reais("15.60"));
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertThat(r.seguro()).isEqualByComparingTo(reais("6.15"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("30.55"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("462.00"));
        assertThat(r.parcelas()).isEqualTo(6);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("77.00"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("8.19"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        ResumoRequest req = new ResumoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");

        ResumoResponse r = servico.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("399.80"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("50.00"));
        assertThat(r.frete()).isEqualByComparingTo(reais("18.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertThat(r.seguro()).isEqualByComparingTo(reais("8.00"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("3.49"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("379.29"));
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("379.29"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao3x_prata_sul() {
        ResumoRequest req = new ResumoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse r = servico.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("299.10"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("39.80"));
        assertThat(r.frete()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertThat(r.seguro()).isEqualByComparingTo(reais("2.99"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("262.29"));
        assertThat(r.parcelas()).isEqualTo(3);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("87.43"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("5.98"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        ResumoRequest req = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse r = servico.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.frete()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo(reais("4.10"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("-20.69"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("393.11"));
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("393.11"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("20.48"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void motoboy_acima_de_5kg_recusa() {
        ResumoRequest req = new ResumoRequest(
                List.of(item("Halter", "100.00", 1, "6.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertThatThrownBy(() -> servico.calcular(req))
                .isInstanceOf(ErroPedido.class)
                .hasMessage("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void boleto_acima_de_1000_recusa() {
        ResumoRequest req = new ResumoRequest(
                List.of(item("TV", "1200.00", 1, "5.00")),
                "ECONOMICA", null, "BOLETO", 1, "BRONZE", "SUDESTE");
        assertThatThrownBy(() -> servico.calcular(req))
                .isInstanceOf(ErroPedido.class)
                .hasMessage("FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void menos50_abaixo_de_300_recusa() {
        ResumoRequest req = new ResumoRequest(
                List.of(item("Meia", "19.90", 2, "0.10")),
                "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");
        assertThatThrownBy(() -> servico.calcular(req))
                .isInstanceOf(ErroPedido.class)
                .hasMessage("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void carrinho_vazio_recusa() {
        ResumoRequest req = new ResumoRequest(
                List.of(), "RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertThatThrownBy(() -> servico.calcular(req))
                .isInstanceOf(ErroPedido.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void parcelamento_invalido_para_pix() {
        ResumoRequest req = new ResumoRequest(
                List.of(item("X", "10.00", 1, "0.10")),
                "RETIRADA_LOJA", null, "PIX", 3, "BRONZE", "SUDESTE");
        assertThatThrownBy(() -> servico.calcular(req))
                .isInstanceOf(ErroPedido.class)
                .hasMessage("PARCELAMENTO_INVALIDO");
    }
}
