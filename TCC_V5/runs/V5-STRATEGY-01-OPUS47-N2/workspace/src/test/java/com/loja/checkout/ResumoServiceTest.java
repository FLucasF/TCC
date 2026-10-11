package com.loja.checkout;

import com.loja.checkout.dominio.ResumoService;
import com.loja.checkout.web.PedidoRequest;
import com.loja.checkout.web.PedidoRequest.ItemRequest;
import com.loja.checkout.web.ResumoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResumoServiceTest {

    private final ResumoService service = new ResumoService();

    private static ItemRequest item(String nome, String preco, int qtd, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static BigDecimal brl(String v) {
        return new BigDecimal(v);
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        var req = new PedidoRequest(
            List.of(item("Camiseta", "79.90", 2, "0.30"),
                    item("Tênis", "249.90", 1, "1.20")),
            "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        ResumoResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(r.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(r.frete()).isEqualByComparingTo("33.10");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo("10.24");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("-20.60");
        assertThat(r.totalFinal()).isEqualByComparingTo("391.47");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo("391.47");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao6x_prata_centrooeste() {
        var req = new PedidoRequest(
            List.of(item("Camiseta", "79.90", 2, "0.30"),
                    item("Tênis", "249.90", 1, "1.20")),
            "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(r.frete()).isEqualByComparingTo("15.60");
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertThat(r.seguro()).isEqualByComparingTo("6.15");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("30.55");
        assertThat(r.totalFinal()).isEqualByComparingTo("462.00");
        assertThat(r.parcelas()).isEqualTo(6);
        assertThat(r.valorParcela()).isEqualByComparingTo("77.00");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("8.19");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        var req = new PedidoRequest(
            List.of(item("Fone", "199.90", 2, "0.25")),
            "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");

        ResumoResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(r.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(r.frete()).isEqualByComparingTo("18.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertThat(r.seguro()).isEqualByComparingTo("8.00");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(r.totalFinal()).isEqualByComparingTo("379.29");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo("379.29");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao3x_prata_sul() {
        var req = new PedidoRequest(
            List.of(item("Meia", "19.90", 7, "0.10"),
                    item("Camiseta", "79.90", 2, "0.30")),
            "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(r.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(r.frete()).isEqualByComparingTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertThat(r.seguro()).isEqualByComparingTo("2.99");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(r.totalFinal()).isEqualByComparingTo("262.29");
        assertThat(r.parcelas()).isEqualTo(3);
        assertThat(r.valorParcela()).isEqualByComparingTo("87.43");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("5.98");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        var req = new PedidoRequest(
            List.of(item("Camiseta", "79.90", 2, "0.30"),
                    item("Tênis", "249.90", 1, "1.20")),
            "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(r.frete()).isEqualByComparingTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo("4.10");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("-20.69");
        assertThat(r.totalFinal()).isEqualByComparingTo("393.11");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo("393.11");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(r.brinde()).isFalse();
    }
}
