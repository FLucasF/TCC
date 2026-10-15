package com.loja.checkout.service;

import com.loja.checkout.web.dto.ItemRequest;
import com.loja.checkout.web.dto.PedidoRequest;
import com.loja.checkout.web.dto.ResumoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static ItemRequest camiseta() {
        return new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    }

    private static ItemRequest tenis() {
        return new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    }

    private static ItemRequest fone() {
        return new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
    }

    private static ItemRequest meia() {
        return new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
    }

    @Test
    void exemplo1() {
        PedidoRequest request = new PedidoRequest(
                List.of(camiseta(), tenis()), "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualTo("40.97");
        assertThat(resumo.frete()).isEqualTo("33.10");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualTo("10.24");
        assertThat(resumo.ajustePagamento()).isEqualTo("-20.60");
        assertThat(resumo.totalFinal()).isEqualTo("391.47");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualTo("391.47");
        assertThat(resumo.creditoProximaCompra()).isEqualTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo2() {
        PedidoRequest request = new PedidoRequest(
                List.of(camiseta(), tenis()), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualTo("0.00");
        assertThat(resumo.frete()).isEqualTo("15.60");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.seguro()).isEqualTo("6.15");
        assertThat(resumo.ajustePagamento()).isEqualTo("30.55");
        assertThat(resumo.totalFinal()).isEqualTo("462.00");
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualTo("77.00");
        assertThat(resumo.creditoProximaCompra()).isEqualTo("8.19");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo3() {
        PedidoRequest request = new PedidoRequest(
                List.of(fone()), "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualTo("50.00");
        assertThat(resumo.frete()).isEqualTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(0);
        assertThat(resumo.seguro()).isEqualTo("8.00");
        assertThat(resumo.ajustePagamento()).isEqualTo("3.49");
        assertThat(resumo.totalFinal()).isEqualTo("379.29");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualTo("379.29");
        assertThat(resumo.creditoProximaCompra()).isEqualTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo4() {
        PedidoRequest request = new PedidoRequest(
                List.of(meia(), camiseta()), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo("299.10");
        assertThat(resumo.descontoCupom()).isEqualTo("39.80");
        assertThat(resumo.frete()).isEqualTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.seguro()).isEqualTo("2.99");
        assertThat(resumo.ajustePagamento()).isEqualTo("0.00");
        assertThat(resumo.totalFinal()).isEqualTo("262.29");
        assertThat(resumo.parcelas()).isEqualTo(3);
        assertThat(resumo.valorParcela()).isEqualTo("87.43");
        assertThat(resumo.creditoProximaCompra()).isEqualTo("5.98");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo5() {
        PedidoRequest request = new PedidoRequest(
                List.of(camiseta(), tenis()), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualTo("0.00");
        assertThat(resumo.frete()).isEqualTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualTo("4.10");
        assertThat(resumo.ajustePagamento()).isEqualTo("-20.69");
        assertThat(resumo.totalFinal()).isEqualTo("393.11");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualTo("393.11");
        assertThat(resumo.creditoProximaCompra()).isEqualTo("20.48");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void carrinhoVazioRetornaPedidoInvalido() {
        PedidoRequest request = new PedidoRequest(
                List.of(), "ECONOMICA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .satisfies(e -> assertThat(((CheckoutException) e).getCodigo()).isEqualTo("PEDIDO_INVALIDO"));
    }

    @Test
    void motoboyAcimaDoLimiteRetornaModalidadeIndisponivel() {
        ItemRequest itemPesado = new ItemRequest("Caixa", new BigDecimal("50.00"), 1, new BigDecimal("6.00"));
        PedidoRequest request = new PedidoRequest(
                List.of(itemPesado), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .satisfies(e -> assertThat(((CheckoutException) e).getCodigo()).isEqualTo("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void menos50AbaixoDoMinimoRetornaCupomNaoAplicavel() {
        ItemRequest item = new ItemRequest("Boné", new BigDecimal("50.00"), 1, new BigDecimal("0.20"));
        PedidoRequest request = new PedidoRequest(
                List.of(item), "ECONOMICA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .satisfies(e -> assertThat(((CheckoutException) e).getCodigo()).isEqualTo("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void boletoAcimaDoLimiteRetornaFormaPagamentoIndisponivel() {
        ItemRequest item = new ItemRequest("Notebook", new BigDecimal("1200.00"), 1, new BigDecimal("2.00"));
        PedidoRequest request = new PedidoRequest(
                List.of(item), "ECONOMICA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(CheckoutException.class)
                .satisfies(e -> assertThat(((CheckoutException) e).getCodigo()).isEqualTo("FORMA_PAGAMENTO_INDISPONIVEL"));
    }
}
