package com.loja.checkout;

import com.loja.checkout.api.ItemRequisicao;
import com.loja.checkout.api.ResumoRequisicao;
import com.loja.checkout.dominio.Erro;
import com.loja.checkout.dominio.PedidoRecusadoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** As recusas e a ordem em que os problemas sao conferidos. */
@DisplayName("Pedido recusado")
class PedidoRecusadoTest {

    private static final ItemRequisicao CAMISETA =
            new ItemRequisicao("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    private static final ItemRequisicao CINCO_TENIS =
            new ItemRequisicao("Tenis", new BigDecimal("249.90"), 5, new BigDecimal("1.20"));

    private final CalculoResumoService servico = new CalculoResumoService();

    @Test
    @DisplayName("carrinho vazio ou ausente")
    void carrinhoVazio() {
        assertThat(recusa(valido().itens(List.of()))).isEqualTo(Erro.PEDIDO_INVALIDO);
        assertThat(recusa(valido().itens(null))).isEqualTo(Erro.PEDIDO_INVALIDO);
    }

    @Test
    @DisplayName("item com preco, quantidade ou peso zero, negativo ou ausente")
    void itemInvalido() {
        assertThat(recusa(valido().itens(List.of(
                new ItemRequisicao("X", BigDecimal.ZERO, 1, new BigDecimal("0.30"))))))
                .isEqualTo(Erro.PEDIDO_INVALIDO);
        assertThat(recusa(valido().itens(List.of(
                new ItemRequisicao("X", new BigDecimal("10.00"), 0, new BigDecimal("0.30"))))))
                .isEqualTo(Erro.PEDIDO_INVALIDO);
        assertThat(recusa(valido().itens(List.of(
                new ItemRequisicao("X", new BigDecimal("10.00"), 1, new BigDecimal("-1"))))))
                .isEqualTo(Erro.PEDIDO_INVALIDO);
        assertThat(recusa(valido().itens(Arrays.asList(CAMISETA,
                new ItemRequisicao("X", new BigDecimal("10.00"), null, new BigDecimal("0.30"))))))
                .isEqualTo(Erro.PEDIDO_INVALIDO);
    }

    @Test
    @DisplayName("nivel do clube inexistente ou ausente")
    void nivelInvalido() {
        assertThat(recusa(valido().nivel("DIAMANTE"))).isEqualTo(Erro.NIVEL_CLUBE_INVALIDO);
        assertThat(recusa(valido().nivel(null))).isEqualTo(Erro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    @DisplayName("regiao inexistente ou ausente")
    void regiaoInvalida() {
        assertThat(recusa(valido().regiao("EUROPA"))).isEqualTo(Erro.REGIAO_INVALIDA);
        assertThat(recusa(valido().regiao(null))).isEqualTo(Erro.REGIAO_INVALIDA);
    }

    @Test
    @DisplayName("modalidade de entrega inexistente ou ausente")
    void modalidadeInvalida() {
        assertThat(recusa(valido().entrega("DRONE"))).isEqualTo(Erro.MODALIDADE_INVALIDA);
        assertThat(recusa(valido().entrega(null))).isEqualTo(Erro.MODALIDADE_INVALIDA);
    }

    @Test
    @DisplayName("motoboy acima de 5 kg")
    void motoboyAcimaDoLimite() {
        assertThat(recusa(valido().itens(List.of(CINCO_TENIS)).entrega("MOTOBOY")))
                .isEqualTo(Erro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    @DisplayName("cupom inexistente")
    void cupomInvalido() {
        assertThat(recusa(valido().cupom("PROMO999"))).isEqualTo(Erro.CUPOM_INVALIDO);
    }

    @Test
    @DisplayName("MENOS50 abaixo de R$ 300,00 em produtos")
    void cupomNaoAplicavel() {
        assertThat(recusa(valido().cupom("MENOS50"))).isEqualTo(Erro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    @DisplayName("forma de pagamento inexistente ou ausente")
    void formaPagamentoInvalida() {
        assertThat(recusa(valido().pagamento("CRIPTO"))).isEqualTo(Erro.FORMA_PAGAMENTO_INVALIDA);
        assertThat(recusa(valido().pagamento(null))).isEqualTo(Erro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    @DisplayName("Pix e boleto so a vista; cartao de 1 a 12")
    void parcelamentoInvalido() {
        assertThat(recusa(valido().pagamento("PIX").parcelas(2)))
                .isEqualTo(Erro.PARCELAMENTO_INVALIDO);
        assertThat(recusa(valido().pagamento("BOLETO").parcelas(3)))
                .isEqualTo(Erro.PARCELAMENTO_INVALIDO);
        assertThat(recusa(valido().pagamento("CARTAO").parcelas(13)))
                .isEqualTo(Erro.PARCELAMENTO_INVALIDO);
        assertThat(recusa(valido().pagamento("CARTAO").parcelas(0)))
                .isEqualTo(Erro.PARCELAMENTO_INVALIDO);
    }

    @Test
    @DisplayName("boleto acima de R$ 1.000,00 no total do pedido")
    void boletoAcimaDoLimite() {
        assertThat(recusa(valido().itens(List.of(CINCO_TENIS)).pagamento("BOLETO")))
                .isEqualTo(Erro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    @DisplayName("com varios problemas, devolve o primeiro da ordem")
    void devolveOPrimeiroProblemaDaOrdem() {
        Requisicao tudoErrado = valido().itens(null).entrega("DRONE").cupom("PROMO999")
                .pagamento("CRIPTO").parcelas(99).nivel("DIAMANTE").regiao("EUROPA");

        assertThat(recusa(tudoErrado)).isEqualTo(Erro.PEDIDO_INVALIDO);
        assertThat(recusa(tudoErrado.itens(List.of(CAMISETA)))).isEqualTo(Erro.NIVEL_CLUBE_INVALIDO);
        assertThat(recusa(tudoErrado.itens(List.of(CAMISETA)).nivel("BRONZE")))
                .isEqualTo(Erro.REGIAO_INVALIDA);
        assertThat(recusa(tudoErrado.itens(List.of(CAMISETA)).nivel("BRONZE").regiao("SUDESTE")))
                .isEqualTo(Erro.MODALIDADE_INVALIDA);
        assertThat(recusa(tudoErrado.itens(List.of(CAMISETA)).nivel("BRONZE").regiao("SUDESTE")
                .entrega("MOTOBOY"))).isEqualTo(Erro.CUPOM_INVALIDO);
        assertThat(recusa(tudoErrado.itens(List.of(CAMISETA)).nivel("BRONZE").regiao("SUDESTE")
                .entrega("MOTOBOY").cupom("MENOS50"))).isEqualTo(Erro.CUPOM_NAO_APLICAVEL);
        assertThat(recusa(tudoErrado.itens(List.of(CAMISETA)).nivel("BRONZE").regiao("SUDESTE")
                .entrega("MOTOBOY").cupom(null))).isEqualTo(Erro.FORMA_PAGAMENTO_INVALIDA);
        assertThat(recusa(tudoErrado.itens(List.of(CAMISETA)).nivel("BRONZE").regiao("SUDESTE")
                .entrega("MOTOBOY").cupom(null).pagamento("BOLETO")))
                .isEqualTo(Erro.PARCELAMENTO_INVALIDO);
    }

    private Erro recusa(Requisicao requisicao) {
        try {
            servico.calcular(requisicao.montar());
        } catch (PedidoRecusadoException excecao) {
            return excecao.erro();
        }
        throw new AssertionError("o pedido deveria ter sido recusado");
    }

    private static Requisicao valido() {
        return new Requisicao(List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", 1,
                "BRONZE", "SUDESTE");
    }

    /** Requisicao valida da qual se troca um campo por vez. */
    private record Requisicao(List<ItemRequisicao> itens, String entrega, String cupom,
                              String pagamento, Integer parcelas, String nivel, String regiao) {

        Requisicao itens(List<ItemRequisicao> novos) {
            return new Requisicao(novos, entrega, cupom, pagamento, parcelas, nivel, regiao);
        }

        Requisicao entrega(String nova) {
            return new Requisicao(itens, nova, cupom, pagamento, parcelas, nivel, regiao);
        }

        Requisicao cupom(String novo) {
            return new Requisicao(itens, entrega, novo, pagamento, parcelas, nivel, regiao);
        }

        Requisicao pagamento(String novo) {
            return new Requisicao(itens, entrega, cupom, novo, parcelas, nivel, regiao);
        }

        Requisicao parcelas(Integer novas) {
            return new Requisicao(itens, entrega, cupom, pagamento, novas, nivel, regiao);
        }

        Requisicao nivel(String novo) {
            return new Requisicao(itens, entrega, cupom, pagamento, parcelas, novo, regiao);
        }

        Requisicao regiao(String nova) {
            return new Requisicao(itens, entrega, cupom, pagamento, parcelas, nivel, nova);
        }

        ResumoRequisicao montar() {
            return new ResumoRequisicao(itens, entrega, cupom, pagamento, parcelas, nivel, regiao);
        }
    }
}
