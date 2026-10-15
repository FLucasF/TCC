package com.loja.checkout.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.web.ResumoCompraRequest;
import com.loja.checkout.web.ResumoCompraRequest.Item;
import com.loja.checkout.web.ResumoCompraResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResumoCompraServiceTest {

    private final ResumoCompraService servico = new ResumoCompraService();

    private static final Item CAMISETA = new Item("Camiseta", bd("79.90"), 2, bd("0.30"));
    private static final Item TENIS = new Item("Tenis", bd("249.90"), 1, bd("1.20"));

    @Test
    void exemplo1() {
        ResumoCompraResponse r = servico.calcular(new ResumoCompraRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"));

        assertValores(r, "409.70", "40.97", "33.10", 2, "10.24", "-20.60", "391.47", 1, "391.47", "0.00", false);
    }

    @Test
    void exemplo2() {
        ResumoCompraResponse r = servico.calcular(new ResumoCompraRequest(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertValores(r, "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void exemplo3() {
        ResumoCompraResponse r = servico.calcular(new ResumoCompraRequest(
                List.of(new Item("Fone", bd("199.90"), 2, bd("0.25"))),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        assertValores(r, "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "0.00", false);
    }

    @Test
    void exemplo4() {
        ResumoCompraResponse r = servico.calcular(new ResumoCompraRequest(
                List.of(new Item("Meia", bd("19.90"), 7, bd("0.10")), CAMISETA),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertValores(r, "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void exemplo5() {
        ResumoCompraResponse r = servico.calcular(new ResumoCompraRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"));

        assertValores(r, "409.70", "0.00", "0.00", 2, "4.10", "-20.69", "393.11", 1, "393.11", "20.48", false);
    }

    @Test
    void ouroComProdutosAcimaDe500GanhaBrinde() {
        ResumoCompraResponse r = servico.calcular(new ResumoCompraRequest(
                List.of(new Item("Casaco", bd("300.00"), 2, bd("0.80"))),
                "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUL"));

        assertThat(r.brinde()).isTrue();
    }

    @Test
    void carrinhoVazio() {
        esperaErro(new ResumoCompraRequest(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUL"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComPrecoZero() {
        esperaErro(new ResumoCompraRequest(
                        List.of(new Item("X", bd("0.00"), 1, bd("0.10"))),
                        "EXPRESSA", null, "PIX", null, "BRONZE", "SUL"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComQuantidadeAusente() {
        esperaErro(new ResumoCompraRequest(
                        List.of(new Item("X", bd("10.00"), null, bd("0.10"))),
                        "EXPRESSA", null, "PIX", null, "BRONZE", "SUL"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivelClubeInvalido() {
        esperaErro(new ResumoCompraRequest(
                        List.of(CAMISETA), "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUL"),
                CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInvalida() {
        esperaErro(new ResumoCompraRequest(
                        List.of(CAMISETA), "EXPRESSA", null, "PIX", null, "BRONZE", "EXTERIOR"),
                CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInvalida() {
        esperaErro(new ResumoCompraRequest(
                        List.of(CAMISETA), "DRONE", null, "PIX", null, "BRONZE", "SUL"),
                CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDe5Kg() {
        esperaErro(new ResumoCompraRequest(
                        List.of(new Item("Halter", bd("50.00"), 1, bd("6.00"))),
                        "MOTOBOY", null, "PIX", null, "BRONZE", "SUL"),
                CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInexistente() {
        esperaErro(new ResumoCompraRequest(
                        List.of(CAMISETA), "EXPRESSA", "NATAL99", "PIX", null, "BRONZE", "SUL"),
                CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void menos50AbaixoDoMinimo() {
        esperaErro(new ResumoCompraRequest(
                        List.of(new Item("Meia", bd("19.90"), 2, bd("0.10"))),
                        "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUL"),
                CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInvalida() {
        esperaErro(new ResumoCompraRequest(
                        List.of(CAMISETA), "EXPRESSA", null, "CRIPTO", null, "BRONZE", "SUL"),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void pixNaoParcela() {
        esperaErro(new ResumoCompraRequest(
                        List.of(CAMISETA), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUL"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoAcimaDe12Parcelas() {
        esperaErro(new ResumoCompraRequest(
                        List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUL"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDe1000() {
        esperaErro(new ResumoCompraRequest(
                        List.of(new Item("TV", bd("1500.00"), 1, bd("8.00"))),
                        "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUL"),
                CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    private void esperaErro(ResumoCompraRequest req, CodigoErro codigo) {
        assertThatThrownBy(() -> servico.calcular(req))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(e -> ((PedidoRecusadoException) e).getCodigo())
                .isEqualTo(codigo);
    }

    private void assertValores(ResumoCompraResponse r, String subtotal, String cupom, String frete,
                               int prazo, String seguro, String ajuste, String totalFinal, int parcelas,
                               String valorParcela, String credito, boolean brinde) {
        assertThat(r.subtotalProdutos()).isEqualByComparingTo(subtotal);
        assertThat(r.descontoCupom()).isEqualByComparingTo(cupom);
        assertThat(r.frete()).isEqualByComparingTo(frete);
        assertThat(r.prazoEntregaDias()).isEqualTo(prazo);
        assertThat(r.seguro()).isEqualByComparingTo(seguro);
        assertThat(r.ajustePagamento()).isEqualByComparingTo(ajuste);
        assertThat(r.totalFinal()).isEqualByComparingTo(totalFinal);
        assertThat(r.parcelas()).isEqualTo(parcelas);
        assertThat(r.valorParcela()).isEqualByComparingTo(valorParcela);
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(credito);
        assertThat(r.brinde()).isEqualTo(brinde);
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }
}
