package com.loja.checkout;

import com.loja.checkout.domain.CheckoutService;
import com.loja.checkout.domain.erro.CheckoutException;
import com.loja.checkout.domain.erro.ErroCheckout;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pedidos recusados: cada codigo e a ordem em que o primeiro problema vence. */
@SpringBootTest
class RecusasTest {

    @Autowired
    CheckoutService service;

    private static final ItemRequest ITEM_OK = new ItemRequest("Item", new BigDecimal("100.00"), 1, new BigDecimal("0.50"));

    private void recusa(ResumoRequest req, ErroCheckout esperado) {
        assertThatThrownBy(() -> service.resumir(req))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).getErro())
                .isEqualTo(esperado);
    }

    private ResumoRequest base(List<ItemRequest> itens, String modalidade, String cupom,
                               String pagamento, Integer parcelas, String nivel, String regiao) {
        return new ResumoRequest(itens, modalidade, cupom, pagamento, parcelas, nivel, regiao);
    }

    @Test
    void carrinhoVazio() {
        recusa(base(List.of(), "ECONOMICA", null, "PIX", 1, "BRONZE", "SUDESTE"), ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void itemComPrecoZero() {
        ItemRequest ruim = new ItemRequest("X", BigDecimal.ZERO, 1, new BigDecimal("0.5"));
        recusa(base(List.of(ruim), "ECONOMICA", null, "PIX", 1, "BRONZE", "SUDESTE"), ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void itemComQuantidadeAusente() {
        ItemRequest ruim = new ItemRequest("X", new BigDecimal("10.00"), null, new BigDecimal("0.5"));
        recusa(base(List.of(ruim), "ECONOMICA", null, "PIX", 1, "BRONZE", "SUDESTE"), ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void itemComPesoNegativo() {
        ItemRequest ruim = new ItemRequest("X", new BigDecimal("10.00"), 1, new BigDecimal("-0.5"));
        recusa(base(List.of(ruim), "ECONOMICA", null, "PIX", 1, "BRONZE", "SUDESTE"), ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void nivelInexistente() {
        recusa(base(List.of(ITEM_OK), "ECONOMICA", null, "PIX", 1, "DIAMANTE", "SUDESTE"), ErroCheckout.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInexistente() {
        recusa(base(List.of(ITEM_OK), "ECONOMICA", null, "PIX", 1, "BRONZE", "LESTE"), ErroCheckout.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInexistente() {
        recusa(base(List.of(ITEM_OK), "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE"), ErroCheckout.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDe5kg() {
        ItemRequest pesado = new ItemRequest("Pesado", new BigDecimal("100.00"), 1, new BigDecimal("6.0"));
        recusa(base(List.of(pesado), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"), ErroCheckout.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInexistente() {
        recusa(base(List.of(ITEM_OK), "ECONOMICA", "XPTO", "PIX", 1, "BRONZE", "SUDESTE"), ErroCheckout.CUPOM_INVALIDO);
    }

    @Test
    void menos50AbaixoDoMinimo() {
        recusa(base(List.of(ITEM_OK), "ECONOMICA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"), ErroCheckout.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void pagamentoInexistente() {
        recusa(base(List.of(ITEM_OK), "ECONOMICA", null, "CHEQUE", 1, "BRONZE", "SUDESTE"), ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void pixParceladoNaoPode() {
        recusa(base(List.of(ITEM_OK), "ECONOMICA", null, "PIX", 2, "BRONZE", "SUDESTE"), ErroCheckout.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoAcimaDe12x() {
        recusa(base(List.of(ITEM_OK), "ECONOMICA", null, "CARTAO", 13, "BRONZE", "SUDESTE"), ErroCheckout.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDe1000() {
        ItemRequest caro = new ItemRequest("Caro", new BigDecimal("249.90"), 5, new BigDecimal("0.10"));
        recusa(base(List.of(caro), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"), ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void ordemPedidoInvalidoVenceNivel() {
        recusa(base(List.of(), "ECONOMICA", null, "PIX", 1, "DIAMANTE", "LESTE"), ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void ordemNivelVenceRegiao() {
        recusa(base(List.of(ITEM_OK), "ECONOMICA", null, "PIX", 1, "DIAMANTE", "LESTE"), ErroCheckout.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void ordemModalidadeIndisponivelVenceCupom() {
        ItemRequest pesado = new ItemRequest("Pesado", new BigDecimal("100.00"), 1, new BigDecimal("6.0"));
        recusa(base(List.of(pesado), "MOTOBOY", "XPTO", "PIX", 1, "BRONZE", "SUDESTE"), ErroCheckout.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void ordemCupomVencePagamento() {
        recusa(base(List.of(ITEM_OK), "ECONOMICA", "XPTO", "CHEQUE", 1, "BRONZE", "SUDESTE"), ErroCheckout.CUPOM_INVALIDO);
    }

    @Test
    void ordemParcelamentoVenceDisponibilidadeBoleto() {
        // Boleto parcelado (invalido) e acima de 1000: o parcelamento (9) vence a disponibilidade (10).
        ItemRequest caro = new ItemRequest("Caro", new BigDecimal("249.90"), 5, new BigDecimal("0.10"));
        recusa(base(List.of(caro), "RETIRADA_LOJA", null, "BOLETO", 3, "BRONZE", "SUDESTE"), ErroCheckout.PARCELAMENTO_INVALIDO);
    }

    @Test
    void motoboyExatamente5kgEhAceito() {
        ItemRequest noLimite = new ItemRequest("Limite", new BigDecimal("100.00"), 1, new BigDecimal("5.0"));
        var r = service.resumir(base(List.of(noLimite), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"));
        assertThat(r.frete()).isEqualByComparingTo("18.00");
    }
}
