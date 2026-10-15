package com.loja.checkout.service;

import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.ItemRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CheckoutServiceErrosTest {

    @Autowired
    CheckoutService servico;

    private static ItemRequest item(String preco, int qtd, String peso) {
        return new ItemRequest("X", preco == null ? null : new BigDecimal(preco), qtd,
                peso == null ? null : new BigDecimal(peso));
    }

    private void esperaErro(CheckoutRequest pedido, CodigoErro esperado) {
        assertThatThrownBy(() -> servico.calcular(pedido))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo(esperado);
    }

    private CheckoutRequest validoCom(List<ItemRequest> itens, String entrega, String cupom,
                                      String pagamento, Integer parcelas, String clube, String regiao) {
        return new CheckoutRequest(itens, entrega, cupom, pagamento, parcelas, clube, regiao);
    }

    private final List<ItemRequest> umItem = List.of(item("100.00", 1, "0.50"));

    @Test
    void carrinhoVazio() {
        esperaErro(validoCom(List.of(), "ECONOMICA", null, "PIX", 1, "BRONZE", "SUL"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComPesoZero() {
        esperaErro(validoCom(List.of(item("100.00", 1, "0")), "ECONOMICA", null, "PIX", 1, "BRONZE", "SUL"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComQuantidadeAusente() {
        var pedido = new CheckoutRequest(List.of(new ItemRequest("X", new BigDecimal("10.00"), null, new BigDecimal("1"))),
                "ECONOMICA", null, "PIX", 1, "BRONZE", "SUL");
        esperaErro(pedido, CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivelClubeInexistente() {
        esperaErro(validoCom(umItem, "ECONOMICA", null, "PIX", 1, "DIAMANTE", "SUL"),
                CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoAusente() {
        esperaErro(validoCom(umItem, "ECONOMICA", null, "PIX", 1, "BRONZE", null),
                CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInexistente() {
        esperaErro(validoCom(umItem, "DRONE", null, "PIX", 1, "BRONZE", "SUL"),
                CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDoPeso() {
        esperaErro(validoCom(List.of(item("100.00", 6, "1.00")), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUL"),
                CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInexistente() {
        esperaErro(validoCom(umItem, "ECONOMICA", "NATAL99", "PIX", 1, "BRONZE", "SUL"),
                CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void menos50AbaixoDoMinimo() {
        esperaErro(validoCom(umItem, "ECONOMICA", "MENOS50", "PIX", 1, "BRONZE", "SUL"),
                CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInexistente() {
        esperaErro(validoCom(umItem, "ECONOMICA", null, "CRIPTO", 1, "BRONZE", "SUL"),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void pixParceladoNaoPode() {
        esperaErro(validoCom(umItem, "ECONOMICA", null, "PIX", 2, "BRONZE", "SUL"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoAcimaDe12() {
        esperaErro(validoCom(umItem, "ECONOMICA", null, "CARTAO", 13, "BRONZE", "SUL"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDeMil() {
        esperaErro(validoCom(List.of(item("2000.00", 1, "1.00")), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUL"),
                CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void ordemContaItensAntesDeClube() {
        // Carrinho vazio E clube invalido: o primeiro problema e o do carrinho.
        esperaErro(validoCom(List.of(), "ECONOMICA", null, "PIX", 1, "DIAMANTE", "SUL"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void brindeOuroAcimaDe500() {
        var pedido = validoCom(List.of(item("600.00", 1, "0.50")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUL");
        assertThat(servico.calcular(pedido).brinde()).isTrue();
    }
}
