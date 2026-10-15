package br.com.loja.checkout;

import static br.com.loja.checkout.PedidoDeTeste.CAMISETA;
import static br.com.loja.checkout.PedidoDeTeste.TENIS;
import static br.com.loja.checkout.PedidoDeTeste.item;
import static br.com.loja.checkout.PedidoDeTeste.pedido;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.loja.checkout.resumo.CalculadoraResumo;
import br.com.loja.checkout.resumo.ResumoCompra;
import java.math.BigDecimal;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CalculadoraResumoTest {

    @Autowired
    private CalculadoraResumo calculadora;

    @Nested
    @DisplayName("exemplos conferidos pelo financeiro")
    class Exemplos {

        @Test
        void exemplo1() {
            ResumoCompra resumo = calculadora.calcular(pedido(CAMISETA, TENIS)
                    .entrega("EXPRESSA").cupom("BEMVINDO10").pagamento("PIX")
                    .clube("BRONZE").regiao("NORTE").montar());

            conferir(resumo).subtotal("409.70").cupom("40.97").frete("33.10").prazo(2)
                    .seguro("10.24").ajuste("-20.60").total("391.47")
                    .parcelas(1, "391.47").credito("0.00").brinde(false);
        }

        @Test
        void exemplo2() {
            ResumoCompra resumo = calculadora.calcular(pedido(CAMISETA, TENIS)
                    .entrega("ECONOMICA").pagamento("CARTAO").parcelas(6)
                    .clube("PRATA").regiao("CENTRO_OESTE").montar());

            conferir(resumo).subtotal("409.70").cupom("0.00").frete("15.60").prazo(7)
                    .seguro("6.15").ajuste("30.55").total("462.00")
                    .parcelas(6, "77.00").credito("8.19").brinde(false);
        }

        @Test
        void exemplo3() {
            ResumoCompra resumo = calculadora.calcular(pedido(item("Fone", "199.90", 2, "0.25"))
                    .entrega("MOTOBOY").cupom("MENOS50").pagamento("BOLETO")
                    .clube("BRONZE").regiao("NORDESTE").montar());

            conferir(resumo).subtotal("399.80").cupom("50.00").frete("18.00").prazo(0)
                    .seguro("8.00").ajuste("3.49").total("379.29")
                    .parcelas(1, "379.29").credito("0.00").brinde(false);
        }

        @Test
        void exemplo4() {
            ResumoCompra resumo = calculadora.calcular(
                    pedido(item("Meia", "19.90", 7, "0.10"), CAMISETA)
                            .entrega("RETIRADA_LOJA").cupom("LEVE3PAGUE2").pagamento("CARTAO")
                            .parcelas(3).clube("PRATA").regiao("SUL").montar());

            conferir(resumo).subtotal("299.10").cupom("39.80").frete("0.00").prazo(1)
                    .seguro("2.99").ajuste("0.00").total("262.29")
                    .parcelas(3, "87.43").credito("5.98").brinde(false);
        }

        @Test
        void exemplo5() {
            ResumoCompra resumo = calculadora.calcular(pedido(CAMISETA, TENIS)
                    .entrega("EXPRESSA").pagamento("PIX")
                    .clube("OURO").regiao("SUDESTE").montar());

            conferir(resumo).subtotal("409.70").cupom("0.00").frete("0.00").prazo(2)
                    .seguro("4.10").ajuste("-20.69").total("393.11")
                    .parcelas(1, "393.11").credito("20.48").brinde(false);
        }

        @Test
        @DisplayName("ouro com cupom e pix: o exemplo do anexo")
        void exemploDoAnexo() {
            ResumoCompra resumo = calculadora.calcular(pedido(CAMISETA, TENIS)
                    .entrega("EXPRESSA").cupom("BEMVINDO10").pagamento("PIX")
                    .clube("OURO").regiao("SUDESTE").montar());

            conferir(resumo).subtotal("409.70").cupom("40.97").frete("0.00").prazo(2)
                    .seguro("4.10").ajuste("-18.64").total("354.19")
                    .parcelas(1, "354.19").credito("20.48").brinde(false);
        }
    }

    @Nested
    @DisplayName("entrega")
    class Entrega {

        @Test
        void motoboyLevaAteCincoQuilos() {
            ResumoCompra resumo = calculadora.calcular(
                    pedido(item("Jaqueta", "100.00", 5, "1.00")).entrega("MOTOBOY").montar());

            assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        }
    }

    @Nested
    @DisplayName("cupons")
    class Cupons {

        @Test
        void freteGratisDescontaExatamenteOFrete() {
            ResumoCompra resumo = calculadora.calcular(pedido(CAMISETA, TENIS)
                    .entrega("EXPRESSA").cupom("FRETEGRATIS").montar());

            assertThat(resumo.frete()).isEqualByComparingTo("33.10");
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("393.11");
        }

        @Test
        void freteGratisParaOuroNaoDescontaNada() {
            ResumoCompra resumo = calculadora.calcular(pedido(CAMISETA, TENIS)
                    .entrega("EXPRESSA").cupom("FRETEGRATIS").clube("OURO").montar());

            assertThat(resumo.frete()).isEqualByComparingTo("0.00");
            assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        }

        @Test
        void leve3pague2ContaPorItemDoCarrinho() {
            ResumoCompra resumo = calculadora.calcular(
                    pedido(item("Meia", "10.00", 6, "0.10"), item("Bone", "30.00", 4, "0.20"))
                            .entrega("RETIRADA_LOJA").cupom("LEVE3PAGUE2").montar());

            assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        }
    }

    @Nested
    @DisplayName("clube")
    class Clube {

        @Test
        void ouroGanhaBrindeAcimaDeQuinhentosEmProdutos() {
            ResumoCompra resumo = calculadora.calcular(
                    pedido(item("Casaco", "300.00", 2, "0.80"))
                            .entrega("EXPRESSA").clube("OURO").montar());

            assertThat(resumo.brinde()).isTrue();
            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("30.00");
        }

        @Test
        void ouroNoLimiteDeQuinhentosNaoGanhaBrinde() {
            ResumoCompra resumo = calculadora.calcular(
                    pedido(item("Casaco", "250.00", 2, "0.80"))
                            .entrega("EXPRESSA").clube("OURO").montar());

            assertThat(resumo.brinde()).isFalse();
        }

        @Test
        void creditoNaoAbateNestaCompra() {
            ResumoCompra resumo = calculadora.calcular(
                    pedido(item("Bone", "100.00", 1, "0.20"))
                            .entrega("RETIRADA_LOJA").pagamento("BOLETO").clube("PRATA").montar());

            assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("2.00");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("104.49");
        }
    }

    @Nested
    @DisplayName("pagamento")
    class Pagamento {

        @Test
        void cartaoSemJurosDivideOTotalDoPedido() {
            ResumoCompra resumo = calculadora.calcular(
                    pedido(item("Bone", "100.00", 1, "0.20"))
                            .entrega("RETIRADA_LOJA").pagamento("CARTAO").parcelas(3).montar());

            assertThat(resumo.totalFinal()).isEqualByComparingTo("101.00");
            assertThat(resumo.valorParcela()).isEqualByComparingTo("33.67");
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        }

        @Test
        void cartaoComJurosUsaATabelaPrice() {
            ResumoCompra resumo = calculadora.calcular(
                    pedido(item("Bone", "1000.00", 1, "0.20"))
                            .entrega("RETIRADA_LOJA").pagamento("CARTAO").parcelas(12).montar());

            assertThat(resumo.valorParcela()).isEqualByComparingTo("95.45");
            assertThat(resumo.totalFinal()).isEqualByComparingTo("1145.40");
            assertThat(resumo.ajustePagamento()).isEqualByComparingTo("135.40");
        }

        @Test
        void semParcelasInformadasEhUmaVez() {
            ResumoCompra resumo = calculadora.calcular(
                    pedido(item("Bone", "100.00", 1, "0.20"))
                            .entrega("RETIRADA_LOJA").pagamento("CARTAO").parcelas(null).montar());

            assertThat(resumo.parcelas()).isEqualTo(1);
            assertThat(resumo.valorParcela()).isEqualByComparingTo("101.00");
        }

        @Test
        void boletoNoLimiteDeMilEhAceito() {
            ResumoCompra resumo = calculadora.calcular(
                    pedido(item("Bone", "990.00", 1, "0.20"))
                            .entrega("RETIRADA_LOJA").pagamento("BOLETO").montar());

            assertThat(resumo.totalFinal()).isEqualByComparingTo("1003.39");
        }
    }

    private static Conferencia conferir(ResumoCompra resumo) {
        return new Conferencia(resumo);
    }

    /** Lê como a tabela do enunciado: cada campo do resumo na ordem. */
    private record Conferencia(ResumoCompra resumo, SoftAssertions conferencia) {

        Conferencia(ResumoCompra resumo) {
            this(resumo, new SoftAssertions());
        }

        Conferencia subtotal(String esperado) {
            return conferir(resumo.subtotalProdutos(), esperado);
        }

        Conferencia cupom(String esperado) {
            return conferir(resumo.descontoCupom(), esperado);
        }

        Conferencia frete(String esperado) {
            return conferir(resumo.frete(), esperado);
        }

        Conferencia prazo(int esperado) {
            conferencia.assertThat(resumo.prazoEntregaDias()).as("prazoEntregaDias").isEqualTo(esperado);
            return this;
        }

        Conferencia seguro(String esperado) {
            return conferir(resumo.seguro(), esperado);
        }

        Conferencia ajuste(String esperado) {
            return conferir(resumo.ajustePagamento(), esperado);
        }

        Conferencia total(String esperado) {
            return conferir(resumo.totalFinal(), esperado);
        }

        Conferencia parcelas(int quantidade, String valorParcela) {
            conferencia.assertThat(resumo.parcelas()).as("parcelas").isEqualTo(quantidade);
            return conferir(resumo.valorParcela(), valorParcela);
        }

        Conferencia credito(String esperado) {
            return conferir(resumo.creditoProximaCompra(), esperado);
        }

        void brinde(boolean esperado) {
            conferencia.assertThat(resumo.brinde()).as("brinde").isEqualTo(esperado);
            conferencia.assertAll();
        }

        private Conferencia conferir(BigDecimal valor, String esperado) {
            conferencia.assertThat(valor).isEqualByComparingTo(esperado);
            conferencia.assertThat(valor.scale()).as("casas decimais de %s", valor).isEqualTo(2);
            return this;
        }
    }
}
