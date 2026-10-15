package br.com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.loja.checkout.api.ItemRequest;
import br.com.loja.checkout.api.ResumoRequest;
import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.ErroCheckout;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Cada erro e devolvido na ordem combinada, mesmo quando o pedido tem mais de um problema. */
@SpringBootTest
class ErrosDoCheckoutTest {

    private static final ItemRequest ITEM_OK =
            new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));

    @Autowired
    private CalculadoraResumo calculadora;

    private void assertErro(ResumoRequest requisicao, CodigoErro esperado) {
        assertThatThrownBy(() -> calculadora.calcular(requisicao))
                .isInstanceOf(ErroCheckout.class)
                .extracting(erro -> ((ErroCheckout) erro).codigo())
                .isEqualTo(esperado);
    }

    @Test
    @DisplayName("1: carrinho vazio vem antes de qualquer outro erro")
    void carrinhoVazio() {
        assertErro(new ResumoRequest(List.of(), "DRONE", "XPTO", "CHEQUE", 99, "DIAMANTE", "ANTARTIDA"),
                CodigoErro.PEDIDO_INVALIDO);
        assertErro(new ResumoRequest(null, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    @DisplayName("1: item com preco, quantidade ou peso zerado, negativo ou ausente")
    void itemInvalido() {
        List<ItemRequest> quebrados = Arrays.asList(
                new ItemRequest("Camiseta", null, 2, new BigDecimal("0.30")),
                new ItemRequest("Camiseta", new BigDecimal("0.00"), 2, new BigDecimal("0.30")),
                new ItemRequest("Camiseta", new BigDecimal("-79.90"), 2, new BigDecimal("0.30")),
                new ItemRequest("Camiseta", new BigDecimal("79.90"), null, new BigDecimal("0.30")),
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 0, new BigDecimal("0.30")),
                new ItemRequest("Camiseta", new BigDecimal("79.90"), -1, new BigDecimal("0.30")),
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, null),
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.00")),
                new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("-0.30")),
                null);

        quebrados.forEach(item -> assertErro(
                new ResumoRequest(Arrays.asList(ITEM_OK, item), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.PEDIDO_INVALIDO));
    }

    @Test
    @DisplayName("2: nivel do clube inexistente ou ausente")
    void nivelClubeInvalido() {
        assertErro(new ResumoRequest(List.of(ITEM_OK), "DRONE", "XPTO", "CHEQUE", 99, "DIAMANTE", "ANTARTIDA"),
                CodigoErro.NIVEL_CLUBE_INVALIDO);
        assertErro(new ResumoRequest(List.of(ITEM_OK), "EXPRESSA", null, "PIX", 1, null, "SUDESTE"),
                CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    @DisplayName("3: regiao inexistente ou ausente")
    void regiaoInvalida() {
        assertErro(new ResumoRequest(List.of(ITEM_OK), "DRONE", "XPTO", "CHEQUE", 99, "BRONZE", "ANTARTIDA"),
                CodigoErro.REGIAO_INVALIDA);
        assertErro(new ResumoRequest(List.of(ITEM_OK), "EXPRESSA", null, "PIX", 1, "BRONZE", null),
                CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    @DisplayName("4: modalidade de entrega inexistente ou ausente")
    void modalidadeInvalida() {
        assertErro(new ResumoRequest(List.of(ITEM_OK), "DRONE", "XPTO", "CHEQUE", 99, "BRONZE", "SUDESTE"),
                CodigoErro.MODALIDADE_INVALIDA);
        assertErro(new ResumoRequest(List.of(ITEM_OK), null, null, "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    @DisplayName("5: modalidade existe mas nao atende o pedido")
    void modalidadeIndisponivel() {
        ItemRequest pesado = new ItemRequest("Tijolo", new BigDecimal("10.00"), 6, new BigDecimal("1.00"));

        assertErro(new ResumoRequest(List.of(pesado), "MOTOBOY", "XPTO", "CHEQUE", 99, "BRONZE", "SUDESTE"),
                CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    @DisplayName("6: cupom inexistente")
    void cupomInvalido() {
        assertErro(new ResumoRequest(List.of(ITEM_OK), "EXPRESSA", "XPTO", "CHEQUE", 99, "BRONZE", "SUDESTE"),
                CodigoErro.CUPOM_INVALIDO);
        assertErro(new ResumoRequest(List.of(ITEM_OK), "EXPRESSA", "bemvindo10", "PIX", 1, "BRONZE", "SUDESTE"),
                CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    @DisplayName("7: cupom existe mas o pedido nao cumpre a condicao")
    void cupomNaoAplicavel() {
        assertErro(new ResumoRequest(List.of(ITEM_OK), "EXPRESSA", "MENOS50", "CHEQUE", 99, "BRONZE", "SUDESTE"),
                CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    @DisplayName("8: forma de pagamento inexistente ou ausente")
    void formaPagamentoInvalida() {
        assertErro(new ResumoRequest(List.of(ITEM_OK), "EXPRESSA", null, "CHEQUE", 99, "BRONZE", "SUDESTE"),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        assertErro(new ResumoRequest(List.of(ITEM_OK), "EXPRESSA", null, null, 1, "BRONZE", "SUDESTE"),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    @DisplayName("9: parcelamento nao permitido para a forma escolhida")
    void parcelamentoInvalido() {
        assertErro(new ResumoRequest(List.of(ITEM_OK), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
        assertErro(new ResumoRequest(List.of(ITEM_OK), "EXPRESSA", null, "BOLETO", 2, "BRONZE", "SUDESTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
        assertErro(new ResumoRequest(List.of(ITEM_OK), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
        assertErro(new ResumoRequest(List.of(ITEM_OK), "EXPRESSA", null, "CARTAO", 0, "BRONZE", "SUDESTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    @DisplayName("9 vem antes de 10: boleto parcelado e caro reclama do parcelamento primeiro")
    void parcelamentoAntesDaDisponibilidade() {
        ItemRequest sofa = new ItemRequest("Sofa", new BigDecimal("2000.00"), 1, new BigDecimal("1.00"));

        assertErro(new ResumoRequest(List.of(sofa), "EXPRESSA", null, "BOLETO", 2, "BRONZE", "SUDESTE"),
                CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    @DisplayName("10: forma existe mas nao atende o pedido")
    void formaPagamentoIndisponivel() {
        ItemRequest sofa = new ItemRequest("Sofa", new BigDecimal("2000.00"), 1, new BigDecimal("1.00"));

        assertErro(new ResumoRequest(List.of(sofa), "EXPRESSA", null, "BOLETO", 1, "BRONZE", "SUDESTE"),
                CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void o_codigo_do_erro_viaja_na_excecao() {
        ErroCheckout erro = new ErroCheckout(CodigoErro.CUPOM_INVALIDO);

        assertThat(erro.codigo().name()).isEqualTo("CUPOM_INVALIDO");
    }
}
