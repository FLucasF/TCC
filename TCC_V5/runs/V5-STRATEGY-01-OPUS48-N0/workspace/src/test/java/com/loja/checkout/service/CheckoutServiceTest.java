package com.loja.checkout.service;

import com.loja.checkout.api.CheckoutRequest;
import com.loja.checkout.api.CheckoutResponse;
import com.loja.checkout.api.ItemRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Confere o calculo do resumo com os exemplos aprovados pelo financeiro e com
 * os principais casos de erro.
 */
class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static ItemRequest item(String nome, String preco, int qtd, String peso) {
        ItemRequest i = new ItemRequest();
        i.setNome(nome);
        i.setPrecoUnitario(new BigDecimal(preco));
        i.setQuantidade(qtd);
        i.setPesoKg(new BigDecimal(peso));
        return i;
    }

    private CheckoutRequest pedido(List<ItemRequest> itens, String modalidade, String cupom,
                                   String forma, Integer parcelas, String nivel, String regiao) {
        CheckoutRequest r = new CheckoutRequest();
        r.setItens(itens);
        r.setModalidadeEntrega(modalidade);
        r.setCupom(cupom);
        r.setFormaPagamento(forma);
        r.setParcelas(parcelas);
        r.setNivelClube(nivel);
        r.setRegiao(regiao);
        return r;
    }

    private static void dinheiro(BigDecimal valor, String esperado) {
        // Confere o valor e tambem que vem com 2 casas decimais.
        assertThat(valor.toPlainString()).isEqualTo(esperado);
    }

    private final List<ItemRequest> camisetaMaisTenis =
            List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20"));

    @Test
    void exemplo1() {
        CheckoutResponse r = service.calcular(pedido(camisetaMaisTenis,
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

        dinheiro(r.subtotalProdutos(), "409.70");
        dinheiro(r.descontoCupom(), "40.97");
        dinheiro(r.frete(), "33.10");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        dinheiro(r.seguro(), "10.24");
        dinheiro(r.ajustePagamento(), "-20.60");
        dinheiro(r.totalFinal(), "391.47");
        assertThat(r.parcelas()).isEqualTo(1);
        dinheiro(r.valorParcela(), "391.47");
        dinheiro(r.creditoProximaCompra(), "0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2() {
        CheckoutResponse r = service.calcular(pedido(camisetaMaisTenis,
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        dinheiro(r.subtotalProdutos(), "409.70");
        dinheiro(r.descontoCupom(), "0.00");
        dinheiro(r.frete(), "15.60");
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        dinheiro(r.seguro(), "6.15");
        dinheiro(r.ajustePagamento(), "30.55");
        dinheiro(r.totalFinal(), "462.00");
        assertThat(r.parcelas()).isEqualTo(6);
        dinheiro(r.valorParcela(), "77.00");
        dinheiro(r.creditoProximaCompra(), "8.19");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo3() {
        CheckoutResponse r = service.calcular(pedido(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        dinheiro(r.subtotalProdutos(), "399.80");
        dinheiro(r.descontoCupom(), "50.00");
        dinheiro(r.frete(), "18.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        dinheiro(r.seguro(), "8.00");
        dinheiro(r.ajustePagamento(), "3.49");
        dinheiro(r.totalFinal(), "379.29");
        assertThat(r.parcelas()).isEqualTo(1);
        dinheiro(r.valorParcela(), "379.29");
        dinheiro(r.creditoProximaCompra(), "0.00");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo4() {
        CheckoutResponse r = service.calcular(pedido(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        dinheiro(r.subtotalProdutos(), "299.10");
        dinheiro(r.descontoCupom(), "39.80");
        dinheiro(r.frete(), "0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        dinheiro(r.seguro(), "2.99");
        dinheiro(r.ajustePagamento(), "0.00");
        dinheiro(r.totalFinal(), "262.29");
        assertThat(r.parcelas()).isEqualTo(3);
        dinheiro(r.valorParcela(), "87.43");
        dinheiro(r.creditoProximaCompra(), "5.98");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo5() {
        CheckoutResponse r = service.calcular(pedido(camisetaMaisTenis,
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        dinheiro(r.subtotalProdutos(), "409.70");
        dinheiro(r.descontoCupom(), "0.00");
        dinheiro(r.frete(), "0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        dinheiro(r.seguro(), "4.10");
        dinheiro(r.ajustePagamento(), "-20.69");
        dinheiro(r.totalFinal(), "393.11");
        assertThat(r.parcelas()).isEqualTo(1);
        dinheiro(r.valorParcela(), "393.11");
        dinheiro(r.creditoProximaCompra(), "20.48");
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemploDoAnexo() {
        CheckoutResponse r = service.calcular(pedido(camisetaMaisTenis,
                "EXPRESSA", "BEMVINDO10", "PIX", null, "OURO", "SUDESTE"));

        dinheiro(r.subtotalProdutos(), "409.70");
        dinheiro(r.descontoCupom(), "40.97");
        dinheiro(r.frete(), "0.00");
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        dinheiro(r.seguro(), "4.10");
        dinheiro(r.ajustePagamento(), "-18.64");
        dinheiro(r.totalFinal(), "354.19");
        dinheiro(r.valorParcela(), "354.19");
        dinheiro(r.creditoProximaCompra(), "20.48");
        assertThat(r.brinde()).isFalse();
    }

    private CodigoErro erroDe(CheckoutRequest req) {
        try {
            service.calcular(req);
        } catch (CheckoutException e) {
            return e.getCodigo();
        }
        throw new AssertionError("esperava recusa, mas o pedido foi aceito");
    }

    @Test
    void carrinhoVazioEhPedidoInvalido() {
        assertThat(erroDe(pedido(List.of(),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComPesoZeroEhPedidoInvalido() {
        assertThat(erroDe(pedido(List.of(item("X", "10.00", 1, "0")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivelClubeInvalido() {
        assertThat(erroDe(pedido(camisetaMaisTenis,
                "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE")))
                .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInvalida() {
        assertThat(erroDe(pedido(camisetaMaisTenis,
                "EXPRESSA", null, "PIX", 1, "BRONZE", "EXTERIOR")))
                .isEqualTo(CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInvalida() {
        assertThat(erroDe(pedido(camisetaMaisTenis,
                "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDe5kgEhIndisponivel() {
        assertThat(erroDe(pedido(List.of(item("Peso", "10.00", 1, "6")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInexistente() {
        assertThat(erroDe(pedido(camisetaMaisTenis,
                "EXPRESSA", "NAOEXISTE", "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void menos50AbaixoDoMinimoNaoEhAplicavel() {
        assertThat(erroDe(pedido(List.of(item("Barato", "10.00", 1, "0.10")),
                "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInvalida() {
        assertThat(erroDe(pedido(camisetaMaisTenis,
                "EXPRESSA", null, "CHEQUE", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void pixParceladoEhParcelamentoInvalido() {
        assertThat(erroDe(pedido(camisetaMaisTenis,
                "EXPRESSA", null, "PIX", 3, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoAcimaDe12xEhParcelamentoInvalido() {
        assertThat(erroDe(pedido(camisetaMaisTenis,
                "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDe1000EhIndisponivel() {
        assertThat(erroDe(pedido(List.of(item("Caro", "600.00", 2, "0.50")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() {
        CheckoutResponse r = service.calcular(pedido(
                List.of(item("Jaqueta", "600.00", 1, "1.00")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));
        assertThat(r.brinde()).isTrue();
        dinheiro(r.creditoProximaCompra(), "30.00");
    }

    @Test
    void semCorpoDeItensNaoDeixaPassar() {
        assertThatThrownBy(() -> service.calcular(pedido(null,
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isInstanceOf(CheckoutException.class);
    }
}
