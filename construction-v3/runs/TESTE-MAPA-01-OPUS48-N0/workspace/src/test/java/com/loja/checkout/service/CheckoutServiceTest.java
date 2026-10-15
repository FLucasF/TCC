package com.loja.checkout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static ItemRequest item(String nome, double preco, int qtd, double peso) {
        return new ItemRequest(nome, BigDecimal.valueOf(preco), qtd, BigDecimal.valueOf(peso));
    }

    private void assertErro(CheckoutRequest req, String codigo) {
        assertThatThrownBy(() -> service.calcular(req))
                .isInstanceOf(CheckoutException.class)
                .extracting("codigo")
                .isEqualTo(codigo);
    }

    private final List<ItemRequest> camisetaETenis = List.of(
            item("Camiseta", 79.90, 2, 0.30),
            item("Tênis", 249.90, 1, 1.20));

    // ----- Exemplos conferidos pelo financeiro -----

    @Test
    void exemplo1() {
        var resp = service.calcular(new CheckoutRequest(
                camisetaETenis, "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

        assertResumo(resp, "409.70", "40.97", "33.10", 2, "10.24",
                "-20.60", "391.47", 1, "391.47", "0.00", false);
    }

    @Test
    void exemplo2() {
        var resp = service.calcular(new CheckoutRequest(
                camisetaETenis, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertResumo(resp, "409.70", "0.00", "15.60", 7, "6.15",
                "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void exemplo3() {
        var resp = service.calcular(new CheckoutRequest(
                List.of(item("Fone", 199.90, 2, 0.25)),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));

        assertResumo(resp, "399.80", "50.00", "18.00", 0, "8.00",
                "3.49", "379.29", 1, "379.29", "0.00", false);
    }

    @Test
    void exemplo4() {
        var resp = service.calcular(new CheckoutRequest(
                List.of(item("Meia", 19.90, 7, 0.10), item("Camiseta", 79.90, 2, 0.30)),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertResumo(resp, "299.10", "39.80", "0.00", 1, "2.99",
                "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void exemplo5() {
        var resp = service.calcular(new CheckoutRequest(
                camisetaETenis, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"));

        assertResumo(resp, "409.70", "0.00", "0.00", 2, "4.10",
                "-20.69", "393.11", 1, "393.11", "20.48", false);
    }

    @Test
    void ouroComProdutosAcimaDe500GanhaBrinde() {
        var resp = service.calcular(new CheckoutRequest(
                List.of(item("Jaqueta", 300.00, 2, 0.80)),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resp.brinde()).isTrue();
        assertThat(resp.frete()).isEqualByComparingTo("0.00");
    }

    @Test
    void freteGratisDescontaValorDoFrete() {
        var resp = service.calcular(new CheckoutRequest(
                camisetaETenis, "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE"));

        // frete aparece normalmente e o desconto fica igual a ele
        assertThat(resp.frete()).isEqualByComparingTo("33.10");
        assertThat(resp.descontoCupom()).isEqualByComparingTo("33.10");
    }

    // ----- Erros, na ordem de verificação -----

    @Test
    void carrinhoVazio() {
        assertErro(new CheckoutRequest(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                "PEDIDO_INVALIDO");
    }

    @Test
    void itemComPrecoZero() {
        assertErro(new CheckoutRequest(List.of(item("X", 0.0, 1, 0.5)),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), "PEDIDO_INVALIDO");
    }

    @Test
    void itemComQuantidadeAusente() {
        assertErro(new CheckoutRequest(
                List.of(new ItemRequest("X", BigDecimal.TEN, null, BigDecimal.ONE)),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), "PEDIDO_INVALIDO");
    }

    @Test
    void nivelClubeInvalido() {
        assertErro(new CheckoutRequest(camisetaETenis, "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE"),
                "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoInvalida() {
        assertErro(new CheckoutRequest(camisetaETenis, "EXPRESSA", null, "PIX", 1, "BRONZE", "EXTERIOR"),
                "REGIAO_INVALIDA");
    }

    @Test
    void modalidadeInvalida() {
        assertErro(new CheckoutRequest(camisetaETenis, "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE"),
                "MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyAcimaDe5kgIndisponivel() {
        assertErro(new CheckoutRequest(List.of(item("Peso", 10.0, 6, 1.0)),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"), "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInexistente() {
        assertErro(new CheckoutRequest(camisetaETenis, "EXPRESSA", "NATAL99", "PIX", 1, "BRONZE", "SUDESTE"),
                "CUPOM_INVALIDO");
    }

    @Test
    void menos50AbaixoDe300NaoAplicavel() {
        assertErro(new CheckoutRequest(List.of(item("Barato", 50.0, 1, 0.2)),
                "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"), "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInvalida() {
        assertErro(new CheckoutRequest(camisetaETenis, "EXPRESSA", null, "CRYPTO", 1, "BRONZE", "SUDESTE"),
                "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void pixParceladoInvalido() {
        assertErro(new CheckoutRequest(camisetaETenis, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE"),
                "PARCELAMENTO_INVALIDO");
    }

    @Test
    void cartaoAcimaDe12xInvalido() {
        assertErro(new CheckoutRequest(camisetaETenis, "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE"),
                "PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDe1000Indisponivel() {
        assertErro(new CheckoutRequest(List.of(item("Caro", 600.0, 2, 0.5)),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"),
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void ordemDeVerificacaoNivelAntesDeRegiao() {
        assertErro(new CheckoutRequest(camisetaETenis, "EXPRESSA", null, "PIX", 1, "DIAMANTE", "EXTERIOR"),
                "NIVEL_CLUBE_INVALIDO");
    }

    private void assertResumo(CheckoutResponse r, String subtotal, String cupom, String frete,
                              int prazo, String seguro, String ajuste, String totalFinal,
                              int parcelas, String valorParcela, String credito, boolean brinde) {
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
}
