package com.loja.checkout.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.api.CheckoutRequest;
import com.loja.checkout.api.CheckoutResponse;
import com.loja.checkout.api.ItemCarrinho;
import com.loja.checkout.domain.clube.Bronze;
import com.loja.checkout.domain.clube.NiveisClube;
import com.loja.checkout.domain.clube.Ouro;
import com.loja.checkout.domain.clube.Prata;
import com.loja.checkout.domain.cupom.Bemvindo10;
import com.loja.checkout.domain.cupom.Cupons;
import com.loja.checkout.domain.cupom.FreteGratis;
import com.loja.checkout.domain.cupom.Leve3Pague2;
import com.loja.checkout.domain.cupom.Menos50;
import com.loja.checkout.domain.entrega.Economica;
import com.loja.checkout.domain.entrega.Expressa;
import com.loja.checkout.domain.entrega.ModalidadesEntrega;
import com.loja.checkout.domain.entrega.Motoboy;
import com.loja.checkout.domain.entrega.RetiradaLoja;
import com.loja.checkout.domain.pagamento.Boleto;
import com.loja.checkout.domain.pagamento.Cartao;
import com.loja.checkout.domain.pagamento.FormasPagamento;
import com.loja.checkout.domain.pagamento.Pix;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService(
            new ModalidadesEntrega(List.of(new Economica(), new Expressa(), new RetiradaLoja(), new Motoboy())),
            new Cupons(List.of(new Bemvindo10(), new Menos50(), new FreteGratis(), new Leve3Pague2())),
            new NiveisClube(List.of(new Bronze(), new Prata(), new Ouro())),
            new FormasPagamento(List.of(new Pix(), new Boleto(), new Cartao())));

    private static ItemCarrinho item(String nome, String preco, int qtd, String peso) {
        return new ItemCarrinho(nome, new BigDecimal(preco), qtd, new BigDecimal(peso));
    }

    private static BigDecimal reais(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void exemplo1() {
        var req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        CheckoutResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualTo(reais("409.70"));
        assertThat(r.descontoCupom()).isEqualTo(reais("40.97"));
        assertThat(r.frete()).isEqualTo(reais("33.10"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualTo(reais("10.24"));
        assertThat(r.ajustePagamento()).isEqualTo(reais("-20.60"));
        assertThat(r.totalFinal()).isEqualTo(reais("391.47"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualTo(reais("391.47"));
        assertThat(r.creditoProximaCompra()).isEqualTo(reais("0.00"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2() {
        var req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        CheckoutResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualTo(reais("409.70"));
        assertThat(r.descontoCupom()).isEqualTo(reais("0.00"));
        assertThat(r.frete()).isEqualTo(reais("15.60"));
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertThat(r.seguro()).isEqualTo(reais("6.15"));
        assertThat(r.ajustePagamento()).isEqualTo(reais("30.55"));
        assertThat(r.totalFinal()).isEqualTo(reais("462.00"));
        assertThat(r.parcelas()).isEqualTo(6);
        assertThat(r.valorParcela()).isEqualTo(reais("77.00"));
        assertThat(r.creditoProximaCompra()).isEqualTo(reais("8.19"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo3() {
        var req = new CheckoutRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        CheckoutResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualTo(reais("399.80"));
        assertThat(r.descontoCupom()).isEqualTo(reais("50.00"));
        assertThat(r.frete()).isEqualTo(reais("18.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertThat(r.seguro()).isEqualTo(reais("8.00"));
        assertThat(r.ajustePagamento()).isEqualTo(reais("3.49"));
        assertThat(r.totalFinal()).isEqualTo(reais("379.29"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualTo(reais("379.29"));
        assertThat(r.creditoProximaCompra()).isEqualTo(reais("0.00"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo4() {
        var req = new CheckoutRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        CheckoutResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualTo(reais("299.10"));
        assertThat(r.descontoCupom()).isEqualTo(reais("39.80"));
        assertThat(r.frete()).isEqualTo(reais("0.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertThat(r.seguro()).isEqualTo(reais("2.99"));
        assertThat(r.ajustePagamento()).isEqualTo(reais("0.00"));
        assertThat(r.totalFinal()).isEqualTo(reais("262.29"));
        assertThat(r.parcelas()).isEqualTo(3);
        assertThat(r.valorParcela()).isEqualTo(reais("87.43"));
        assertThat(r.creditoProximaCompra()).isEqualTo(reais("5.98"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo5() {
        var req = new CheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        CheckoutResponse r = service.calcular(req);

        assertThat(r.subtotalProdutos()).isEqualTo(reais("409.70"));
        assertThat(r.descontoCupom()).isEqualTo(reais("0.00"));
        assertThat(r.frete()).isEqualTo(reais("0.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualTo(reais("4.10"));
        assertThat(r.ajustePagamento()).isEqualTo(reais("-20.69"));
        assertThat(r.totalFinal()).isEqualTo(reais("393.11"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualTo(reais("393.11"));
        assertThat(r.creditoProximaCompra()).isEqualTo(reais("20.48"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void carrinhoVazioRecusado() {
        var req = new CheckoutRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE");
        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting("codigo").isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComPesoZeroRecusado() {
        var req = new CheckoutRequest(List.of(item("X", "10.00", 1, "0")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE");
        assertThatThrownBy(() -> service.calcular(req))
                .extracting("codigo").isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void motoboyAcimaDe5kgIndisponivel() {
        var req = new CheckoutRequest(List.of(item("Peso", "10.00", 6, "1.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "NORTE");
        assertThatThrownBy(() -> service.calcular(req))
                .extracting("codigo").isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInexistenteRecusado() {
        var req = new CheckoutRequest(List.of(item("X", "10.00", 1, "0.10")),
                "EXPRESSA", "NAOEXISTE", "PIX", 1, "BRONZE", "NORTE");
        assertThatThrownBy(() -> service.calcular(req))
                .extracting("codigo").isEqualTo(CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void menos50AbaixoDoMinimoNaoAplicavel() {
        var req = new CheckoutRequest(List.of(item("X", "10.00", 1, "0.10")),
                "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "NORTE");
        assertThatThrownBy(() -> service.calcular(req))
                .extracting("codigo").isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void parcelamentoInvalidoNoPix() {
        var req = new CheckoutRequest(List.of(item("X", "10.00", 1, "0.10")),
                "EXPRESSA", null, "PIX", 3, "BRONZE", "NORTE");
        assertThatThrownBy(() -> service.calcular(req))
                .extracting("codigo").isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDeMilIndisponivel() {
        var req = new CheckoutRequest(List.of(item("Caro", "600.00", 2, "0.10")),
                "EXPRESSA", null, "BOLETO", 1, "BRONZE", "NORTE");
        assertThatThrownBy(() -> service.calcular(req))
                .extracting("codigo").isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }
}
