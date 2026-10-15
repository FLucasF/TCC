package com.loja.checkout;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.exception.PedidoRecusadoException;
import com.loja.checkout.service.ResumoCompraService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumoCompraServiceTest {

    private final ResumoCompraService service = new ResumoCompraService();

    private static ItemRequest item(String nome, String precoUnitario, int quantidade, String pesoKg) {
        return new ItemRequest(nome, new BigDecimal(precoUnitario), quantidade, new BigDecimal(pesoKg));
    }

    private static BigDecimal valor(String valor) {
        return new BigDecimal(valor).setScale(2);
    }

    @Test
    void exemplo1_expressaComBemvindo10PixBronzeNorte() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        ResumoResponse r = service.calcular(pedido);

        assertThat(r.subtotalProdutos()).isEqualTo(valor("409.70"));
        assertThat(r.descontoCupom()).isEqualTo(valor("40.97"));
        assertThat(r.frete()).isEqualTo(valor("33.10"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualTo(valor("10.24"));
        assertThat(r.ajustePagamento()).isEqualTo(valor("-20.60"));
        assertThat(r.totalFinal()).isEqualTo(valor("391.47"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualTo(valor("391.47"));
        assertThat(r.creditoProximaCompra()).isEqualTo(valor("0.00"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo2_economicaSemCupomCartao6xPrataCentroOeste() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse r = service.calcular(pedido);

        assertThat(r.subtotalProdutos()).isEqualTo(valor("409.70"));
        assertThat(r.descontoCupom()).isEqualTo(valor("0.00"));
        assertThat(r.frete()).isEqualTo(valor("15.60"));
        assertThat(r.prazoEntregaDias()).isEqualTo(7);
        assertThat(r.seguro()).isEqualTo(valor("6.15"));
        assertThat(r.ajustePagamento()).isEqualTo(valor("30.55"));
        assertThat(r.totalFinal()).isEqualTo(valor("462.00"));
        assertThat(r.parcelas()).isEqualTo(6);
        assertThat(r.valorParcela()).isEqualTo(valor("77.00"));
        assertThat(r.creditoProximaCompra()).isEqualTo(valor("8.19"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo3_motoboyMenos50BoletoBronzeNordeste() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponse r = service.calcular(pedido);

        assertThat(r.subtotalProdutos()).isEqualTo(valor("399.80"));
        assertThat(r.descontoCupom()).isEqualTo(valor("50.00"));
        assertThat(r.frete()).isEqualTo(valor("18.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(0);
        assertThat(r.seguro()).isEqualTo(valor("8.00"));
        assertThat(r.ajustePagamento()).isEqualTo(valor("3.49"));
        assertThat(r.totalFinal()).isEqualTo(valor("379.29"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualTo(valor("379.29"));
        assertThat(r.creditoProximaCompra()).isEqualTo(valor("0.00"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo4_retiradaLojaLeve3Pague2Cartao3xPrataSul() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse r = service.calcular(pedido);

        assertThat(r.subtotalProdutos()).isEqualTo(valor("299.10"));
        assertThat(r.descontoCupom()).isEqualTo(valor("39.80"));
        assertThat(r.frete()).isEqualTo(valor("0.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(1);
        assertThat(r.seguro()).isEqualTo(valor("2.99"));
        assertThat(r.ajustePagamento()).isEqualTo(valor("0.00"));
        assertThat(r.totalFinal()).isEqualTo(valor("262.29"));
        assertThat(r.parcelas()).isEqualTo(3);
        assertThat(r.valorParcela()).isEqualTo(valor("87.43"));
        assertThat(r.creditoProximaCompra()).isEqualTo(valor("5.98"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void exemplo5_expressaSemCupomPixOuroSudeste() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse r = service.calcular(pedido);

        assertThat(r.subtotalProdutos()).isEqualTo(valor("409.70"));
        assertThat(r.descontoCupom()).isEqualTo(valor("0.00"));
        assertThat(r.frete()).isEqualTo(valor("0.00"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualTo(valor("4.10"));
        assertThat(r.ajustePagamento()).isEqualTo(valor("-20.69"));
        assertThat(r.totalFinal()).isEqualTo(valor("393.11"));
        assertThat(r.parcelas()).isEqualTo(1);
        assertThat(r.valorParcela()).isEqualTo(valor("393.11"));
        assertThat(r.creditoProximaCompra()).isEqualTo(valor("20.48"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void cupomFretegratisZeraDescontoIgualAoFrete() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE");

        ResumoResponse r = service.calcular(pedido);

        assertThat(r.subtotalProdutos()).isEqualTo(valor("249.90"));
        assertThat(r.descontoCupom()).isEqualTo(valor("30.40"));
        assertThat(r.frete()).isEqualTo(valor("30.40"));
        assertThat(r.prazoEntregaDias()).isEqualTo(2);
        assertThat(r.seguro()).isEqualTo(valor("2.50"));
        assertThat(r.ajustePagamento()).isEqualTo(valor("-12.62"));
        assertThat(r.totalFinal()).isEqualTo(valor("239.78"));
        assertThat(r.creditoProximaCompra()).isEqualTo(valor("0.00"));
        assertThat(r.brinde()).isFalse();
    }

    @Test
    void ouroComProdutosAcimaDe500GanhaBrinde() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(item("Tenis", "249.90", 3, "1.20")),
                "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse r = service.calcular(pedido);

        assertThat(r.subtotalProdutos()).isEqualTo(valor("749.70"));
        assertThat(r.frete()).isEqualTo(valor("0.00"));
        assertThat(r.seguro()).isEqualTo(valor("7.50"));
        assertThat(r.ajustePagamento()).isEqualTo(valor("-37.86"));
        assertThat(r.totalFinal()).isEqualTo(valor("719.34"));
        assertThat(r.creditoProximaCompra()).isEqualTo(valor("37.48"));
        assertThat(r.brinde()).isTrue();
    }

    @Test
    void carrinhoVazioRetornaPedidoInvalido() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(ex -> ((PedidoRecusadoException) ex).getCodigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void motoboyAcimaDoPesoRetornaModalidadeIndisponivel() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(item("Caixa", "10.00", 1, "6.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(ex -> ((PedidoRecusadoException) ex).getCodigo())
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void menos50AbaixoDoMinimoRetornaCupomNaoAplicavel() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(item("Meia", "19.90", 1, "0.10")),
                "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(ex -> ((PedidoRecusadoException) ex).getCodigo())
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void boletoAcimaDoLimiteRetornaFormaPagamentoIndisponivel() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(item("Notebook", "2000.00", 1, "2.00")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(ex -> ((PedidoRecusadoException) ex).getCodigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void nivelClubeInexistenteRetornaNivelClubeInvalido() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(item("Meia", "19.90", 1, "0.10")),
                "RETIRADA_LOJA", null, "PIX", 1, "DIAMANTE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(ex -> ((PedidoRecusadoException) ex).getCodigo())
                .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInexistenteRetornaRegiaoInvalida() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(item("Meia", "19.90", 1, "0.10")),
                "RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "EXTERIOR");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(ex -> ((PedidoRecusadoException) ex).getCodigo())
                .isEqualTo(CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInexistenteRetornaModalidadeInvalida() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(item("Meia", "19.90", 1, "0.10")),
                "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(ex -> ((PedidoRecusadoException) ex).getCodigo())
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void cupomInexistenteRetornaCupomInvalido() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(item("Meia", "19.90", 1, "0.10")),
                "RETIRADA_LOJA", "NAOEXISTE", "PIX", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(ex -> ((PedidoRecusadoException) ex).getCodigo())
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void formaPagamentoInexistenteRetornaFormaPagamentoInvalida() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(item("Meia", "19.90", 1, "0.10")),
                "RETIRADA_LOJA", null, "CRIPTO", 1, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(ex -> ((PedidoRecusadoException) ex).getCodigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void pixEmDuasParcelasRetornaParcelamentoInvalido() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(item("Meia", "19.90", 1, "0.10")),
                "RETIRADA_LOJA", null, "PIX", 2, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(ex -> ((PedidoRecusadoException) ex).getCodigo())
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoEmTrezeParcelasRetornaParcelamentoInvalido() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(item("Meia", "19.90", 1, "0.10")),
                "RETIRADA_LOJA", null, "CARTAO", 13, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoRecusadoException.class)
                .extracting(ex -> ((PedidoRecusadoException) ex).getCodigo())
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }
}
