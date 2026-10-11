package br.com.loja.checkout.aplicacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.loja.checkout.api.ResumoCompraRequest;
import br.com.loja.checkout.api.ResumoCompraRequest.ItemRequest;
import br.com.loja.checkout.api.ResumoCompraResponse;
import br.com.loja.checkout.cupom.BemVindo10;
import br.com.loja.checkout.cupom.FreteGratis;
import br.com.loja.checkout.cupom.Leve3Pague2;
import br.com.loja.checkout.cupom.Menos50;
import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.ErroPedidoException;
import br.com.loja.checkout.entrega.EntregaEconomica;
import br.com.loja.checkout.entrega.EntregaExpressa;
import br.com.loja.checkout.entrega.Motoboy;
import br.com.loja.checkout.entrega.RetiradaLoja;

class ResumoCompraServiceTest {

    private static final ItemRequest CAMISETA = new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    private static final ItemRequest TENIS = new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));

    private ResumoCompraService servico;

    @BeforeEach
    void criarServico() {
        servico = new ResumoCompraService(
                List.of(new EntregaEconomica(), new EntregaExpressa(), new RetiradaLoja(), new Motoboy()),
                List.of(new BemVindo10(), new Menos50(), new FreteGratis(), new Leve3Pague2()));
    }

    private static ResumoCompraRequest pedido(List<ItemRequest> itens, String entrega, String cupom,
                                              String forma, Integer parcelas, String nivel, String regiao) {
        return new ResumoCompraRequest(itens, entrega, cupom, forma, parcelas, nivel, regiao);
    }

    private ResumoCompraResponse calcular(ResumoCompraRequest pedido) {
        return servico.calcular(pedido);
    }

    private void assertErro(ResumoCompraRequest pedido, CodigoErro esperado) {
        assertThatThrownBy(() -> calcular(pedido))
                .isInstanceOfSatisfying(ErroPedidoException.class, e -> assertThat(e.getCodigo()).isEqualTo(esperado));
    }

    @Test
    void exemploPixComCupomBemVindo() {
        var r = calcular(pedido(List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"));

        assertThat(r.subtotalProdutos()).isEqualTo(new BigDecimal("409.70"));
        assertThat(r.descontoCupom()).isEqualTo(new BigDecimal("40.97"));
        assertThat(r.frete()).isEqualTo(new BigDecimal("33.10"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualTo(new BigDecimal("10.24"));
        assertThat(r.ajustePagamento()).isEqualTo(new BigDecimal("-20.60"));
        assertThat(r.totalFinal()).isEqualTo(new BigDecimal("391.47"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualTo(new BigDecimal("391.47"));
        assertThat(r.creditoProximaCompra()).isEqualTo(new BigDecimal("0.00"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemploCartaoSeisVezesComJurosPrata() {
        var r = calcular(pedido(List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(r.subtotalProdutos()).isEqualTo(new BigDecimal("409.70"));
        assertThat(r.descontoCupom()).isEqualTo(new BigDecimal("0.00"));
        assertThat(r.frete()).isEqualTo(new BigDecimal("15.60"));
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertThat(r.seguro()).isEqualTo(new BigDecimal("6.15"));
        assertThat(r.ajustePagamento()).isEqualTo(new BigDecimal("30.55"));
        assertThat(r.totalFinal()).isEqualTo(new BigDecimal("462.00"));
        assertThat(r.parcelas()).isEqualTo(6);
        assertThat(r.valorParcela()).isEqualTo(new BigDecimal("77.00"));
        assertThat(r.creditoProximaCompra()).isEqualTo(new BigDecimal("8.19"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemploBoletoComMotoboyERegraMenos50() {
        var fone = new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
        var r = calcular(pedido(List.of(fone), "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        assertThat(r.subtotalProdutos()).isEqualTo(new BigDecimal("399.80"));
        assertThat(r.descontoCupom()).isEqualTo(new BigDecimal("50.00"));
        assertThat(r.frete()).isEqualTo(new BigDecimal("18.00"));
        assertThat(r.prazoEntregaDias()).isZero();
        assertThat(r.seguro()).isEqualTo(new BigDecimal("8.00"));
        assertThat(r.ajustePagamento()).isEqualTo(new BigDecimal("3.49"));
        assertThat(r.totalFinal()).isEqualTo(new BigDecimal("379.29"));
        assertThat(r.valorParcela()).isEqualTo(new BigDecimal("379.29"));
    }

    @Test
    void exemploCartaoTresVezesSemJurosComLeve3Pague2() {
        var meia = new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
        var r = calcular(pedido(List.of(meia, CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(r.subtotalProdutos()).isEqualTo(new BigDecimal("299.10"));
        assertThat(r.descontoCupom()).isEqualTo(new BigDecimal("39.80"));
        assertThat(r.frete()).isEqualTo(new BigDecimal("0.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertThat(r.seguro()).isEqualTo(new BigDecimal("2.99"));
        assertThat(r.ajustePagamento()).isEqualTo(new BigDecimal("0.00"));
        assertThat(r.totalFinal()).isEqualTo(new BigDecimal("262.29"));
        assertThat(r.parcelas()).isEqualTo(3);
        assertThat(r.valorParcela()).isEqualTo(new BigDecimal("87.43"));
        assertThat(r.creditoProximaCompra()).isEqualTo(new BigDecimal("5.98"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemploPixOuroSemFreteComCreditoArredondadoParaPar() {
        var r = calcular(pedido(List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"));

        assertThat(r.subtotalProdutos()).isEqualTo(new BigDecimal("409.70"));
        assertThat(r.frete()).isEqualTo(new BigDecimal("0.00"));
        assertThat(r.seguro()).isEqualTo(new BigDecimal("4.10"));
        assertThat(r.ajustePagamento()).isEqualTo(new BigDecimal("-20.69"));
        assertThat(r.totalFinal()).isEqualTo(new BigDecimal("393.11"));
        assertThat(r.creditoProximaCompra()).isEqualTo(new BigDecimal("20.48"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void ouroGanhaBrindeSoAcimaDeQuinhentos() {
        var acima = new ItemRequest("Jaqueta", new BigDecimal("500.01"), 1, new BigDecimal("1"));
        var igual = new ItemRequest("Jaqueta", new BigDecimal("500.00"), 1, new BigDecimal("1"));

        assertThat(calcular(pedido(List.of(acima), "ECONOMICA", null, "PIX", null, "OURO", "SUL")).brinde()).isTrue();
        assertThat(calcular(pedido(List.of(igual), "ECONOMICA", null, "PIX", null, "OURO", "SUL")).brinde()).isFalse();
    }

    @Test
    void freteGratisDeixaDescontoIgualAoFrete() {
        var r = calcular(pedido(List.of(CAMISETA), "ECONOMICA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUL"));

        assertThat(r.frete()).isEqualTo(new BigDecimal("13.20"));
        assertThat(r.descontoCupom()).isEqualTo(new BigDecimal("13.20"));
    }

    @Test
    void motoboyAceitaAteCincoQuilos() {
        var limite = new ItemRequest("Casaco", new BigDecimal("100.00"), 1, new BigDecimal("5.00"));
        var acima = new ItemRequest("Casaco", new BigDecimal("100.00"), 1, new BigDecimal("5.01"));

        assertThat(calcular(pedido(List.of(limite), "MOTOBOY", null, "PIX", null, "BRONZE", "SUL")).frete())
                .isEqualTo(new BigDecimal("18.00"));
        assertErro(pedido(List.of(acima), "MOTOBOY", null, "PIX", null, "BRONZE", "SUL"), CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void boletoAcimaDeMilReaisNaoEAceito() {
        var caro = new ItemRequest("Bolsa", new BigDecimal("1200.00"), 1, new BigDecimal("1"));

        assertErro(pedido(List.of(caro), "ECONOMICA", null, "BOLETO", null, "BRONZE", "SUL"), CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void menos50AbaixoDeTrezentosNaoEAplicavel() {
        var abaixo = new ItemRequest("Cinto", new BigDecimal("299.99"), 1, new BigDecimal("0.2"));

        assertErro(pedido(List.of(abaixo), "ECONOMICA", "MENOS50", "PIX", null, "BRONZE", "SUL"), CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void cupomDesconhecidoOuMinusculoEhInvalido() {
        assertErro(pedido(List.of(CAMISETA), "ECONOMICA", "bemvindo10", "PIX", null, "BRONZE", "SUL"), CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void parcelamentoForaDaRegra() {
        assertErro(pedido(List.of(CAMISETA), "ECONOMICA", null, "PIX", 2, "BRONZE", "SUL"), CodigoErro.PARCELAMENTO_INVALIDO);
        assertErro(pedido(List.of(CAMISETA), "ECONOMICA", null, "BOLETO", 2, "BRONZE", "SUL"), CodigoErro.PARCELAMENTO_INVALIDO);
        assertErro(pedido(List.of(CAMISETA), "ECONOMICA", null, "CARTAO", 13, "BRONZE", "SUL"), CodigoErro.PARCELAMENTO_INVALIDO);
        assertErro(pedido(List.of(CAMISETA), "ECONOMICA", null, "CARTAO", 0, "BRONZE", "SUL"), CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoDozeVezesCalculaParcelaComJuros() {
        var r = calcular(pedido(List.of(TENIS), "ECONOMICA", null, "CARTAO", 12, "BRONZE", "SUL"));

        assertThat(r.parcelas()).isEqualTo(12);
        assertThat(r.totalFinal()).isEqualTo(r.valorParcela().multiply(BigDecimal.valueOf(12)).setScale(2));
        assertThat(r.ajustePagamento()).isEqualTo(r.totalFinal().subtract(new BigDecimal("266.80")));
    }

    @Test
    void ordemDeErrosRespeitaAPrioridade() {
        var itemInvalido = new ItemRequest("X", new BigDecimal("0"), 1, new BigDecimal("1"));

        assertErro(pedido(List.of(itemInvalido), "NAO_EXISTE", "NAO_EXISTE", "NAO_EXISTE", null, "NAO_EXISTE", "NAO_EXISTE"),
                CodigoErro.PEDIDO_INVALIDO);
        assertErro(pedido(List.of(CAMISETA), "NAO_EXISTE", "NAO_EXISTE", "NAO_EXISTE", null, "NAO_EXISTE", "NAO_EXISTE"),
                CodigoErro.NIVEL_CLUBE_INVALIDO);
        assertErro(pedido(List.of(CAMISETA), "ECONOMICA", "NAO_EXISTE", "NAO_EXISTE", null, "BRONZE", "NAO_EXISTE"),
                CodigoErro.REGIAO_INVALIDA);
        assertErro(pedido(List.of(CAMISETA), "NAO_EXISTE", "NAO_EXISTE", "NAO_EXISTE", null, "BRONZE", "SUL"),
                CodigoErro.MODALIDADE_INVALIDA);
        assertErro(pedido(List.of(CAMISETA), "ECONOMICA", "NAO_EXISTE", "NAO_EXISTE", null, "BRONZE", "SUL"),
                CodigoErro.CUPOM_INVALIDO);
        assertErro(pedido(List.of(CAMISETA), "ECONOMICA", null, "NAO_EXISTE", null, "BRONZE", "SUL"),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void carrinhoVazioOuItemSemPrecoEhPedidoInvalido() {
        assertErro(pedido(List.of(), "ECONOMICA", null, "PIX", null, "BRONZE", "SUL"), CodigoErro.PEDIDO_INVALIDO);
        assertErro(pedido(null, "ECONOMICA", null, "PIX", null, "BRONZE", "SUL"), CodigoErro.PEDIDO_INVALIDO);
        var semPeso = new ItemRequest("X", new BigDecimal("10"), 1, null);
        assertErro(pedido(List.of(semPeso), "ECONOMICA", null, "PIX", null, "BRONZE", "SUL"), CodigoErro.PEDIDO_INVALIDO);
    }
}
