package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.servico.ErroNegocioException;
import com.loja.checkout.servico.ResumoCompra;
import com.loja.checkout.servico.ResumoCompraService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResumoCompraServiceTest {

    private final ResumoCompraService service = new ResumoCompraService();

    @Test
    void exemplo1_expressaComBemvindo10NoPix() {
        Pedido pedido = new Pedido(List.of(
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))));

        ResumoCompra resumo = service.calcular(pedido, "EXPRESSA", "BEMVINDO10", "PIX", 1);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.09");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("381.74");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("381.74");
    }

    @Test
    void exemplo2_economicaSemCupomCartaoSeisVezes() {
        Pedido pedido = new Pedido(List.of(
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))));

        ResumoCompra resumo = service.calcular(pedido, "ECONOMICA", null, "CARTAO", 6);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("15.60");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("30.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("455.40");
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("75.90");
    }

    @Test
    void exemplo3_motoboyComMenos50NoBoleto() {
        Pedido pedido = new Pedido(List.of(
                new Item("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))));

        ResumoCompra resumo = service.calcular(pedido, "MOTOBOY", "MENOS50", "BOLETO", 1);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(0);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("371.29");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("371.29");
    }

    @Test
    void exemplo4_retiradaLojaComLeve3Pague2NoCartaoTresVezes() {
        Pedido pedido = new Pedido(List.of(
                new Item("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))));

        ResumoCompra resumo = service.calcular(pedido, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(resumo.parcelas()).isEqualTo(3);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("86.43");
    }

    @Test
    void carrinhoVazioDevolvePedidoInvalido() {
        Pedido pedido = new Pedido(List.of());

        assertThatThrownBy(() -> service.calcular(pedido, "EXPRESSA", null, "PIX", 1))
                .isInstanceOf(ErroNegocioException.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void itemComQuantidadeZeroDevolvePedidoInvalido() {
        Pedido pedido = new Pedido(List.of(
                new Item("Camiseta", new BigDecimal("79.90"), 0, new BigDecimal("0.30"))));

        assertThatThrownBy(() -> service.calcular(pedido, "EXPRESSA", null, "PIX", 1))
                .isInstanceOf(ErroNegocioException.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void modalidadeInexistenteDevolveModalidadeInvalida() {
        Pedido pedido = new Pedido(List.of(
                new Item("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))));

        assertThatThrownBy(() -> service.calcular(pedido, "DRONE", null, "PIX", 1))
                .isInstanceOf(ErroNegocioException.class)
                .hasMessage("MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyAcimaDoLimiteDevolveModalidadeIndisponivel() {
        Pedido pedido = new Pedido(List.of(
                new Item("Sofá", new BigDecimal("999.90"), 1, new BigDecimal("6"))));

        assertThatThrownBy(() -> service.calcular(pedido, "MOTOBOY", null, "PIX", 1))
                .isInstanceOf(ErroNegocioException.class)
                .hasMessage("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInexistenteDevolveCupomInvalido() {
        Pedido pedido = new Pedido(List.of(
                new Item("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))));

        assertThatThrownBy(() -> service.calcular(pedido, "EXPRESSA", "NAOEXISTE", "PIX", 1))
                .isInstanceOf(ErroNegocioException.class)
                .hasMessage("CUPOM_INVALIDO");
    }

    @Test
    void menos50AbaixoDoMinimoDevolveCupomNaoAplicavel() {
        Pedido pedido = new Pedido(List.of(
                new Item("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))));

        assertThatThrownBy(() -> service.calcular(pedido, "EXPRESSA", "MENOS50", "PIX", 1))
                .isInstanceOf(ErroNegocioException.class)
                .hasMessage("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInexistenteDevolveFormaPagamentoInvalida() {
        Pedido pedido = new Pedido(List.of(
                new Item("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))));

        assertThatThrownBy(() -> service.calcular(pedido, "EXPRESSA", null, "DINHEIRO", 1))
                .isInstanceOf(ErroNegocioException.class)
                .hasMessage("FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void pixComDuasParcelasDevolveParcelamentoInvalido() {
        Pedido pedido = new Pedido(List.of(
                new Item("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))));

        assertThatThrownBy(() -> service.calcular(pedido, "EXPRESSA", null, "PIX", 2))
                .isInstanceOf(ErroNegocioException.class)
                .hasMessage("PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDoLimiteDevolveFormaPagamentoIndisponivel() {
        Pedido pedido = new Pedido(List.of(
                new Item("Notebook", new BigDecimal("1200.00"), 1, new BigDecimal("2"))));

        assertThatThrownBy(() -> service.calcular(pedido, "RETIRADA_LOJA", null, "BOLETO", 1))
                .isInstanceOf(ErroNegocioException.class)
                .hasMessage("FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
