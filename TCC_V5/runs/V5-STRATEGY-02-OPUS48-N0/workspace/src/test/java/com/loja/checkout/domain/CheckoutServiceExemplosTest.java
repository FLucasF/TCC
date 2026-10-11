package com.loja.checkout.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.api.dto.CheckoutRequest;
import com.loja.checkout.api.dto.ItemRequest;
import com.loja.checkout.api.dto.ResumoResponse;
import com.loja.checkout.domain.cupom.CupomBemVindo10;
import com.loja.checkout.domain.cupom.CupomFreteGratis;
import com.loja.checkout.domain.cupom.CupomLeve3Pague2;
import com.loja.checkout.domain.cupom.CupomMenos50;
import com.loja.checkout.domain.cupom.RegistroCupons;
import com.loja.checkout.domain.entrega.EntregaEconomica;
import com.loja.checkout.domain.entrega.EntregaExpressa;
import com.loja.checkout.domain.entrega.EntregaMotoboy;
import com.loja.checkout.domain.entrega.EntregaRetiradaLoja;
import com.loja.checkout.domain.entrega.RegistroModalidadesEntrega;
import com.loja.checkout.domain.pagamento.PagamentoBoleto;
import com.loja.checkout.domain.pagamento.PagamentoCartao;
import com.loja.checkout.domain.pagamento.PagamentoPix;
import com.loja.checkout.domain.pagamento.RegistroFormasPagamento;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Confere os 5 exemplos ja validados pelo financeiro. */
class CheckoutServiceExemplosTest {

    private final CheckoutService service = new CheckoutService(
            new RegistroModalidadesEntrega(List.of(
                    new EntregaEconomica(), new EntregaExpressa(),
                    new EntregaRetiradaLoja(), new EntregaMotoboy())),
            new RegistroCupons(List.of(
                    new CupomBemVindo10(), new CupomMenos50(),
                    new CupomFreteGratis(), new CupomLeve3Pague2())),
            new RegistroFormasPagamento(List.of(
                    new PagamentoPix(), new PagamentoCartao(), new PagamentoBoleto())));

    private static ItemRequest item(String nome, String preco, int qtd, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static BigDecimal reais(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void exemplo1() {
        ResumoResponse r = service.calcular(new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

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
    void exemplo2() {
        ResumoResponse r = service.calcular(new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

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
    void exemplo3() {
        ResumoResponse r = service.calcular(new CheckoutRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("399.80"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("50.00"));
        assertThat(r.frete()).isEqualByComparingTo(reais("18.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertThat(r.seguro()).isEqualByComparingTo(reais("8.00"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("3.49"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("379.29"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("379.29"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo4() {
        ResumoResponse r = service.calcular(new CheckoutRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

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
    void exemplo5() {
        ResumoResponse r = service.calcular(new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.frete()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo(reais("4.10"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("-20.69"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("393.11"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("393.11"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("20.48"));
        assertThat(r.brinde()).isFalse();
    }
}
