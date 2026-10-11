package com.loja.checkout.regra;

import com.loja.checkout.regra.PedidoCompra.Item;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraResumoTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    private static final List<Item> CAMISETA_E_TENIS = List.of(
            new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

    @Test
    void exemplo1PixComBemVindoEBronzeNoNorte() {
        var pedido = new PedidoCompra(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        var r = calculadora.calcular(pedido);

        assertEquals(new BigDecimal("409.70"), r.subtotalProdutos());
        assertEquals(new BigDecimal("40.97"), r.descontoCupom());
        assertEquals(new BigDecimal("33.10"), r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), r.seguro());
        assertEquals(new BigDecimal("-20.60"), r.ajustePagamento());
        assertEquals(new BigDecimal("391.47"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(new BigDecimal("391.47"), r.valorParcela());
        assertEquals(new BigDecimal("0.00"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo2CartaoSeisVezesComJurosEClubePrata() {
        var pedido = new PedidoCompra(CAMISETA_E_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        var r = calculadora.calcular(pedido);

        assertEquals(new BigDecimal("409.70"), r.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), r.descontoCupom());
        assertEquals(new BigDecimal("15.60"), r.frete());
        assertEquals(7, r.prazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), r.seguro());
        assertEquals(new BigDecimal("30.55"), r.ajustePagamento());
        assertEquals(new BigDecimal("462.00"), r.totalFinal());
        assertEquals(6, r.parcelas());
        assertEquals(new BigDecimal("77.00"), r.valorParcela());
        assertEquals(new BigDecimal("8.19"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo3MotoboyComMenos50EBoleto() {
        var itens = List.of(new Item("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));
        var pedido = new PedidoCompra(itens, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        var r = calculadora.calcular(pedido);

        assertEquals(new BigDecimal("399.80"), r.subtotalProdutos());
        assertEquals(new BigDecimal("50.00"), r.descontoCupom());
        assertEquals(new BigDecimal("18.00"), r.frete());
        assertEquals(0, r.prazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), r.seguro());
        assertEquals(new BigDecimal("3.49"), r.ajustePagamento());
        assertEquals(new BigDecimal("379.29"), r.totalFinal());
        assertEquals(1, r.parcelas());
        assertEquals(new BigDecimal("379.29"), r.valorParcela());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo4RetiradaComLeve3Pague2EmTresVezes() {
        var itens = List.of(
                new Item("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        var pedido = new PedidoCompra(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        var r = calculadora.calcular(pedido);

        assertEquals(new BigDecimal("299.10"), r.subtotalProdutos());
        assertEquals(new BigDecimal("39.80"), r.descontoCupom());
        assertEquals(new BigDecimal("0.00"), r.frete());
        assertEquals(1, r.prazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), r.seguro());
        assertEquals(new BigDecimal("0.00"), r.ajustePagamento());
        assertEquals(new BigDecimal("262.29"), r.totalFinal());
        assertEquals(3, r.parcelas());
        assertEquals(new BigDecimal("87.43"), r.valorParcela());
        assertEquals(new BigDecimal("5.98"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void exemplo5OuroSemFreteComPixEBrindeNaoAtingido() {
        var pedido = new PedidoCompra(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        var r = calculadora.calcular(pedido);

        assertEquals(new BigDecimal("409.70"), r.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), r.descontoCupom());
        assertEquals(new BigDecimal("0.00"), r.frete());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), r.seguro());
        assertEquals(new BigDecimal("-20.69"), r.ajustePagamento());
        assertEquals(new BigDecimal("393.11"), r.totalFinal());
        assertEquals(new BigDecimal("20.48"), r.creditoProximaCompra());
        assertEquals(false, r.brinde());
    }

    @Test
    void ouroComProdutosAcimaDe500GanhaBrinde() {
        var itens = List.of(new Item("Casaco", new BigDecimal("250.00"), 2, new BigDecimal("1.00")));
        var pedido = new PedidoCompra(itens, "ECONOMICA", null, "PIX", null, "OURO", "SUL");

        assertEquals(false, calculadora.calcular(pedido).brinde());

        var itensAcima = List.of(new Item("Casaco", new BigDecimal("250.01"), 2, new BigDecimal("1.00")));
        var pedidoAcima = new PedidoCompra(itensAcima, "ECONOMICA", null, "PIX", null, "OURO", "SUL");

        assertEquals(true, calculadora.calcular(pedidoAcima).brinde());
    }

    @Test
    void motoboyAcimaDe5kgNaoEstaDisponivel() {
        var itens = List.of(new Item("Bota", new BigDecimal("300.00"), 1, new BigDecimal("5.01")));
        var pedido = new PedidoCompra(itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUL");

        assertErro(pedido, CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void motoboyComExatos5kgEstaDisponivel() {
        var itens = List.of(new Item("Bota", new BigDecimal("300.00"), 1, new BigDecimal("5.00")));
        var pedido = new PedidoCompra(itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUL");

        assertEquals(new BigDecimal("18.00"), calculadora.calcular(pedido).frete());
    }

    @Test
    void menos50AbaixoDe300NaoEAplicavel() {
        var itens = List.of(new Item("Meia", new BigDecimal("19.90"), 1, new BigDecimal("0.10")));
        var pedido = new PedidoCompra(itens, "RETIRADA_LOJA", "MENOS50", "PIX", null, "BRONZE", "SUL");

        assertErro(pedido, CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void leve3Pague2SemItemComTresUnidadesNaoEAplicavel() {
        var itens = List.of(new Item("Meia", new BigDecimal("19.90"), 2, new BigDecimal("0.10")));
        var pedido = new PedidoCompra(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "PIX", null, "BRONZE", "SUL");

        assertErro(pedido, CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void freteGratisDescontaOValorDoFrete() {
        var pedido = new PedidoCompra(CAMISETA_E_TENIS, "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE");

        var r = calculadora.calcular(pedido);

        assertEquals(r.frete(), r.descontoCupom());
        assertEquals(new BigDecimal("33.10"), r.descontoCupom());
    }

    @Test
    void boletoAcimaDe1000NaoEAceito() {
        var itens = List.of(new Item("Casaco", new BigDecimal("500.00"), 2, new BigDecimal("1.00")));
        var pedido = new PedidoCompra(itens, "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUL");

        assertErro(pedido, CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void cartaoComTrezeParcelasEhParcelamentoInvalido() {
        var pedido = new PedidoCompra(CAMISETA_E_TENIS, "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUL");

        assertErro(pedido, CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void pixEmDuasVezesEhParcelamentoInvalido() {
        var pedido = new PedidoCompra(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUL");

        assertErro(pedido, CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void carrinhoVazioEhPedidoInvalido() {
        var pedido = new PedidoCompra(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUL");

        assertErro(pedido, CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComPesoZeroEhPedidoInvalido() {
        var itens = List.of(new Item("Meia", new BigDecimal("19.90"), 1, BigDecimal.ZERO));
        var pedido = new PedidoCompra(itens, "EXPRESSA", null, "PIX", null, "BRONZE", "SUL");

        assertErro(pedido, CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void nivelInvalidoTemPrioridadeSobreRegiaoInvalida() {
        var pedido = new PedidoCompra(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", null, "DIAMANTE", "MARTE");

        assertErro(pedido, CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void modalidadeAusenteEhModalidadeInvalida() {
        var pedido = new PedidoCompra(CAMISETA_E_TENIS, null, null, "PIX", null, "BRONZE", "SUL");

        assertErro(pedido, CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void cupomEmMinusculoEhCupomInvalido() {
        var pedido = new PedidoCompra(CAMISETA_E_TENIS, "EXPRESSA", "bemvindo10", "PIX", null, "BRONZE", "SUL");

        assertErro(pedido, CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void formaDePagamentoAusenteEhFormaInvalida() {
        var pedido = new PedidoCompra(CAMISETA_E_TENIS, "EXPRESSA", null, null, null, "BRONZE", "SUL");

        assertErro(pedido, CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    private void assertErro(PedidoCompra pedido, CodigoErro codigo) {
        assertThatThrownBy(() -> calculadora.calcular(pedido))
                .isInstanceOfSatisfying(ErroCheckout.class, e -> assertEquals(codigo, e.getCodigo()));
    }
}
