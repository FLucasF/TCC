package com.loja.checkout;

import com.loja.checkout.exception.PedidoRecusadoException;
import com.loja.checkout.service.ResumoCompraService;
import com.loja.checkout.web.dto.ItemRequest;
import com.loja.checkout.web.dto.ResumoRequest;
import com.loja.checkout.web.dto.ResumoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumoCompraServiceTest {

    private final ResumoCompraService service = new ResumoCompraService();

    private static final ItemRequest CAMISETA = new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    private static final ItemRequest TENIS = new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    private static final ItemRequest FONE = new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
    private static final ItemRequest MEIA = new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));

    @Test
    void exemplo1() {
        ResumoResponse resposta = service.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"));

        assertThat(resposta.subtotalProdutos()).isEqualTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualTo("40.97");
        assertThat(resposta.frete()).isEqualTo("33.10");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.seguro()).isEqualTo("10.24");
        assertThat(resposta.ajustePagamento()).isEqualTo("-20.60");
        assertThat(resposta.totalFinal()).isEqualTo("391.47");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualTo("391.47");
        assertThat(resposta.creditoProximaCompra()).isEqualTo("0.00");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo2() {
        ResumoResponse resposta = service.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(resposta.subtotalProdutos()).isEqualTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualTo("0.00");
        assertThat(resposta.frete()).isEqualTo("15.60");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(7);
        assertThat(resposta.seguro()).isEqualTo("6.15");
        assertThat(resposta.ajustePagamento()).isEqualTo("30.55");
        assertThat(resposta.totalFinal()).isEqualTo("462.00");
        assertThat(resposta.parcelas()).isEqualTo(6);
        assertThat(resposta.valorParcela()).isEqualTo("77.00");
        assertThat(resposta.creditoProximaCompra()).isEqualTo("8.19");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo3() {
        ResumoResponse resposta = service.calcular(new ResumoRequest(
                List.of(FONE), "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        assertThat(resposta.subtotalProdutos()).isEqualTo("399.80");
        assertThat(resposta.descontoCupom()).isEqualTo("50.00");
        assertThat(resposta.frete()).isEqualTo("18.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(0);
        assertThat(resposta.seguro()).isEqualTo("8.00");
        assertThat(resposta.ajustePagamento()).isEqualTo("3.49");
        assertThat(resposta.totalFinal()).isEqualTo("379.29");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualTo("379.29");
        assertThat(resposta.creditoProximaCompra()).isEqualTo("0.00");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo4() {
        ResumoResponse resposta = service.calcular(new ResumoRequest(
                List.of(MEIA, CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(resposta.subtotalProdutos()).isEqualTo("299.10");
        assertThat(resposta.descontoCupom()).isEqualTo("39.80");
        assertThat(resposta.frete()).isEqualTo("0.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(1);
        assertThat(resposta.seguro()).isEqualTo("2.99");
        assertThat(resposta.ajustePagamento()).isEqualTo("0.00");
        assertThat(resposta.totalFinal()).isEqualTo("262.29");
        assertThat(resposta.parcelas()).isEqualTo(3);
        assertThat(resposta.valorParcela()).isEqualTo("87.43");
        assertThat(resposta.creditoProximaCompra()).isEqualTo("5.98");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo5() {
        ResumoResponse resposta = service.calcular(new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"));

        assertThat(resposta.subtotalProdutos()).isEqualTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualTo("0.00");
        assertThat(resposta.frete()).isEqualTo("0.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.seguro()).isEqualTo("4.10");
        assertThat(resposta.ajustePagamento()).isEqualTo("-20.69");
        assertThat(resposta.totalFinal()).isEqualTo("393.11");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualTo("393.11");
        assertThat(resposta.creditoProximaCompra()).isEqualTo("20.48");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void carrinhoVazioEhInvalido() {
        assertThatThrownBy(() -> service.calcular(new ResumoRequest(
                List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE")))
                .isInstanceOf(PedidoRecusadoException.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void itemComPrecoZeroEhInvalido() {
        ItemRequest itemInvalido = new ItemRequest("Boné", BigDecimal.ZERO, 1, new BigDecimal("0.10"));
        assertThatThrownBy(() -> service.calcular(new ResumoRequest(
                List.of(itemInvalido), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE")))
                .isInstanceOf(PedidoRecusadoException.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void nivelClubeInvalido() {
        assertThatThrownBy(() -> service.calcular(new ResumoRequest(
                List.of(CAMISETA), "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUDESTE")))
                .isInstanceOf(PedidoRecusadoException.class)
                .hasMessage("NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoInvalida() {
        assertThatThrownBy(() -> service.calcular(new ResumoRequest(
                List.of(CAMISETA), "EXPRESSA", null, "PIX", null, "BRONZE", "LUA")))
                .isInstanceOf(PedidoRecusadoException.class)
                .hasMessage("REGIAO_INVALIDA");
    }

    @Test
    void modalidadeInvalida() {
        assertThatThrownBy(() -> service.calcular(new ResumoRequest(
                List.of(CAMISETA), "TELEPORTE", null, "PIX", null, "BRONZE", "SUDESTE")))
                .isInstanceOf(PedidoRecusadoException.class)
                .hasMessage("MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyAcimaDoPesoEhIndisponivel() {
        ItemRequest itemPesado = new ItemRequest("Sofá", new BigDecimal("500.00"), 1, new BigDecimal("10.00"));
        assertThatThrownBy(() -> service.calcular(new ResumoRequest(
                List.of(itemPesado), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE")))
                .isInstanceOf(PedidoRecusadoException.class)
                .hasMessage("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInvalido() {
        assertThatThrownBy(() -> service.calcular(new ResumoRequest(
                List.of(CAMISETA), "EXPRESSA", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE")))
                .isInstanceOf(PedidoRecusadoException.class)
                .hasMessage("CUPOM_INVALIDO");
    }

    @Test
    void cupomNaoAplicavel() {
        assertThatThrownBy(() -> service.calcular(new ResumoRequest(
                List.of(CAMISETA), "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE")))
                .isInstanceOf(PedidoRecusadoException.class)
                .hasMessage("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInvalida() {
        assertThatThrownBy(() -> service.calcular(new ResumoRequest(
                List.of(CAMISETA), "EXPRESSA", null, "CRIPTO", null, "BRONZE", "SUDESTE")))
                .isInstanceOf(PedidoRecusadoException.class)
                .hasMessage("FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void parcelamentoInvalidoNoPix() {
        assertThatThrownBy(() -> service.calcular(new ResumoRequest(
                List.of(CAMISETA), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE")))
                .isInstanceOf(PedidoRecusadoException.class)
                .hasMessage("PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDoLimiteEhIndisponivel() {
        ItemRequest itemCaro = new ItemRequest("Notebook", new BigDecimal("2000.00"), 1, new BigDecimal("2.00"));
        assertThatThrownBy(() -> service.calcular(new ResumoRequest(
                List.of(itemCaro), "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE")))
                .isInstanceOf(PedidoRecusadoException.class)
                .hasMessage("FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
