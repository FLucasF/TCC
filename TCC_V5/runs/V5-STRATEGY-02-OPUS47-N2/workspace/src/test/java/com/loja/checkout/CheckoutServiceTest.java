package com.loja.checkout;

import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.CheckoutResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CheckoutServiceTest {

    @Autowired
    CheckoutService service;

    private static CheckoutRequest.ItemRequest item(String nome, String preco, int qtd, String peso) {
        return new CheckoutRequest.ItemRequest(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static BigDecimal brl(String s) { return new BigDecimal(s); }

    @Test
    void exemplo1() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");
        CheckoutResponse r = service.calcular(req);
        assertThat(r.subtotalProdutos()).isEqualByComparingTo(brl("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(brl("40.97"));
        assertThat(r.frete()).isEqualByComparingTo(brl("33.10"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo(brl("10.24"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(brl("-20.60"));
        assertThat(r.totalFinal()).isEqualByComparingTo(brl("391.47"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo(brl("391.47"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(brl("0.00"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");
        CheckoutResponse r = service.calcular(req);
        assertThat(r.subtotalProdutos()).isEqualByComparingTo(brl("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(brl("0.00"));
        assertThat(r.frete()).isEqualByComparingTo(brl("15.60"));
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertThat(r.seguro()).isEqualByComparingTo(brl("6.15"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(brl("30.55"));
        assertThat(r.totalFinal()).isEqualByComparingTo(brl("462.00"));
        assertThat(r.valorParcela()).isEqualByComparingTo(brl("77.00"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(brl("8.19"));
    }

    @Test
    void exemplo3() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");
        CheckoutResponse r = service.calcular(req);
        assertThat(r.subtotalProdutos()).isEqualByComparingTo(brl("399.80"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(brl("50.00"));
        assertThat(r.frete()).isEqualByComparingTo(brl("18.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertThat(r.seguro()).isEqualByComparingTo(brl("8.00"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(brl("3.49"));
        assertThat(r.totalFinal()).isEqualByComparingTo(brl("379.29"));
    }

    @Test
    void exemplo4() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");
        CheckoutResponse r = service.calcular(req);
        assertThat(r.subtotalProdutos()).isEqualByComparingTo(brl("299.10"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(brl("39.80"));
        assertThat(r.frete()).isEqualByComparingTo(brl("0.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertThat(r.seguro()).isEqualByComparingTo(brl("2.99"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(brl("0.00"));
        assertThat(r.totalFinal()).isEqualByComparingTo(brl("262.29"));
        assertThat(r.valorParcela()).isEqualByComparingTo(brl("87.43"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(brl("5.98"));
    }

    @Test
    void exemplo5() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");
        CheckoutResponse r = service.calcular(req);
        assertThat(r.frete()).isEqualByComparingTo(brl("0.00"));
        assertThat(r.seguro()).isEqualByComparingTo(brl("4.10"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(brl("-20.69"));
        assertThat(r.totalFinal()).isEqualByComparingTo(brl("393.11"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(brl("20.48"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void motoboyAcimaDe5kg() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Pesado", "100.00", 1, "6.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(ErroCheckoutException.class)
                .hasMessage("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void boletoAcimaDe1000() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Caro", "1200.00", 1, "0.50")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE");
        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(ErroCheckoutException.class)
                .hasMessage("FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void carrinhoVazio() {
        CheckoutRequest req = new CheckoutRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(ErroCheckoutException.class)
                .hasMessage("PEDIDO_INVALIDO");
    }
}
