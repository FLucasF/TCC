package com.loja.checkout.servico;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoPedidoRequest;
import com.loja.checkout.api.ResumoPedidoResponse;
import com.loja.checkout.erro.ErroNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static ItemRequest camiseta(int quantidade) {
        return new ItemRequest("Camiseta", new BigDecimal("79.90"), quantidade, new BigDecimal("0.30"));
    }

    private static ItemRequest tenis(int quantidade) {
        return new ItemRequest("Tênis", new BigDecimal("249.90"), quantidade, new BigDecimal("1.20"));
    }

    @Test
    void exemplo1_expressaComCupomPixBronzeNorte() {
        ResumoPedidoRequest request = new ResumoPedidoRequest(
                List.of(camiseta(2), tenis(1)),
                "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"
        );

        ResumoPedidoResponse resumo = service.calcularResumo(request);

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
    void exemplo2_economicaSemCupomCartao6xPrataCentroOeste() {
        ResumoPedidoRequest request = new ResumoPedidoRequest(
                List.of(camiseta(2), tenis(1)),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"
        );

        ResumoPedidoResponse resumo = service.calcularResumo(request);

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
    void exemplo3_motoboyComCupomBoletoBronzeNordeste() {
        ItemRequest fone = new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
        ResumoPedidoRequest request = new ResumoPedidoRequest(
                List.of(fone),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"
        );

        ResumoPedidoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(0);
        assertThat(resumo.seguro()).isEqualByComparingTo("8.00");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("379.29");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("379.29");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo4_retiradaLojaComCupomCartao3xPrataSul() {
        ItemRequest meia = new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
        ResumoPedidoRequest request = new ResumoPedidoRequest(
                List.of(meia, camiseta(2)),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"
        );

        ResumoPedidoResponse resumo = service.calcularResumo(request);

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
    void exemplo5_expressaSemCupomPixOuroSudeste() {
        ResumoPedidoRequest request = new ResumoPedidoRequest(
                List.of(camiseta(2), tenis(1)),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"
        );

        ResumoPedidoResponse resumo = service.calcularResumo(request);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualByComparingTo("4.10");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.69");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("393.11");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("393.11");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void carrinhoVazioRetornaPedidoInvalido() {
        ResumoPedidoRequest request = new ResumoPedidoRequest(
                List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE"
        );

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(ErroNegocioException.class)
                .extracting(e -> ((ErroNegocioException) e).getCodigo())
                .isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void motoboyAcimaDoLimiteRetornaModalidadeIndisponivel() {
        ItemRequest pesado = new ItemRequest("Caixa", new BigDecimal("50.00"), 1, new BigDecimal("6.00"));
        ResumoPedidoRequest request = new ResumoPedidoRequest(
                List.of(pesado), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE"
        );

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(ErroNegocioException.class)
                .extracting(e -> ((ErroNegocioException) e).getCodigo())
                .isEqualTo("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void menos50AbaixoDoMinimoRetornaCupomNaoAplicavel() {
        ItemRequest meia = new ItemRequest("Meia", new BigDecimal("19.90"), 1, new BigDecimal("0.10"));
        ResumoPedidoRequest request = new ResumoPedidoRequest(
                List.of(meia), "RETIRADA_LOJA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE"
        );

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(ErroNegocioException.class)
                .extracting(e -> ((ErroNegocioException) e).getCodigo())
                .isEqualTo("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void boletoAcimaDeMilRetornaFormaPagamentoIndisponivel() {
        ItemRequest item = new ItemRequest("Notebook", new BigDecimal("1200.00"), 1, new BigDecimal("2.00"));
        ResumoPedidoRequest request = new ResumoPedidoRequest(
                List.of(item), "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE"
        );

        assertThatThrownBy(() -> service.calcularResumo(request))
                .isInstanceOf(ErroNegocioException.class)
                .extracting(e -> ((ErroNegocioException) e).getCodigo())
                .isEqualTo("FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
