package com.loja.checkout;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.ErroCheckoutException;
import com.loja.checkout.dominio.ItemInformado;
import com.loja.checkout.dominio.PedidoInformado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RegrasDeRecusaTest {

    private static final ItemInformado CAMISETA =
            new ItemInformado("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));

    @Autowired
    private CalculadoraResumo calculadora;

    @Test
    void carrinhoVazio() {
        recusa(pedido(List.of()), ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void itemComQuantidadeZero() {
        recusa(pedido(List.of(new ItemInformado("Meia", new BigDecimal("19.90"), 0, new BigDecimal("0.10")))),
                ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void itemComPrecoNegativo() {
        recusa(pedido(List.of(new ItemInformado("Meia", new BigDecimal("-1.00"), 1, new BigDecimal("0.10")))),
                ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void itemSemPeso() {
        recusa(pedido(List.of(new ItemInformado("Meia", new BigDecimal("19.90"), 1, null))),
                ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void pedidoInvalidoVemAntesDosOutrosProblemas() {
        recusa(new PedidoInformado(List.of(), "VOANDO", "NAOEXISTE", "CHEQUE", 99, "DIAMANTE", "LESTE"),
                ErroCheckout.PEDIDO_INVALIDO);
    }

    @Test
    void nivelClubeInexistente() {
        recusa(new PedidoInformado(List.of(CAMISETA), "VOANDO", null, "CHEQUE", 1, "DIAMANTE", "LESTE"),
                ErroCheckout.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void nivelClubeAusente() {
        recusa(new PedidoInformado(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, null, "SUDESTE"),
                ErroCheckout.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInexistenteVemAntesDaModalidade() {
        recusa(new PedidoInformado(List.of(CAMISETA), "VOANDO", null, "PIX", 1, "BRONZE", "LESTE"),
                ErroCheckout.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInexistente() {
        recusa(new PedidoInformado(List.of(CAMISETA), "VOANDO", null, "PIX", 1, "BRONZE", "SUDESTE"),
                ErroCheckout.MODALIDADE_INVALIDA);
    }

    @Test
    void modalidadeAusente() {
        recusa(new PedidoInformado(List.of(CAMISETA), null, null, "PIX", 1, "BRONZE", "SUDESTE"),
                ErroCheckout.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDeCincoQuilos() {
        ItemInformado pesado = new ItemInformado("Mala", new BigDecimal("100.00"), 1, new BigDecimal("5.01"));
        recusa(new PedidoInformado(List.of(pesado), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE"),
                ErroCheckout.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void motoboyAceitaExatamenteCincoQuilos() {
        ItemInformado noLimite = new ItemInformado("Mala", new BigDecimal("100.00"), 1, new BigDecimal("5.00"));
        assertThat(calculadora.calcular(
                new PedidoInformado(List.of(noLimite), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE")).frete())
                .isEqualByComparingTo("18.00");
    }

    @Test
    void cupomInexistente() {
        recusa(new PedidoInformado(List.of(CAMISETA), "EXPRESSA", "NAOEXISTE", "PIX", 1, "BRONZE", "SUDESTE"),
                ErroCheckout.CUPOM_INVALIDO);
    }

    @Test
    void cupomEmMinusculasNaoExiste() {
        recusa(new PedidoInformado(List.of(CAMISETA), "EXPRESSA", "bemvindo10", "PIX", 1, "BRONZE", "SUDESTE"),
                ErroCheckout.CUPOM_INVALIDO);
    }

    @Test
    void menos50AbaixoDoMinimo() {
        recusa(new PedidoInformado(List.of(CAMISETA), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"),
                ErroCheckout.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInexistente() {
        recusa(new PedidoInformado(List.of(CAMISETA), "EXPRESSA", null, "CHEQUE", 1, "BRONZE", "SUDESTE"),
                ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void formaPagamentoAusente() {
        recusa(new PedidoInformado(List.of(CAMISETA), "EXPRESSA", null, null, 1, "BRONZE", "SUDESTE"),
                ErroCheckout.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void pixNaoParcela() {
        recusa(new PedidoInformado(List.of(CAMISETA), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE"),
                ErroCheckout.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoNaoParcela() {
        recusa(new PedidoInformado(List.of(CAMISETA), "EXPRESSA", null, "BOLETO", 3, "BRONZE", "SUDESTE"),
                ErroCheckout.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoAcimaDeDozeParcelas() {
        recusa(new PedidoInformado(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE"),
                ErroCheckout.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoComZeroParcelas() {
        recusa(new PedidoInformado(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 0, "BRONZE", "SUDESTE"),
                ErroCheckout.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDeMilReais() {
        ItemInformado caro = new ItemInformado("Jaqueta", new BigDecimal("600.00"), 2, new BigDecimal("1.00"));
        recusa(new PedidoInformado(List.of(caro), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"),
                ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void parcelamentoInvalidoVemAntesDaIndisponibilidade() {
        ItemInformado caro = new ItemInformado("Jaqueta", new BigDecimal("600.00"), 2, new BigDecimal("1.00"));
        recusa(new PedidoInformado(List.of(caro), "RETIRADA_LOJA", null, "BOLETO", 2, "BRONZE", "SUDESTE"),
                ErroCheckout.PARCELAMENTO_INVALIDO);
    }

    private PedidoInformado pedido(List<ItemInformado> itens) {
        return new PedidoInformado(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
    }

    private void recusa(PedidoInformado pedido, ErroCheckout esperado) {
        assertThatThrownBy(() -> calculadora.calcular(pedido))
                .isInstanceOf(ErroCheckoutException.class)
                .extracting(excecao -> ((ErroCheckoutException) excecao).erro())
                .isEqualTo(esperado);
    }
}
