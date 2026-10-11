package com.loja.checkout.service;

import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.CheckoutRequest.Item;
import com.loja.checkout.web.CheckoutResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exemplos conferidos pelo financeiro e casos de erro, em cima do servico.
 */
class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static Item item(String nome, String preco, int qtd, String peso) {
        return new Item(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static void assertValor(BigDecimal valor, String esperado) {
        assertThat(valor.toPlainString()).isEqualTo(esperado);
    }

    private static List<Item> camisetaETenis() {
        return List.of(
                item("Camiseta", "79.90", 2, "0.30"),
                item("Tenis", "249.90", 1, "1.20"));
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaETenis(), "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        CheckoutResponse r = service.calcular(req);

        assertValor(r.subtotalProdutos(), "409.70");
        assertValor(r.descontoCupom(), "40.97");
        assertValor(r.frete(), "33.10");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertValor(r.seguro(), "10.24");
        assertValor(r.ajustePagamento(), "-20.60");
        assertValor(r.totalFinal(), "391.47");
        assertThat(r.parcelas()).isEqualTo(1);
        assertValor(r.valorParcela(), "391.47");
        assertValor(r.creditoProximaCompra(), "0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2_economica_semCupom_cartao6x_prata_centroOeste() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaETenis(), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        CheckoutResponse r = service.calcular(req);

        assertValor(r.subtotalProdutos(), "409.70");
        assertValor(r.descontoCupom(), "0.00");
        assertValor(r.frete(), "15.60");
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertValor(r.seguro(), "6.15");
        assertValor(r.ajustePagamento(), "30.55");
        assertValor(r.totalFinal(), "462.00");
        assertThat(r.parcelas()).isEqualTo(6);
        assertValor(r.valorParcela(), "77.00");
        assertValor(r.creditoProximaCompra(), "8.19");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        CheckoutResponse r = service.calcular(req);

        assertValor(r.subtotalProdutos(), "399.80");
        assertValor(r.descontoCupom(), "50.00");
        assertValor(r.frete(), "18.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertValor(r.seguro(), "8.00");
        assertValor(r.ajustePagamento(), "3.49");
        assertValor(r.totalFinal(), "379.29");
        assertThat(r.parcelas()).isEqualTo(1);
        assertValor(r.valorParcela(), "379.29");
        assertValor(r.creditoProximaCompra(), "0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao3x_prata_sul() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Meia", "19.90", 7, "0.10"),
                        item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        CheckoutResponse r = service.calcular(req);

        assertValor(r.subtotalProdutos(), "299.10");
        assertValor(r.descontoCupom(), "39.80");
        assertValor(r.frete(), "0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertValor(r.seguro(), "2.99");
        assertValor(r.ajustePagamento(), "0.00");
        assertValor(r.totalFinal(), "262.29");
        assertThat(r.parcelas()).isEqualTo(3);
        assertValor(r.valorParcela(), "87.43");
        assertValor(r.creditoProximaCompra(), "5.98");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo5_expressa_semCupom_pix_ouro_sudeste() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaETenis(), "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        CheckoutResponse r = service.calcular(req);

        assertValor(r.subtotalProdutos(), "409.70");
        assertValor(r.descontoCupom(), "0.00");
        assertValor(r.frete(), "0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertValor(r.seguro(), "4.10");
        assertValor(r.ajustePagamento(), "-20.69");
        assertValor(r.totalFinal(), "393.11");
        assertThat(r.parcelas()).isEqualTo(1);
        assertValor(r.valorParcela(), "393.11");
        assertValor(r.creditoProximaCompra(), "20.48");
        assertThat(r.brinde()).isFalse();
    }

    // ------------------------------------------------------------------
    // Erros (verificados na ordem combinada)
    // ------------------------------------------------------------------

    private CheckoutRequest valido() {
        return new CheckoutRequest(camisetaETenis(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
    }

    private void esperarErro(CheckoutRequest req, CodigoErro codigo) {
        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getCodigo())
                .isEqualTo(codigo);
    }

    @Test
    void carrinhoVazio_ehPedidoInvalido() {
        esperarErro(new CheckoutRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComQuantidadeZero_ehPedidoInvalido() {
        esperarErro(new CheckoutRequest(List.of(item("Camiseta", "79.90", 0, "0.30")),
                        "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComPrecoNegativo_ehPedidoInvalido() {
        esperarErro(new CheckoutRequest(List.of(item("Camiseta", "-1.00", 1, "0.30")),
                        "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivelDesconhecido_ehNivelInvalido() {
        esperarErro(new CheckoutRequest(camisetaETenis(), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE"),
                CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoAusente_ehRegiaoInvalida() {
        esperarErro(new CheckoutRequest(camisetaETenis(), "EXPRESSA", null, "PIX", 1, "BRONZE", null),
                CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeDesconhecida_ehModalidadeInvalida() {
        esperarErro(new CheckoutRequest(camisetaETenis(), "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDe5kg_ehModalidadeIndisponivel() {
        esperarErro(new CheckoutRequest(List.of(item("Peso", "10.00", 6, "1.00")),
                        "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomDesconhecido_ehCupomInvalido() {
        esperarErro(new CheckoutRequest(camisetaETenis(), "EXPRESSA", "PROMO", "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void menos50AbaixoDe300_ehCupomNaoAplicavel() {
        esperarErro(new CheckoutRequest(List.of(item("Camiseta", "79.90", 1, "0.30")),
                        "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void pagamentoDesconhecido_ehFormaInvalida() {
        esperarErro(new CheckoutRequest(camisetaETenis(), "EXPRESSA", null, "CRIPTO", 1, "BRONZE", "SUDESTE"),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void pixParceladoEhParcelamentoInvalido() {
        esperarErro(new CheckoutRequest(camisetaETenis(), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoAcimaDe12x_ehParcelamentoInvalido() {
        esperarErro(new CheckoutRequest(camisetaETenis(), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDe1000_ehFormaIndisponivel() {
        esperarErro(new CheckoutRequest(List.of(item("Sofa", "2000.00", 1, "1.00")),
                        "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"),
                CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void ouroComProdutosAcimaDe500_ganhaBrinde() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Jaqueta", "600.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE");

        CheckoutResponse r = service.calcular(req);

        assertThat(r.brinde()).isTrue();
        assertValor(r.frete(), "0.00");
    }

    @Test
    void fretegratis_desconotoIgualAoFrete() {
        CheckoutRequest req = new CheckoutRequest(
                camisetaETenis(), "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE");

        CheckoutResponse r = service.calcular(req);

        assertValor(r.frete(), "33.10");
        assertValor(r.descontoCupom(), "33.10");
    }
}
