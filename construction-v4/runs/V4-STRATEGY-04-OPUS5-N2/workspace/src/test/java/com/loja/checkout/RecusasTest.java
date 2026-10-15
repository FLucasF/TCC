package com.loja.checkout;

import static com.loja.checkout.Pedidos.CAMISETA;
import static com.loja.checkout.Pedidos.TENIS;
import static com.loja.checkout.Pedidos.item;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.ErroPedido;
import com.loja.checkout.dominio.ItemRecebido;
import com.loja.checkout.dominio.PedidoRecebido;
import com.loja.checkout.dominio.PedidoRecusadoException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RecusasTest {

    @Autowired
    private CalculadoraResumo calculadora;

    @Nested
    @DisplayName("1. PEDIDO_INVALIDO")
    class PedidoInvalido {

        @Test
        @DisplayName("carrinho vazio ou ausente")
        void carrinhoVazio() {
            recusa(pedido(List.of()), ErroPedido.PEDIDO_INVALIDO);
            recusa(pedido(null), ErroPedido.PEDIDO_INVALIDO);
            recusa(pedido(comNulo()), ErroPedido.PEDIDO_INVALIDO);
        }

        @ParameterizedTest(name = "preco {0}")
        @ValueSource(strings = {"0.00", "-1.00"})
        @DisplayName("item com preco zero ou negativo")
        void precoInvalido(String preco) {
            recusa(pedido(List.of(item("Camiseta", preco, 2, "0.30"))), ErroPedido.PEDIDO_INVALIDO);
        }

        @ParameterizedTest(name = "quantidade {0}")
        @ValueSource(ints = {0, -2})
        @DisplayName("item com quantidade zero ou negativa")
        void quantidadeInvalida(int quantidade) {
            recusa(pedido(List.of(item("Camiseta", "79.90", quantidade, "0.30"))),
                    ErroPedido.PEDIDO_INVALIDO);
        }

        @ParameterizedTest(name = "peso {0}")
        @ValueSource(strings = {"0.00", "-0.30"})
        @DisplayName("item com peso zero ou negativo")
        void pesoInvalido(String peso) {
            recusa(pedido(List.of(item("Camiseta", "79.90", 2, peso))), ErroPedido.PEDIDO_INVALIDO);
        }

        @Test
        @DisplayName("item com campo ausente")
        void campoAusente() {
            BigDecimal valor = new BigDecimal("1.00");
            recusa(pedido(List.of(new ItemRecebido("Camiseta", null, 2, valor))),
                    ErroPedido.PEDIDO_INVALIDO);
            recusa(pedido(List.of(new ItemRecebido("Camiseta", valor, null, valor))),
                    ErroPedido.PEDIDO_INVALIDO);
            recusa(pedido(List.of(new ItemRecebido("Camiseta", valor, 2, null))),
                    ErroPedido.PEDIDO_INVALIDO);
        }

        private PedidoRecebido pedido(List<ItemRecebido> itens) {
            return new PedidoRecebido(itens, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");
        }

        private List<ItemRecebido> comNulo() {
            return new ArrayList<>(Arrays.asList(CAMISETA, null));
        }
    }

    @Nested
    @DisplayName("2 a 4. codigos que nao existem")
    class CodigosDesconhecidos {

        @ParameterizedTest
        @ValueSource(strings = {"DIAMANTE", "bronze", ""})
        @DisplayName("nivel do clube que nao existe")
        void nivelClube(String nivel) {
            recusa(new PedidoRecebido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, nivel, "SUDESTE"),
                    ErroPedido.NIVEL_CLUBE_INVALIDO);
        }

        @Test
        @DisplayName("nivel do clube nao informado")
        void nivelClubeAusente() {
            recusa(new PedidoRecebido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, null, "SUDESTE"),
                    ErroPedido.NIVEL_CLUBE_INVALIDO);
        }

        @ParameterizedTest
        @ValueSource(strings = {"CENTRO", "sudeste"})
        @DisplayName("regiao que nao existe")
        void regiao(String regiao) {
            recusa(new PedidoRecebido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "BRONZE", regiao),
                    ErroPedido.REGIAO_INVALIDA);
        }

        @Test
        @DisplayName("regiao nao informada")
        void regiaoAusente() {
            recusa(new PedidoRecebido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "BRONZE", null),
                    ErroPedido.REGIAO_INVALIDA);
        }

        @ParameterizedTest
        @ValueSource(strings = {"DRONE", "expressa"})
        @DisplayName("modalidade de entrega que nao existe")
        void modalidade(String modalidade) {
            recusa(new PedidoRecebido(List.of(CAMISETA), modalidade, null, "PIX", 1, "BRONZE", "SUDESTE"),
                    ErroPedido.MODALIDADE_INVALIDA);
        }

        @Test
        @DisplayName("modalidade de entrega nao informada")
        void modalidadeAusente() {
            recusa(new PedidoRecebido(List.of(CAMISETA), null, null, "PIX", 1, "BRONZE", "SUDESTE"),
                    ErroPedido.MODALIDADE_INVALIDA);
        }

        @ParameterizedTest
        @ValueSource(strings = {"DINHEIRO", "pix"})
        @DisplayName("forma de pagamento que nao existe")
        void formaPagamento(String forma) {
            recusa(new PedidoRecebido(List.of(CAMISETA), "EXPRESSA", null, forma, 1, "BRONZE", "SUDESTE"),
                    ErroPedido.FORMA_PAGAMENTO_INVALIDA);
        }

        @Test
        @DisplayName("forma de pagamento nao informada")
        void formaPagamentoAusente() {
            recusa(new PedidoRecebido(List.of(CAMISETA), "EXPRESSA", null, null, 1, "BRONZE", "SUDESTE"),
                    ErroPedido.FORMA_PAGAMENTO_INVALIDA);
        }

        @ParameterizedTest
        @ValueSource(strings = {"BEMVINDO5", "bemvindo10", ""})
        @DisplayName("cupom que nao existe")
        void cupom(String cupom) {
            recusa(new PedidoRecebido(List.of(CAMISETA), "EXPRESSA", cupom, "PIX", 1, "BRONZE", "SUDESTE"),
                    ErroPedido.CUPOM_INVALIDO);
        }
    }

    @Nested
    @DisplayName("5, 7 e 10. a opcao existe mas nao atende o pedido")
    class NaoAtende {

        @Test
        @DisplayName("motoboy acima de 5 kg")
        void motoboyPesado() {
            recusa(new PedidoRecebido(List.of(item("Jaqueta", "100.00", 6, "1.00")), "MOTOBOY",
                    null, "PIX", 1, "BRONZE", "SUDESTE"), ErroPedido.MODALIDADE_INDISPONIVEL);
        }

        @Test
        @DisplayName("MENOS50 abaixo de R$ 300,00 em produtos")
        void menos50Abaixo() {
            recusa(new PedidoRecebido(List.of(item("Mochila", "299.99", 1, "0.50")), "RETIRADA_LOJA",
                    "MENOS50", "PIX", 1, "BRONZE", "SUDESTE"), ErroPedido.CUPOM_NAO_APLICAVEL);
        }

        @Test
        @DisplayName("boleto acima de R$ 1.000,00 no total do pedido")
        void boletoAcimaDoLimite() {
            recusa(new PedidoRecebido(List.of(item("Bolsa", "990.20", 1, "0.50")), "RETIRADA_LOJA",
                    null, "BOLETO", 1, "BRONZE", "SUDESTE"), ErroPedido.FORMA_PAGAMENTO_INDISPONIVEL);
        }
    }

    @Nested
    @DisplayName("9. PARCELAMENTO_INVALIDO")
    class Parcelamento {

        @ParameterizedTest(name = "PIX em {0}x")
        @ValueSource(ints = {0, 2, 3})
        @DisplayName("pix e sempre a vista")
        void pix(int parcelas) {
            recusa(parcelando("PIX", parcelas), ErroPedido.PARCELAMENTO_INVALIDO);
        }

        @ParameterizedTest(name = "BOLETO em {0}x")
        @ValueSource(ints = {0, 2, 12})
        @DisplayName("boleto e sempre a vista")
        void boleto(int parcelas) {
            recusa(parcelando("BOLETO", parcelas), ErroPedido.PARCELAMENTO_INVALIDO);
        }

        @ParameterizedTest(name = "CARTAO em {0}x")
        @ValueSource(ints = {-1, 0, 13, 24})
        @DisplayName("cartao vai de 1 a 12")
        void cartao(int parcelas) {
            recusa(parcelando("CARTAO", parcelas), ErroPedido.PARCELAMENTO_INVALIDO);
        }

        private PedidoRecebido parcelando(String forma, int parcelas) {
            return new PedidoRecebido(List.of(CAMISETA), "EXPRESSA", null, forma, parcelas,
                    "BRONZE", "SUDESTE");
        }
    }

    @Nested
    @DisplayName("a ordem de conferencia: vale o primeiro problema encontrado")
    class Ordem {

        @Test
        @DisplayName("pedido invalido vem antes de tudo")
        void primeiroOPedido() {
            recusa(new PedidoRecebido(List.of(), "DRONE", "BEMVINDO5", "DINHEIRO", 9, "DIAMANTE",
                    "LESTE"), ErroPedido.PEDIDO_INVALIDO);
        }

        @Test
        @DisplayName("nivel do clube vem antes da regiao")
        void nivelAntesDaRegiao() {
            recusa(new PedidoRecebido(List.of(CAMISETA), "DRONE", null, "PIX", 1, "DIAMANTE",
                    "LESTE"), ErroPedido.NIVEL_CLUBE_INVALIDO);
        }

        @Test
        @DisplayName("regiao vem antes da modalidade")
        void regiaoAntesDaModalidade() {
            recusa(new PedidoRecebido(List.of(CAMISETA), "DRONE", null, "PIX", 1, "BRONZE",
                    "LESTE"), ErroPedido.REGIAO_INVALIDA);
        }

        @Test
        @DisplayName("modalidade que nao existe vem antes do cupom")
        void modalidadeAntesDoCupom() {
            recusa(new PedidoRecebido(List.of(CAMISETA), "DRONE", "BEMVINDO5", "PIX", 1, "BRONZE",
                    "SUDESTE"), ErroPedido.MODALIDADE_INVALIDA);
        }

        @Test
        @DisplayName("modalidade que nao atende vem antes do cupom")
        void modalidadeIndisponivelAntesDoCupom() {
            recusa(new PedidoRecebido(List.of(item("Jaqueta", "100.00", 6, "1.00")), "MOTOBOY",
                    "BEMVINDO5", "PIX", 1, "BRONZE", "SUDESTE"), ErroPedido.MODALIDADE_INDISPONIVEL);
        }

        @Test
        @DisplayName("cupom que nao existe vem antes da forma de pagamento")
        void cupomAntesDoPagamento() {
            recusa(new PedidoRecebido(List.of(CAMISETA), "EXPRESSA", "BEMVINDO5", "DINHEIRO", 9,
                    "BRONZE", "SUDESTE"), ErroPedido.CUPOM_INVALIDO);
        }

        @Test
        @DisplayName("cupom que nao se aplica vem antes da forma de pagamento")
        void cupomNaoAplicavelAntesDoPagamento() {
            recusa(new PedidoRecebido(List.of(CAMISETA), "EXPRESSA", "MENOS50", "DINHEIRO", 9,
                    "BRONZE", "SUDESTE"), ErroPedido.CUPOM_NAO_APLICAVEL);
        }

        @Test
        @DisplayName("forma de pagamento que nao existe vem antes do parcelamento")
        void pagamentoAntesDoParcelamento() {
            recusa(new PedidoRecebido(List.of(CAMISETA), "EXPRESSA", null, "DINHEIRO", 99,
                    "BRONZE", "SUDESTE"), ErroPedido.FORMA_PAGAMENTO_INVALIDA);
        }

        @Test
        @DisplayName("parcelamento vem antes de a forma de pagamento nao atender")
        void parcelamentoAntesDeIndisponivel() {
            recusa(new PedidoRecebido(List.of(item("Bolsa", "2000.00", 1, "0.50")), "RETIRADA_LOJA",
                    null, "BOLETO", 2, "BRONZE", "SUDESTE"), ErroPedido.PARCELAMENTO_INVALIDO);
        }
    }

    private void recusa(PedidoRecebido pedido, ErroPedido esperado) {
        assertThatThrownBy(() -> calculadora.calcular(pedido))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(excecao -> ((PedidoRecusadoException) excecao).erro())
                .isEqualTo(esperado);
    }

    @Test
    @DisplayName("o pedido do atendimento mais comum passa sem recusa")
    void pedidoValidoPassa() {
        assertThat(calculadora.calcular(new PedidoRecebido(List.of(CAMISETA, TENIS), "EXPRESSA",
                "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"))).isNotNull();
    }
}
