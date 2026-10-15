package br.com.loja.checkout.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.loja.checkout.api.ResumoRequest;
import br.com.loja.checkout.api.ResumoRequest.ItemRequest;
import br.com.loja.checkout.api.ResumoResponse;
import br.com.loja.checkout.dominio.CheckoutException;
import br.com.loja.checkout.dominio.CodigoErro;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private static final List<ItemRequest> CAMISETA_E_TENIS = List.of(
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

    @Test
    void exemplo1PixComBemvindoEmNorte() {
        ResumoResponse r = calcular(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        assertThat(r.subtotalProdutos()).hasToString("409.70");
        assertThat(r.descontoCupom()).hasToString("40.97");
        assertThat(r.frete()).hasToString("33.10");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).hasToString("10.24");
        assertThat(r.ajustePagamento()).hasToString("-20.60");
        assertThat(r.totalFinal()).hasToString("391.47");
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).hasToString("391.47");
        assertThat(r.creditoProximaCompra()).hasToString("0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2CartaoSeisVezesComJurosEmPrata() {
        ResumoResponse r = calcular(CAMISETA_E_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        assertThat(r.subtotalProdutos()).hasToString("409.70");
        assertThat(r.descontoCupom()).hasToString("0.00");
        assertThat(r.frete()).hasToString("15.60");
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertThat(r.seguro()).hasToString("6.15");
        assertThat(r.ajustePagamento()).hasToString("30.55");
        assertThat(r.totalFinal()).hasToString("462.00");
        assertThat(r.parcelas()).isEqualTo(6);
        assertThat(r.valorParcela()).hasToString("77.00");
        assertThat(r.creditoProximaCompra()).hasToString("8.19");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo3MotoboyComMenos50EmBoleto() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));
        ResumoResponse r = calcular(itens, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        assertThat(r.subtotalProdutos()).hasToString("399.80");
        assertThat(r.descontoCupom()).hasToString("50.00");
        assertThat(r.frete()).hasToString("18.00");
        assertThat(r.prazoEntregaDias()).isZero();
        assertThat(r.seguro()).hasToString("8.00");
        assertThat(r.ajustePagamento()).hasToString("3.49");
        assertThat(r.totalFinal()).hasToString("379.29");
        assertThat(r.valorParcela()).hasToString("379.29");
    }

    @Test
    void exemplo4RetiradaComLeve3Pague2EmTresVezesSemJuros() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        ResumoResponse r = calcular(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        assertThat(r.subtotalProdutos()).hasToString("299.10");
        assertThat(r.descontoCupom()).hasToString("39.80");
        assertThat(r.frete()).hasToString("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertThat(r.seguro()).hasToString("2.99");
        assertThat(r.ajustePagamento()).hasToString("0.00");
        assertThat(r.totalFinal()).hasToString("262.29");
        assertThat(r.parcelas()).isEqualTo(3);
        assertThat(r.valorParcela()).hasToString("87.43");
        assertThat(r.creditoProximaCompra()).hasToString("5.98");
    }

    @Test
    void exemplo5OuroNaoPagaFretePixSudeste() {
        ResumoResponse r = calcular(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        assertThat(r.subtotalProdutos()).hasToString("409.70");
        assertThat(r.frete()).hasToString("0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).hasToString("4.10");
        assertThat(r.ajustePagamento()).hasToString("-20.69");
        assertThat(r.totalFinal()).hasToString("393.11");
        assertThat(r.creditoProximaCompra()).hasToString("20.48");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void ouroComCompraAcimaDe500GanhaBrinde() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Casaco", new BigDecimal("300.00"), 2, new BigDecimal("0.80")));
        ResumoResponse r = calcular(itens, "ECONOMICA", null, "PIX", null, "OURO", "SUL");

        assertThat(r.brinde()).isTrue();
        assertThat(r.frete()).hasToString("0.00");
    }

    @Test
    void fretegratisZeraFreteMasMantemValorNoDesconto() {
        ResumoResponse r = calcular(CAMISETA_E_TENIS, "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUL");

        assertThat(r.frete()).hasToString("33.10");
        assertThat(r.descontoCupom()).hasToString("33.10");
    }

    @Test
    void motoboyAcimaDe5kgNaoEstaDisponivel() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Bota", new BigDecimal("100.00"), 3, new BigDecimal("1.70")));

        assertErro(itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUL", CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void menos50AbaixoDe300NaoEAplicavel() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", new BigDecimal("19.90"), 1, new BigDecimal("0.10")));

        assertErro(itens, "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUL", CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void boletoAcimaDe1000NaoEstaDisponivel() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Jaqueta", new BigDecimal("990.00"), 1, new BigDecimal("1.00")));

        assertErro(itens, "ECONOMICA", null, "BOLETO", null, "BRONZE", "SUL",
                CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void pixNaoParcela() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUL", CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoAcimaDe12ParcelasNaoEPermitido() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUL",
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void carrinhoVazioEInvalido() {
        assertErro(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUL", CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComQuantidadeZeroEInvalido() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", new BigDecimal("19.90"), 0, new BigDecimal("0.10")));

        assertErro(itens, "EXPRESSA", null, "PIX", null, "BRONZE", "SUL", CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivelInexistenteEhVerificadoAntesDaRegiao() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", null, "DIAMANTE", "MARTE",
                CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void cupomInexistenteEInformado() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", "NATAL", "PIX", null, "BRONZE", "SUL", CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void cupomEmMinusculoNaoExiste() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", "bemvindo10", "PIX", null, "BRONZE", "SUL",
                CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void formaDePagamentoAusenteEInvalida() {
        assertErro(CAMISETA_E_TENIS, "EXPRESSA", null, null, null, "BRONZE", "SUL",
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    private ResumoResponse calcular(List<ItemRequest> itens, String modalidade, String cupom, String pagamento,
            Integer parcelas, String nivel, String regiao) {
        return calculadora.calcular(new ResumoRequest(itens, modalidade, cupom, pagamento, parcelas, nivel, regiao));
    }

    private void assertErro(List<ItemRequest> itens, String modalidade, String cupom, String pagamento,
            Integer parcelas, String nivel, String regiao, CodigoErro esperado) {
        assertThatThrownBy(() -> calcular(itens, modalidade, cupom, pagamento, parcelas, nivel, regiao))
                .isInstanceOfSatisfying(CheckoutException.class, e -> assertThat(e.codigo()).isEqualTo(esperado));
    }
}
