package com.loja.checkout.calculo;

import com.loja.checkout.api.PedidoRequest;
import com.loja.checkout.api.PedidoRequest.ItemRequest;
import com.loja.checkout.api.ResumoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    @Test
    void exemplo1PixComCupomEEntregaExpressa() {
        ResumoResponse r = calcular(pedido(
                List.of(camiseta(2), tenis(1)), "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"));

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
    void exemplo2CartaoEmSeisVezesComJuros() {
        ResumoResponse r = calcular(pedido(
                List.of(camiseta(2), tenis(1)), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

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
    void exemplo3BoletoComMotoboyECupomMenos50() {
        ResumoResponse r = calcular(pedido(
                List.of(fone(2)), "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(r.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(r.frete()).isEqualByComparingTo("18.00");
        assertThat(r.prazoEntregaDias()).isZero();
        assertThat(r.seguro()).isEqualByComparingTo("8.00");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(r.totalFinal()).isEqualByComparingTo("379.29");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo("379.29");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("0.00");
    }

    @Test
    void exemplo4CartaoEmTresVezesSemJurosComLeve3Pague2() {
        ResumoResponse r = calcular(pedido(
                List.of(meia(7), camiseta(2)), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

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
    }

    @Test
    void exemplo5OuroSemFretePixComBrindeAbaixoDoLimite() {
        ResumoResponse r = calcular(pedido(
                List.of(camiseta(2), tenis(1)), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"));

        assertThat(r.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(r.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(r.frete()).isEqualByComparingTo("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo("4.10");
        assertThat(r.ajustePagamento()).isEqualByComparingTo("-20.69");
        assertThat(r.totalFinal()).isEqualByComparingTo("393.11");
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void seguroArredondaMeioParaOPar() {
        assertThat(calcular(pedido(List.of(item("299.50", 1, "1")), "RETIRADA_LOJA", null, "PIX", null, "BRONZE", "SUDESTE"))
                .seguro()).isEqualByComparingTo("3.00");
        assertThat(calcular(pedido(List.of(item("298.50", 1, "1")), "RETIRADA_LOJA", null, "PIX", null, "BRONZE", "SUDESTE"))
                .seguro()).isEqualByComparingTo("2.98");
    }

    @Test
    void fretegratisDevolveFreteComoDesconto() {
        ResumoResponse r = calcular(pedido(
                List.of(camiseta(1)), "ECONOMICA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE"));

        assertThat(r.frete()).isEqualByComparingTo("12.60");
        assertThat(r.descontoCupom()).isEqualByComparingTo("12.60");
    }

    @Test
    void ouroComSubtotalAcimaDe500GanhaBrinde() {
        ResumoResponse r = calcular(pedido(
                List.of(item("600.00", 1, "1")), "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE"));

        assertThat(r.brinde()).isTrue();
    }

    @Test
    void ouroComSubtotalIgualA500NaoGanhaBrinde() {
        ResumoResponse r = calcular(pedido(
                List.of(item("500.00", 1, "1")), "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE"));

        assertThat(r.brinde()).isFalse();
    }

    @Test
    void motoboyAcimaDe5kgEhIndisponivel() {
        assertThatThrownBy(() -> calcular(pedido(
                List.of(item("50.00", 1, "5.01")), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE")))
                .hasMessage("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void motoboyComExatamente5kgEstaDisponivel() {
        ResumoResponse r = calcular(pedido(
                List.of(item("50.00", 1, "5")), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE"));

        assertThat(r.frete()).isEqualByComparingTo("18.00");
    }

    @Test
    void modalidadeIndisponivelTemPrecedenciaSobreCupomInvalido() {
        assertThatThrownBy(() -> calcular(pedido(
                List.of(item("50.00", 1, "6")), "MOTOBOY", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE")))
                .hasMessage("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void carrinhoVazioEhPedidoInvalido() {
        assertThatThrownBy(() -> calcular(pedido(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE")))
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void itemComPesoZeroEhPedidoInvalido() {
        assertThatThrownBy(() -> calcular(pedido(
                List.of(item("50.00", 1, "0")), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE")))
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void cupomEmMinusculoEhInvalido() {
        assertThatThrownBy(() -> calcular(pedido(
                List.of(camiseta(1)), "EXPRESSA", "bemvindo10", "PIX", null, "BRONZE", "SUDESTE")))
                .hasMessage("CUPOM_INVALIDO");
    }

    @Test
    void leve3Pague2SemItemComTresUnidadesNaoEhAplicavel() {
        assertThatThrownBy(() -> calcular(pedido(
                List.of(camiseta(2)), "EXPRESSA", "LEVE3PAGUE2", "PIX", null, "BRONZE", "SUDESTE")))
                .hasMessage("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void menos50AbaixoDe300NaoEhAplicavel() {
        assertThatThrownBy(() -> calcular(pedido(
                List.of(camiseta(3)), "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE")))
                .hasMessage("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void boletoAcimaDe1000EhIndisponivel() {
        assertThatThrownBy(() -> calcular(pedido(
                List.of(item("1000.01", 1, "1")), "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE")))
                .hasMessage("FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void pixComMaisDeUmaParcelaEhInvalido() {
        assertThatThrownBy(() -> calcular(pedido(
                List.of(camiseta(1)), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE")))
                .hasMessage("PARCELAMENTO_INVALIDO");
    }

    @Test
    void cartaoAcimaDe12ParcelasEhInvalido() {
        assertThatThrownBy(() -> calcular(pedido(
                List.of(camiseta(1)), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE")))
                .hasMessage("PARCELAMENTO_INVALIDO");
    }

    @Test
    void formaDePagamentoAusenteEhInvalida() {
        assertThatThrownBy(() -> calcular(pedido(
                List.of(camiseta(1)), "EXPRESSA", null, null, null, "BRONZE", "SUDESTE")))
                .hasMessage("FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void nivelDesconhecidoEhInvalido() {
        assertThatThrownBy(() -> calcular(pedido(
                List.of(camiseta(1)), "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUDESTE")))
                .hasMessage("NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoAusenteEhInvalida() {
        assertThatThrownBy(() -> calcular(pedido(
                List.of(camiseta(1)), "EXPRESSA", null, "PIX", null, "BRONZE", null)))
                .hasMessage("REGIAO_INVALIDA");
    }

    private ResumoResponse calcular(PedidoRequest pedido) {
        return calculadora.calcular(pedido);
    }

    private static PedidoRequest pedido(List<ItemRequest> itens, String modalidade, String cupom,
                                        String forma, Integer parcelas, String nivel, String regiao) {
        return new PedidoRequest(itens, modalidade, cupom, forma, parcelas, nivel, regiao);
    }

    private static ItemRequest item(String preco, int quantidade, String peso) {
        return new ItemRequest("Item", new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    private static ItemRequest camiseta(int quantidade) {
        return new ItemRequest("Camiseta", new BigDecimal("79.90"), quantidade, new BigDecimal("0.30"));
    }

    private static ItemRequest tenis(int quantidade) {
        return new ItemRequest("Tênis", new BigDecimal("249.90"), quantidade, new BigDecimal("1.20"));
    }

    private static ItemRequest fone(int quantidade) {
        return new ItemRequest("Fone", new BigDecimal("199.90"), quantidade, new BigDecimal("0.25"));
    }

    private static ItemRequest meia(int quantidade) {
        return new ItemRequest("Meia", new BigDecimal("19.90"), quantidade, new BigDecimal("0.10"));
    }
}
