package com.loja.checkout;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutServiceTests {
    private final CheckoutService service = new CheckoutService();

    private ItemRequest item(String nome, String preco, int qtd, String peso) {
        ItemRequest i = new ItemRequest();
        i.nome = nome;
        i.precoUnitario = new BigDecimal(preco);
        i.quantidade = qtd;
        i.pesoKg = new BigDecimal(peso);
        return i;
    }

    private CheckoutRequest req(List<ItemRequest> itens, String modalidade, String cupom,
                                 String pagamento, Integer parcelas, String nivel, String regiao) {
        CheckoutRequest r = new CheckoutRequest();
        r.itens = itens;
        r.modalidadeEntrega = modalidade;
        r.cupom = cupom;
        r.formaPagamento = pagamento;
        r.parcelas = parcelas;
        r.nivelClube = nivel;
        r.regiao = regiao;
        return r;
    }

    private static BigDecimal d(String s) { return new BigDecimal(s); }

    @Test
    void exemplo1() {
        List<ItemRequest> itens = List.of(
                item("Camiseta", "79.90", 2, "0.30"),
                item("Tenis", "249.90", 1, "1.20"));
        CheckoutResponse r = service.calcular(req(itens, "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));
        assertThat(r.subtotalProdutos()).isEqualByComparingTo(d("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(d("40.97"));
        assertThat(r.frete()).isEqualByComparingTo(d("33.10"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo(d("10.24"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(d("-20.60"));
        assertThat(r.totalFinal()).isEqualByComparingTo(d("391.47"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualByComparingTo(d("391.47"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(d("0.00"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2() {
        List<ItemRequest> itens = List.of(
                item("Camiseta", "79.90", 2, "0.30"),
                item("Tenis", "249.90", 1, "1.20"));
        CheckoutResponse r = service.calcular(req(itens, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));
        assertThat(r.subtotalProdutos()).isEqualByComparingTo(d("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(d("0.00"));
        assertThat(r.frete()).isEqualByComparingTo(d("15.60"));
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertThat(r.seguro()).isEqualByComparingTo(d("6.15"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(d("30.55"));
        assertThat(r.totalFinal()).isEqualByComparingTo(d("462.00"));
        assertThat(r.parcelas()).isEqualTo(6);
        assertThat(r.valorParcela()).isEqualByComparingTo(d("77.00"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(d("8.19"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo3() {
        List<ItemRequest> itens = List.of(item("Fone", "199.90", 2, "0.25"));
        CheckoutResponse r = service.calcular(req(itens, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));
        assertThat(r.subtotalProdutos()).isEqualByComparingTo(d("399.80"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(d("50.00"));
        assertThat(r.frete()).isEqualByComparingTo(d("18.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertThat(r.seguro()).isEqualByComparingTo(d("8.00"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(d("3.49"));
        assertThat(r.totalFinal()).isEqualByComparingTo(d("379.29"));
        assertThat(r.valorParcela()).isEqualByComparingTo(d("379.29"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(d("0.00"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo4() {
        List<ItemRequest> itens = List.of(
                item("Meia", "19.90", 7, "0.10"),
                item("Camiseta", "79.90", 2, "0.30"));
        CheckoutResponse r = service.calcular(req(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));
        assertThat(r.subtotalProdutos()).isEqualByComparingTo(d("299.10"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(d("39.80"));
        assertThat(r.frete()).isEqualByComparingTo(d("0.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertThat(r.seguro()).isEqualByComparingTo(d("2.99"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(d("0.00"));
        assertThat(r.totalFinal()).isEqualByComparingTo(d("262.29"));
        assertThat(r.valorParcela()).isEqualByComparingTo(d("87.43"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(d("5.98"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo5() {
        List<ItemRequest> itens = List.of(
                item("Camiseta", "79.90", 2, "0.30"),
                item("Tenis", "249.90", 1, "1.20"));
        CheckoutResponse r = service.calcular(req(itens, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));
        assertThat(r.subtotalProdutos()).isEqualByComparingTo(d("409.70"));
        assertThat(r.descontoCupom()).isEqualByComparingTo(d("0.00"));
        assertThat(r.frete()).isEqualByComparingTo(d("0.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualByComparingTo(d("4.10"));
        assertThat(r.ajustePagamento()).isEqualByComparingTo(d("-20.69"));
        assertThat(r.totalFinal()).isEqualByComparingTo(d("393.11"));
        assertThat(r.valorParcela()).isEqualByComparingTo(d("393.11"));
        assertThat(r.creditoProximaCompra()).isEqualByComparingTo(d("20.48"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void carrinhoVazio() {
        assertErro(req(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), "PEDIDO_INVALIDO");
    }

    @Test
    void itemPrecoAusente() {
        ItemRequest i = new ItemRequest();
        i.nome = "x"; i.quantidade = 1; i.pesoKg = d("1");
        assertErro(req(List.of(i), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"), "PEDIDO_INVALIDO");
    }

    @Test
    void motoboyAcimaDe5kg() {
        List<ItemRequest> itens = List.of(item("Caixa", "10.00", 6, "1.0"));
        assertErro(req(itens, "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"), "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInvalido() {
        List<ItemRequest> itens = List.of(item("X", "10.00", 1, "0.1"));
        assertErro(req(itens, "EXPRESSA", "ZZZ", "PIX", 1, "BRONZE", "SUDESTE"), "CUPOM_INVALIDO");
    }

    @Test
    void menos50SemMinimo() {
        List<ItemRequest> itens = List.of(item("X", "10.00", 1, "0.1"));
        assertErro(req(itens, "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"), "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void boletoParcelado() {
        List<ItemRequest> itens = List.of(item("X", "10.00", 1, "0.1"));
        assertErro(req(itens, "EXPRESSA", null, "BOLETO", 2, "BRONZE", "SUDESTE"), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDe1000() {
        List<ItemRequest> itens = List.of(item("X", "1500.00", 1, "0.1"));
        assertErro(req(itens, "EXPRESSA", null, "BOLETO", 1, "BRONZE", "SUDESTE"), "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void nivelClubeInvalido() {
        List<ItemRequest> itens = List.of(item("X", "10.00", 1, "0.1"));
        assertErro(req(itens, "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE"), "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoInvalida() {
        List<ItemRequest> itens = List.of(item("X", "10.00", 1, "0.1"));
        assertErro(req(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "EUROPA"), "REGIAO_INVALIDA");
    }

    @Test
    void modalidadeInvalida() {
        List<ItemRequest> itens = List.of(item("X", "10.00", 1, "0.1"));
        assertErro(req(itens, "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE"), "MODALIDADE_INVALIDA");
    }

    @Test
    void brindeOuroAcimaDe500() {
        List<ItemRequest> itens = new ArrayList<>();
        itens.add(item("Casaco", "600.00", 1, "0.8"));
        CheckoutResponse r = service.calcular(req(itens, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));
        assertThat(r.brinde()).isTrue();
    }

    private void assertErro(CheckoutRequest r, String codigo) {
        assertThatThrownBy(() -> service.calcular(r))
                .isInstanceOf(ErroPedido.class)
                .hasMessage(codigo);
    }
}
