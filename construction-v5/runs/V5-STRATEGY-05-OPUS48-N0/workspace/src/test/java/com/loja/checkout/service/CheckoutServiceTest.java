package com.loja.checkout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.domain.CheckoutException;
import com.loja.checkout.domain.CodigoErro;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.CheckoutRequest.Item;
import com.loja.checkout.web.CheckoutResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static Item camiseta() {
        return new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    }

    private static Item tenis() {
        return new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    }

    private static void assertMoney(BigDecimal atual, String esperado) {
        assertThat(atual.toPlainString()).isEqualTo(esperado);
    }

    @Nested
    class ExemplosDoFinanceiro {

        @Test
        void exemplo1() {
            var req = new CheckoutRequest(List.of(camiseta(), tenis()),
                    "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

            CheckoutResponse r = service.calcular(req);

            assertMoney(r.subtotalProdutos(), "409.70");
            assertMoney(r.descontoCupom(), "40.97");
            assertMoney(r.frete(), "33.10");
            assertThat(r.prazoEntregaDias()).isEqualTo(2);
            assertMoney(r.seguro(), "10.24");
            assertMoney(r.ajustePagamento(), "-20.60");
            assertMoney(r.totalFinal(), "391.47");
            assertThat(r.parcelas()).isEqualTo(1);
            assertMoney(r.valorParcela(), "391.47");
            assertMoney(r.creditoProximaCompra(), "0.00");
            assertThat(r.brinde()).isFalse();
        }

        @Test
        void exemplo2() {
            var req = new CheckoutRequest(List.of(camiseta(), tenis()),
                    "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

            CheckoutResponse r = service.calcular(req);

            assertMoney(r.subtotalProdutos(), "409.70");
            assertMoney(r.descontoCupom(), "0.00");
            assertMoney(r.frete(), "15.60");
            assertThat(r.prazoEntregaDias()).isEqualTo(7);
            assertMoney(r.seguro(), "6.15");
            assertMoney(r.ajustePagamento(), "30.55");
            assertMoney(r.totalFinal(), "462.00");
            assertThat(r.parcelas()).isEqualTo(6);
            assertMoney(r.valorParcela(), "77.00");
            assertMoney(r.creditoProximaCompra(), "8.19");
            assertThat(r.brinde()).isFalse();
        }

        @Test
        void exemplo3() {
            var fone = new Item("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
            var req = new CheckoutRequest(List.of(fone),
                    "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

            CheckoutResponse r = service.calcular(req);

            assertMoney(r.subtotalProdutos(), "399.80");
            assertMoney(r.descontoCupom(), "50.00");
            assertMoney(r.frete(), "18.00");
            assertThat(r.prazoEntregaDias()).isEqualTo(0);
            assertMoney(r.seguro(), "8.00");
            assertMoney(r.ajustePagamento(), "3.49");
            assertMoney(r.totalFinal(), "379.29");
            assertThat(r.parcelas()).isEqualTo(1);
            assertMoney(r.valorParcela(), "379.29");
            assertMoney(r.creditoProximaCompra(), "0.00");
            assertThat(r.brinde()).isFalse();
        }

        @Test
        void exemplo4() {
            var meia = new Item("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
            var req = new CheckoutRequest(List.of(meia, camiseta()),
                    "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

            CheckoutResponse r = service.calcular(req);

            assertMoney(r.subtotalProdutos(), "299.10");
            assertMoney(r.descontoCupom(), "39.80");
            assertMoney(r.frete(), "0.00");
            assertThat(r.prazoEntregaDias()).isEqualTo(1);
            assertMoney(r.seguro(), "2.99");
            assertMoney(r.ajustePagamento(), "0.00");
            assertMoney(r.totalFinal(), "262.29");
            assertThat(r.parcelas()).isEqualTo(3);
            assertMoney(r.valorParcela(), "87.43");
            assertMoney(r.creditoProximaCompra(), "5.98");
            assertThat(r.brinde()).isFalse();
        }

        @Test
        void exemplo5() {
            var req = new CheckoutRequest(List.of(camiseta(), tenis()),
                    "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

            CheckoutResponse r = service.calcular(req);

            assertMoney(r.subtotalProdutos(), "409.70");
            assertMoney(r.descontoCupom(), "0.00");
            assertMoney(r.frete(), "0.00");
            assertThat(r.prazoEntregaDias()).isEqualTo(2);
            assertMoney(r.seguro(), "4.10");
            assertMoney(r.ajustePagamento(), "-20.69");
            assertMoney(r.totalFinal(), "393.11");
            assertThat(r.parcelas()).isEqualTo(1);
            assertMoney(r.valorParcela(), "393.11");
            assertMoney(r.creditoProximaCompra(), "20.48");
            assertThat(r.brinde()).isFalse();
        }
    }

    @Nested
    class OutrasRegras {

        @Test
        void ouroComProdutosAcimaDe500GanhaBrinde() {
            var jaqueta = new Item("Jaqueta", new BigDecimal("600.00"), 1, new BigDecimal("1.00"));
            var req = new CheckoutRequest(List.of(jaqueta),
                    "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE");

            CheckoutResponse r = service.calcular(req);

            assertThat(r.brinde()).isTrue();
            assertMoney(r.frete(), "0.00");
            assertMoney(r.creditoProximaCompra(), "30.00");
        }

        @Test
        void freteGratisDescontaOValorDoFrete() {
            var req = new CheckoutRequest(List.of(camiseta(), tenis()),
                    "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE");

            CheckoutResponse r = service.calcular(req);

            assertMoney(r.frete(), "33.10");
            assertMoney(r.descontoCupom(), "33.10");
        }

        @Test
        void parcelasAusentesViram1() {
            var req = new CheckoutRequest(List.of(camiseta()),
                    "RETIRADA_LOJA", null, "CARTAO", null, "BRONZE", "SUDESTE");

            CheckoutResponse r = service.calcular(req);

            assertThat(r.parcelas()).isEqualTo(1);
        }
    }

    @Nested
    class Erros {

        private CheckoutRequest comItens(List<Item> itens) {
            return new CheckoutRequest(itens, "ECONOMICA", null, "PIX", 1, "BRONZE", "SUDESTE");
        }

        private void assertErro(CheckoutRequest req, CodigoErro esperado) {
            assertThatThrownBy(() -> service.calcular(req))
                    .isInstanceOf(CheckoutException.class)
                    .extracting("codigo")
                    .isEqualTo(esperado);
        }

        @Test
        void carrinhoVazio() {
            assertErro(comItens(List.of()), CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void itemComPrecoZero() {
            var item = new Item("X", BigDecimal.ZERO, 1, new BigDecimal("0.1"));
            assertErro(comItens(List.of(item)), CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void itemComQuantidadeNegativa() {
            var item = new Item("X", new BigDecimal("10.00"), -1, new BigDecimal("0.1"));
            assertErro(comItens(List.of(item)), CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void itemComPesoAusente() {
            var item = new Item("X", new BigDecimal("10.00"), 1, null);
            assertErro(comItens(List.of(item)), CodigoErro.PEDIDO_INVALIDO);
        }

        @Test
        void nivelClubeInvalido() {
            var req = new CheckoutRequest(List.of(camiseta()),
                    "ECONOMICA", null, "PIX", 1, "DIAMANTE", "SUDESTE");
            assertErro(req, CodigoErro.NIVEL_CLUBE_INVALIDO);
        }

        @Test
        void regiaoInvalida() {
            var req = new CheckoutRequest(List.of(camiseta()),
                    "ECONOMICA", null, "PIX", 1, "BRONZE", "LESTE");
            assertErro(req, CodigoErro.REGIAO_INVALIDA);
        }

        @Test
        void modalidadeInvalida() {
            var req = new CheckoutRequest(List.of(camiseta()),
                    "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE");
            assertErro(req, CodigoErro.MODALIDADE_INVALIDA);
        }

        @Test
        void motoboyAcimaDe5kg() {
            var pesado = new Item("Halteres", new BigDecimal("100.00"), 1, new BigDecimal("6.00"));
            var req = new CheckoutRequest(List.of(pesado),
                    "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");
            assertErro(req, CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        @Test
        void cupomInexistente() {
            var req = new CheckoutRequest(List.of(camiseta()),
                    "ECONOMICA", "PROMO999", "PIX", 1, "BRONZE", "SUDESTE");
            assertErro(req, CodigoErro.CUPOM_INVALIDO);
        }

        @Test
        void menos50AbaixoDe300() {
            var req = new CheckoutRequest(List.of(camiseta()),
                    "ECONOMICA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");
            assertErro(req, CodigoErro.CUPOM_NAO_APLICAVEL);
        }

        @Test
        void formaPagamentoInvalida() {
            var req = new CheckoutRequest(List.of(camiseta()),
                    "ECONOMICA", null, "CRIPTO", 1, "BRONZE", "SUDESTE");
            assertErro(req, CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }

        @Test
        void pixParceladoNaoPermitido() {
            var req = new CheckoutRequest(List.of(camiseta()),
                    "ECONOMICA", null, "PIX", 2, "BRONZE", "SUDESTE");
            assertErro(req, CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void cartaoAcimaDe12xNaoPermitido() {
            var req = new CheckoutRequest(List.of(camiseta()),
                    "ECONOMICA", null, "CARTAO", 13, "BRONZE", "SUDESTE");
            assertErro(req, CodigoErro.PARCELAMENTO_INVALIDO);
        }

        @Test
        void boletoAcimaDe1000() {
            var caro = new Item("Notebook", new BigDecimal("1200.00"), 1, new BigDecimal("2.00"));
            var req = new CheckoutRequest(List.of(caro),
                    "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE");
            assertErro(req, CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        @Test
        void primeiroProblemaNaOrdemGanha() {
            // nível e região inválidos ao mesmo tempo: devolve o nível (verificado antes).
            var req = new CheckoutRequest(List.of(camiseta()),
                    "ECONOMICA", null, "PIX", 1, "DIAMANTE", "LESTE");
            assertErro(req, CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
    }
}
