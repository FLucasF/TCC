package com.loja.checkout;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoPedidoRequest;
import com.loja.checkout.api.ResumoPedidoResponse;
import com.loja.checkout.clube.CatalogoNiveisClube;
import com.loja.checkout.clube.NivelBronze;
import com.loja.checkout.clube.NivelOuro;
import com.loja.checkout.clube.NivelPrata;
import com.loja.checkout.cupom.CatalogoCupons;
import com.loja.checkout.cupom.CupomBemVindo10;
import com.loja.checkout.cupom.CupomFreteGratis;
import com.loja.checkout.cupom.CupomLeve3Pague2;
import com.loja.checkout.cupom.CupomMenos50;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.entrega.CatalogoEntregas;
import com.loja.checkout.entrega.EntregaEconomica;
import com.loja.checkout.entrega.EntregaExpressa;
import com.loja.checkout.entrega.EntregaMotoboy;
import com.loja.checkout.entrega.RetiradaLoja;
import com.loja.checkout.pagamento.CatalogoFormasPagamento;
import com.loja.checkout.pagamento.PagamentoBoleto;
import com.loja.checkout.pagamento.PagamentoCartao;
import com.loja.checkout.pagamento.PagamentoPix;
import com.loja.checkout.seguro.CalculadoraSeguro;
import com.loja.checkout.servico.CalculadoraResumoPedido;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.loja.checkout.ResumoPedidoFixtures.CAMISETA;
import static com.loja.checkout.ResumoPedidoFixtures.FONE;
import static com.loja.checkout.ResumoPedidoFixtures.MEIA;
import static com.loja.checkout.ResumoPedidoFixtures.carrinhoPadrao;
import static com.loja.checkout.ResumoPedidoFixtures.item;
import static com.loja.checkout.ResumoPedidoFixtures.pedido;
import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraResumoPedidoTest {

    private final CalculadoraResumoPedido calculadora = new CalculadoraResumoPedido(
            new CatalogoEntregas(List.of(new EntregaEconomica(), new EntregaExpressa(),
                    new RetiradaLoja(), new EntregaMotoboy())),
            new CatalogoCupons(List.of(new CupomBemVindo10(), new CupomMenos50(),
                    new CupomFreteGratis(), new CupomLeve3Pague2())),
            new CatalogoNiveisClube(List.of(new NivelBronze(), new NivelPrata(), new NivelOuro())),
            new CatalogoFormasPagamento(List.of(new PagamentoPix(), new PagamentoCartao(),
                    new PagamentoBoleto())),
            new CalculadoraSeguro());

    @Test
    void exemplo1_expressaComBemvindo10NoPixBronzeNorte() {
        ResumoPedidoResponse resumo = calculadora.calcular(pedido(carrinhoPadrao(),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualByComparingTo("10.24");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.60");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("391.47");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("391.47");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo2_economicaNoCartaoEm6xPrataCentroOeste() {
        ResumoPedidoResponse resumo = calculadora.calcular(pedido(carrinhoPadrao(),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("15.60");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.seguro()).isEqualByComparingTo("6.15");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("30.55");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("462.00");
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("77.00");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("8.19");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo3_motoboyComMenos50NoBoletoBronzeNordeste() {
        ResumoPedidoResponse resumo = calculadora.calcular(pedido(List.of(FONE),
                "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isZero();
        assertThat(resumo.seguro()).isEqualByComparingTo("8.00");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("379.29");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("379.29");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo4_retiradaComLeve3Pague2NoCartaoEm3xPrataSul() {
        ResumoPedidoResponse resumo = calculadora.calcular(pedido(List.of(MEIA, CAMISETA),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.seguro()).isEqualByComparingTo("2.99");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("262.29");
        assertThat(resumo.parcelas()).isEqualTo(3);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("87.43");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("5.98");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo5_ouroNaoPagaFreteNoPixSudeste() {
        ResumoPedidoResponse resumo = calculadora.calcular(pedido(carrinhoPadrao(),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualByComparingTo("4.10");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.69");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("393.11");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("393.11");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemploDoAnexo_ouroComBemvindo10NoPixSudeste() {
        ResumoPedidoResponse resumo = calculadora.calcular(pedido(carrinhoPadrao(),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.seguro()).isEqualByComparingTo("4.10");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-18.64");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("354.19");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
    }

    @Test
    void freteGratisDescontaExatamenteOFrete() {
        ResumoPedidoResponse resumo = calculadora.calcular(pedido(carrinhoPadrao(),
                "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "BRONZE", "SUDESTE"));

        // produtos 409,70 - cupom 33,10 + frete 33,10 + seguro 4,10
        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("33.10");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("413.80");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
    }

    @Test
    void ouroAcimaDe500EmProdutosGanhaBrinde() {
        ResumoPedidoResponse resumo = calculadora.calcular(pedido(List.of(item("Jaqueta", "600.00", 1, "1.00")),
                "ECONOMICA", null, "PIX", 1, "OURO", "SUDESTE"));

        assertThat(resumo.brinde()).isTrue();
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("30.00");
    }

    @Test
    void cartaoAte3xNaoTemJuros() {
        ResumoPedidoResponse resumo = calculadora.calcular(pedido(List.of(item("Bolsa", "100.00", 1, "0.50")),
                "RETIRADA_LOJA", null, "CARTAO", 2, "BRONZE", "SUDESTE"));

        // produtos 100,00 + seguro 1,00 = 101,00
        assertThat(resumo.totalFinal()).isEqualByComparingTo("101.00");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resumo.valorParcela()).isEqualByComparingTo("50.50");
    }

    @Test
    void cartaoDe4xEmDianteUsaTabelaPrice() {
        ResumoPedidoResponse resumo = calculadora.calcular(pedido(List.of(item("Bolsa", "1000.00", 1, "0.50")),
                "RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE"));

        // total do pedido 1010,00; parcela Price de 12x a 1,99% = 95,45
        assertThat(resumo.valorParcela()).isEqualByComparingTo("95.45");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("1145.40");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("135.40");
    }

    @Test
    void pedidoVazioOuItemInvalidoEhRecusado() {
        assertThat(erroDe(pedido(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(null, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(List.of(item("Brinde", "0.00", 1, "0.10")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(List.of(item("Camiseta", "79.90", 0, "0.30")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(List.of(item("Camiseta", "79.90", 1, "-0.30")),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
        assertThat(erroDe(pedido(itensCom(new ItemRequest("Camiseta", null, 1, new BigDecimal("0.30"))),
                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void dadosDesconhecidosSaoRecusadosNaOrdemCombinada() {
        assertThat(erroDe(pedido(carrinhoPadrao(), "FOGUETE", "NAOEXISTE", "CHEQUE", 1, "DIAMANTE", "LESTE")))
                .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
        assertThat(erroDe(pedido(carrinhoPadrao(), "FOGUETE", "NAOEXISTE", "CHEQUE", 1, null, "LESTE")))
                .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
        assertThat(erroDe(pedido(carrinhoPadrao(), "FOGUETE", "NAOEXISTE", "CHEQUE", 1, "BRONZE", "LESTE")))
                .isEqualTo(CodigoErro.REGIAO_INVALIDA);
        assertThat(erroDe(pedido(carrinhoPadrao(), "FOGUETE", "NAOEXISTE", "CHEQUE", 1, "BRONZE", null)))
                .isEqualTo(CodigoErro.REGIAO_INVALIDA);
        assertThat(erroDe(pedido(carrinhoPadrao(), "FOGUETE", "NAOEXISTE", "CHEQUE", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
        assertThat(erroDe(pedido(carrinhoPadrao(), null, "NAOEXISTE", "CHEQUE", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
        assertThat(erroDe(pedido(carrinhoPadrao(), "EXPRESSA", "NAOEXISTE", "CHEQUE", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
        assertThat(erroDe(pedido(carrinhoPadrao(), "EXPRESSA", "bemvindo10", "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
        assertThat(erroDe(pedido(carrinhoPadrao(), "EXPRESSA", null, "CHEQUE", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        assertThat(erroDe(pedido(carrinhoPadrao(), "EXPRESSA", null, null, 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void motoboyAcimaDe5kgFicaIndisponivel() {
        List<ItemRequest> pesado = List.of(item("Mala", "300.00", 2, "3.00"));

        assertThat(erroDe(pedido(pesado, "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
        // exatamente 5 kg ainda é atendido
        assertThat(calculadora.calcular(pedido(List.of(item("Mala", "300.00", 1, "5.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE")).frete())
                .isEqualByComparingTo("18.00");
    }

    @Test
    void menos50AbaixoDe300NaoSeAplica() {
        assertThat(erroDe(pedido(List.of(item("Camiseta", "79.90", 2, "0.30")),
                "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void parcelamentoForaDoPermitidoEhRecusado() {
        assertThat(erroDe(pedido(carrinhoPadrao(), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(pedido(carrinhoPadrao(), "EXPRESSA", null, "BOLETO", 3, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(pedido(carrinhoPadrao(), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
        assertThat(erroDe(pedido(carrinhoPadrao(), "EXPRESSA", null, "CARTAO", 0, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void parcelasAusentesValemUmaVez() {
        ResumoPedidoResponse resumo = calculadora.calcular(pedido(carrinhoPadrao(),
                "EXPRESSA", null, "BOLETO", null, "BRONZE", "SUDESTE"));

        assertThat(resumo.parcelas()).isEqualTo(1);
    }

    @Test
    void boletoAcimaDeMilFicaIndisponivel() {
        assertThat(erroDe(pedido(List.of(item("Sofa", "1200.00", 1, "2.00")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void parcelamentoEhConferidoAntesDaDisponibilidadeDaForma() {
        assertThat(erroDe(pedido(List.of(item("Sofa", "1200.00", 1, "2.00")),
                "RETIRADA_LOJA", null, "BOLETO", 2, "BRONZE", "SUDESTE")))
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    /** Executa o cálculo esperando recusa e devolve o código do problema. */
    private CodigoErro erroDe(ResumoPedidoRequest request) {
        try {
            calculadora.calcular(request);
        } catch (CheckoutException excecao) {
            return excecao.getCodigo();
        }
        throw new AssertionError("o pedido deveria ter sido recusado");
    }

    private List<ItemRequest> itensCom(ItemRequest item) {
        List<ItemRequest> itens = new ArrayList<>();
        itens.add(item);
        return itens;
    }
}
