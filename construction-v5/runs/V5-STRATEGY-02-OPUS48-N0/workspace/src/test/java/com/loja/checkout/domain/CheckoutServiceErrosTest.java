package com.loja.checkout.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.api.dto.CheckoutRequest;
import com.loja.checkout.api.dto.ItemRequest;
import com.loja.checkout.api.dto.ResumoResponse;
import com.loja.checkout.domain.cupom.CupomBemVindo10;
import com.loja.checkout.domain.cupom.CupomFreteGratis;
import com.loja.checkout.domain.cupom.CupomLeve3Pague2;
import com.loja.checkout.domain.cupom.CupomMenos50;
import com.loja.checkout.domain.cupom.RegistroCupons;
import com.loja.checkout.domain.entrega.EntregaEconomica;
import com.loja.checkout.domain.entrega.EntregaExpressa;
import com.loja.checkout.domain.entrega.EntregaMotoboy;
import com.loja.checkout.domain.entrega.EntregaRetiradaLoja;
import com.loja.checkout.domain.entrega.RegistroModalidadesEntrega;
import com.loja.checkout.domain.pagamento.PagamentoBoleto;
import com.loja.checkout.domain.pagamento.PagamentoCartao;
import com.loja.checkout.domain.pagamento.PagamentoPix;
import com.loja.checkout.domain.pagamento.RegistroFormasPagamento;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckoutServiceErrosTest {

    private final CheckoutService service = new CheckoutService(
            new RegistroModalidadesEntrega(List.of(
                    new EntregaEconomica(), new EntregaExpressa(),
                    new EntregaRetiradaLoja(), new EntregaMotoboy())),
            new RegistroCupons(List.of(
                    new CupomBemVindo10(), new CupomMenos50(),
                    new CupomFreteGratis(), new CupomLeve3Pague2())),
            new RegistroFormasPagamento(List.of(
                    new PagamentoPix(), new PagamentoCartao(), new PagamentoBoleto())));

    private static ItemRequest ok() {
        return new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    }

    private void esperaErro(CheckoutRequest req, String codigo) {
        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting("codigo")
                .isEqualTo(codigo);
    }

    @Test
    void carrinhoVazio() {
        esperaErro(new CheckoutRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itensNulos() {
        esperaErro(new CheckoutRequest(null, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComQuantidadeZero() {
        ItemRequest ruim = new ItemRequest("X", new BigDecimal("10.00"), 0, new BigDecimal("0.1"));
        esperaErro(new CheckoutRequest(List.of(ruim), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComPrecoAusente() {
        ItemRequest ruim = new ItemRequest("X", null, 1, new BigDecimal("0.1"));
        esperaErro(new CheckoutRequest(List.of(ruim), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComPesoNegativo() {
        ItemRequest ruim = new ItemRequest("X", new BigDecimal("10.00"), 1, new BigDecimal("-0.1"));
        esperaErro(new CheckoutRequest(List.of(ruim), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivelClubeInvalido() {
        esperaErro(new CheckoutRequest(List.of(ok()), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE"),
                CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void nivelClubeAusente() {
        esperaErro(new CheckoutRequest(List.of(ok()), "EXPRESSA", null, "PIX", 1, null, "SUDESTE"),
                CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInvalida() {
        esperaErro(new CheckoutRequest(List.of(ok()), "EXPRESSA", null, "PIX", 1, "BRONZE", "LESTE"),
                CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInvalida() {
        esperaErro(new CheckoutRequest(List.of(ok()), "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDe5kg() {
        // 20 unidades x 0,30 kg = 6 kg > 5 kg.
        ItemRequest pesado = new ItemRequest("Casaco", new BigDecimal("50.00"), 20, new BigDecimal("0.30"));
        esperaErro(new CheckoutRequest(List.of(pesado), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInvalido() {
        esperaErro(new CheckoutRequest(List.of(ok()), "EXPRESSA", "BLACKFRIDAY", "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void cupomNaoAplicavel() {
        // MENOS50 exige R$ 300; aqui o subtotal e 159,80.
        esperaErro(new CheckoutRequest(List.of(ok()), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInvalida() {
        esperaErro(new CheckoutRequest(List.of(ok()), "EXPRESSA", null, "CRIPTO", 1, "BRONZE", "SUDESTE"),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void formaPagamentoAusente() {
        esperaErro(new CheckoutRequest(List.of(ok()), "EXPRESSA", null, null, 1, "BRONZE", "SUDESTE"),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void parcelamentoInvalidoNoPix() {
        esperaErro(new CheckoutRequest(List.of(ok()), "EXPRESSA", null, "PIX", 3, "BRONZE", "SUDESTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void parcelamentoInvalidoNoCartaoAcimaDe12() {
        esperaErro(new CheckoutRequest(List.of(ok()), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDe1000() {
        // Subtotal 2000,00 -> total do pedido passa de 1000.
        ItemRequest caro = new ItemRequest("TV", new BigDecimal("1000.00"), 2, new BigDecimal("1.0"));
        esperaErro(new CheckoutRequest(List.of(caro), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"),
                CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void ordemPedidoInvalidoVemAntesDeTudo() {
        // Tudo invalido ao mesmo tempo: o primeiro problema e o do carrinho.
        esperaErro(new CheckoutRequest(Arrays.asList((ItemRequest) null), "DRONE", "BLACKFRIDAY",
                "CRIPTO", 99, "DIAMANTE", "LESTE"), CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void parcelamentoVemAntesDeBoletoIndisponivel() {
        // Boleto em 2x e total > 1000: o parcelamento e conferido antes.
        ItemRequest caro = new ItemRequest("TV", new BigDecimal("1000.00"), 2, new BigDecimal("1.0"));
        esperaErro(new CheckoutRequest(List.of(caro), "RETIRADA_LOJA", null, "BOLETO", 2, "BRONZE", "SUDESTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void ouroComProdutosAcimaDe500GanhaBrinde() {
        ItemRequest caro = new ItemRequest("Jaqueta", new BigDecimal("300.00"), 2, new BigDecimal("0.5"));
        ResumoResponse r = service.calcular(new CheckoutRequest(List.of(caro),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));
        assertThat(r.brinde()).isTrue();
        assertThat(r.frete()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void freteGratisDescontaOValorDoFrete() {
        ResumoResponse r = service.calcular(new CheckoutRequest(
                List.of(new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))),
                "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE"));
        // Frete aparece normalmente e o desconto fica igual a ele.
        assertThat(r.frete()).isEqualByComparingTo(r.descontoCupom());
        assertThat(r.frete()).isEqualByComparingTo(new BigDecimal("27.70"));
    }
}
