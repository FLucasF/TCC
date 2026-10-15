package com.loja.checkout.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.api.CheckoutRequest;
import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.ErroCheckout;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Confere o serviço contra os exemplos fechados pelo financeiro e os casos de recusa. */
@SpringBootTest
class ResumoServiceTest {

    @Autowired
    private ResumoService service;

    private static ItemRequest item(String nome, String preco, int qtd, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static BigDecimal reais(String v) {
        return new BigDecimal(v);
    }

    @Test
    void exemplo1() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        ResumoResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("40.97"));
        assertThat(r.frete()).isEqualByComparingTo(reais("33.10"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo(reais("10.24"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("-20.60"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("391.47"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("391.47"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.frete()).isEqualByComparingTo(reais("15.60"));
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertThat(r.seguro()).isEqualByComparingTo(reais("6.15"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("30.55"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("462.00"));
        assertThat(r.parcelas()).isEqualTo(6);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("77.00"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("8.19"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo3() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");

        ResumoResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("399.80"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("50.00"));
        assertThat(r.frete()).isEqualByComparingTo(reais("18.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertThat(r.seguro()).isEqualByComparingTo(reais("8.00"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("3.49"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("379.29"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("379.29"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo4() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("299.10"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("39.80"));
        assertThat(r.frete()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertThat(r.seguro()).isEqualByComparingTo(reais("2.99"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("262.29"));
        assertThat(r.parcelas()).isEqualTo(3);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("87.43"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("5.98"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo5() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.frete()).isEqualByComparingTo(reais("0.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo(reais("4.10"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(reais("-20.69"));
        assertThat(r.totalFinal()).isEqualByComparingTo(reais("393.11"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo(reais("393.11"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(reais("20.48"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void brindeOuroAcimaDe500() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Casaco", "300.00", 2, "0.50")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualByComparingTo(reais("600.00"));
        assertThat(r.brinde()).isTrue();
    }

    @Test
    void parcelasAusentesViram1() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Fone", "199.90", 1, "0.25")),
                "RETIRADA_LOJA", null, "PIX", null, "BRONZE", "SUDESTE");

        ResumoResponse r = service.calcular(req);

        assertThat(r.parcelas()).isEqualTo(1);
    }

    private CheckoutRequest comItens(List<ItemRequest> itens) {
        return new CheckoutRequest(itens, "RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUDESTE");
    }

    private void esperaErro(CheckoutRequest req, ErroCheckout erro) {
        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getErro())
                .isEqualTo(erro);
    }

    @Test
    void carrinhoVazio() {
        esperaErro(comItens(List.of()), ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void itemComPrecoZero() {
        esperaErro(comItens(List.of(item("X", "0.00", 1, "0.10"))), ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void itemComQuantidadeNegativa() {
        esperaErro(comItens(List.of(item("X", "10.00", -1, "0.10"))), ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void itemComPesoAusente() {
        esperaErro(comItens(List.of(new ItemRequest("X", new BigDecimal("10.00"), 1, null))),
                ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void nivelClubeInvalido() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")),
                "RETIRADA_LOJA", null, "PIX", 1, "DIAMANTE", "SUDESTE");
        esperaErro(req, ErroCheckout.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInvalida() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")),
                "RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "EXTERIOR");
        esperaErro(req, ErroCheckout.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInvalida() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")),
                "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE");
        esperaErro(req, ErroCheckout.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDe5kg() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("Haltere", "100.00", 1, "6.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");
        esperaErro(req, ErroCheckout.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInexistente() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")),
                "RETIRADA_LOJA", "NAOEXISTE", "PIX", 1, "BRONZE", "SUDESTE");
        esperaErro(req, ErroCheckout.CUPOM_INVALIDO);
    }

    @Test
    void menos50AbaixoDoMinimo() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("X", "100.00", 1, "0.10")),
                "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");
        esperaErro(req, ErroCheckout.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInvalida() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")),
                "RETIRADA_LOJA", null, "CRIPTO", 1, "BRONZE", "SUDESTE");
        esperaErro(req, ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void pixParcelado() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")),
                "RETIRADA_LOJA", null, "PIX", 2, "BRONZE", "SUDESTE");
        esperaErro(req, ErroCheckout.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoAcimaDe12x() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("X", "10.00", 1, "0.10")),
                "RETIRADA_LOJA", null, "CARTAO", 13, "BRONZE", "SUDESTE");
        esperaErro(req, ErroCheckout.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDe1000() {
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("TV", "1200.00", 1, "3.00")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE");
        esperaErro(req, ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void ordemErro_itemInvalidoAntesDeClube() {
        // item inválido E clube inválido: deve devolver o primeiro (PEDIDO_INVALIDO)
        CheckoutRequest req = new CheckoutRequest(
                List.of(item("X", "0.00", 1, "0.10")),
                "RETIRADA_LOJA", null, "PIX", 1, "DIAMANTE", "SUDESTE");
        esperaErro(req, ErroCheckout.PEDIDO_INVALIDO);
    }
}
