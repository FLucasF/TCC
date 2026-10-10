package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.clube.CatalogoClube;
import com.loja.checkout.cupom.CatalogoCupons;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.CatalogoEntregas;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.erro.PedidoInvalidoException;
import com.loja.checkout.pagamento.CatalogoPagamentos;
import com.loja.checkout.regiao.CatalogoRegioes;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResumoCompraServiceTest {

    private ResumoCompraService service;

    @BeforeEach
    void montarServico() {
        service = new ResumoCompraService(
                new CatalogoEntregas(),
                new CatalogoCupons(),
                new CatalogoClube(),
                new CatalogoPagamentos(),
                new CatalogoRegioes());
    }

    private static ItemRequest camiseta() {
        return new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    }

    private static ItemRequest tenis() {
        return new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    }

    @Test
    void exemplo1() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(camiseta(), tenis()), "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        ResumoResponse resposta = service.calcular(pedido);

        assertThat(resposta.subtotalProdutos()).isEqualTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualTo("40.97");
        assertThat(resposta.frete()).isEqualTo("33.10");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.seguro()).isEqualTo("10.24");
        assertThat(resposta.ajustePagamento()).isEqualTo("-20.60");
        assertThat(resposta.totalFinal()).isEqualTo("391.47");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualTo("391.47");
        assertThat(resposta.creditoProximaCompra()).isEqualTo("0.00");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo2() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(camiseta(), tenis()), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse resposta = service.calcular(pedido);

        assertThat(resposta.subtotalProdutos()).isEqualTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualTo("0.00");
        assertThat(resposta.frete()).isEqualTo("15.60");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(7);
        assertThat(resposta.seguro()).isEqualTo("6.15");
        assertThat(resposta.ajustePagamento()).isEqualTo("30.55");
        assertThat(resposta.totalFinal()).isEqualTo("462.00");
        assertThat(resposta.parcelas()).isEqualTo(6);
        assertThat(resposta.valorParcela()).isEqualTo("77.00");
        assertThat(resposta.creditoProximaCompra()).isEqualTo("8.19");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo3() {
        ItemRequest fone = new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
        ResumoRequest pedido = new ResumoRequest(
                List.of(fone), "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponse resposta = service.calcular(pedido);

        assertThat(resposta.subtotalProdutos()).isEqualTo("399.80");
        assertThat(resposta.descontoCupom()).isEqualTo("50.00");
        assertThat(resposta.frete()).isEqualTo("18.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(0);
        assertThat(resposta.seguro()).isEqualTo("8.00");
        assertThat(resposta.ajustePagamento()).isEqualTo("3.49");
        assertThat(resposta.totalFinal()).isEqualTo("379.29");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualTo("379.29");
        assertThat(resposta.creditoProximaCompra()).isEqualTo("0.00");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo4() {
        ItemRequest meia = new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
        ResumoRequest pedido = new ResumoRequest(
                List.of(meia, camiseta()), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse resposta = service.calcular(pedido);

        assertThat(resposta.subtotalProdutos()).isEqualTo("299.10");
        assertThat(resposta.descontoCupom()).isEqualTo("39.80");
        assertThat(resposta.frete()).isEqualTo("0.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(1);
        assertThat(resposta.seguro()).isEqualTo("2.99");
        assertThat(resposta.ajustePagamento()).isEqualTo("0.00");
        assertThat(resposta.totalFinal()).isEqualTo("262.29");
        assertThat(resposta.parcelas()).isEqualTo(3);
        assertThat(resposta.valorParcela()).isEqualTo("87.43");
        assertThat(resposta.creditoProximaCompra()).isEqualTo("5.98");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo5() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(camiseta(), tenis()), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resposta = service.calcular(pedido);

        assertThat(resposta.subtotalProdutos()).isEqualTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualTo("0.00");
        assertThat(resposta.frete()).isEqualTo("0.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.seguro()).isEqualTo("4.10");
        assertThat(resposta.ajustePagamento()).isEqualTo("-20.69");
        assertThat(resposta.totalFinal()).isEqualTo("393.11");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualTo("393.11");
        assertThat(resposta.creditoProximaCompra()).isEqualTo("20.48");
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void carrinhoVazioDaErro() {
        ResumoRequest pedido = new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting("codigo")
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void motoboyAcimaDoPesoDaErro() {
        ItemRequest pesado = new ItemRequest("Caixa", new BigDecimal("10.00"), 1, new BigDecimal("6"));
        ResumoRequest pedido = new ResumoRequest(
                List.of(pesado), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting("codigo")
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void boletoAcimaDoLimiteDaErro() {
        ItemRequest item = new ItemRequest("Produto", new BigDecimal("2000.00"), 1, new BigDecimal("1"));
        ResumoRequest pedido = new ResumoRequest(
                List.of(item), "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting("codigo")
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void cupomMenos50AbaixoDoMinimoDaErro() {
        ItemRequest item = new ItemRequest("Produto", new BigDecimal("50.00"), 1, new BigDecimal("1"));
        ResumoRequest pedido = new ResumoRequest(
                List.of(item), "RETIRADA_LOJA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting("codigo")
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void motoboyComExatamente5kgEPermitido() {
        ItemRequest item = new ItemRequest("Caixa", new BigDecimal("10.00"), 1, new BigDecimal("5"));
        ResumoRequest pedido = new ResumoRequest(
                List.of(item), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        ResumoResponse resposta = service.calcular(pedido);

        assertThat(resposta.frete()).isEqualTo("18.00");
    }

    @Test
    void boletoComTotalDoPedidoExatamente1000EPermitido() {
        ItemRequest item = new ItemRequest("Produto", new BigDecimal("990.10"), 1, new BigDecimal("1"));
        ResumoRequest pedido = new ResumoRequest(
                List.of(item), "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUL");

        ResumoResponse resposta = service.calcular(pedido);

        assertThat(resposta.ajustePagamento()).isEqualTo("3.49");
        assertThat(resposta.totalFinal()).isEqualTo("1003.49");
    }

    @Test
    void ouroComProdutosAcimaDe500GanhaBrinde() {
        ItemRequest item = new ItemRequest("Produto", new BigDecimal("600.00"), 1, new BigDecimal("1"));
        ResumoRequest pedido = new ResumoRequest(
                List.of(item), "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resposta = service.calcular(pedido);

        assertThat(resposta.brinde()).isTrue();
    }

    @Test
    void cupomFreteGratisZeraOFreteNoDesconto() {
        ResumoRequest pedido = new ResumoRequest(
                List.of(camiseta(), tenis()), "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE");

        ResumoResponse resposta = service.calcular(pedido);

        assertThat(resposta.frete()).isEqualTo("33.10");
        assertThat(resposta.descontoCupom()).isEqualTo("33.10");
        assertThat(resposta.totalFinal()).isEqualTo("393.11");
    }

    @Test
    void nivelClubeInvalidoDaErro() {
        ItemRequest item = new ItemRequest("Produto", new BigDecimal("50.00"), 1, new BigDecimal("1"));
        ResumoRequest pedido = new ResumoRequest(
                List.of(item), "RETIRADA_LOJA", null, "PIX", null, "DIAMANTE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting("codigo")
                .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInvalidaDaErro() {
        ItemRequest item = new ItemRequest("Produto", new BigDecimal("50.00"), 1, new BigDecimal("1"));
        ResumoRequest pedido = new ResumoRequest(
                List.of(item), "RETIRADA_LOJA", null, "PIX", null, "BRONZE", "LUA");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting("codigo")
                .isEqualTo(CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeEntregaInvalidaDaErro() {
        ItemRequest item = new ItemRequest("Produto", new BigDecimal("50.00"), 1, new BigDecimal("1"));
        ResumoRequest pedido = new ResumoRequest(
                List.of(item), "TELETRANSPORTE", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting("codigo")
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void cupomInvalidoDaErro() {
        ItemRequest item = new ItemRequest("Produto", new BigDecimal("50.00"), 1, new BigDecimal("1"));
        ResumoRequest pedido = new ResumoRequest(
                List.of(item), "RETIRADA_LOJA", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting("codigo")
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void formaPagamentoInvalidaDaErro() {
        ItemRequest item = new ItemRequest("Produto", new BigDecimal("50.00"), 1, new BigDecimal("1"));
        ResumoRequest pedido = new ResumoRequest(
                List.of(item), "RETIRADA_LOJA", null, "CHEQUE", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting("codigo")
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void parcelamentoInvalidoParaPixDaErro() {
        ItemRequest item = new ItemRequest("Produto", new BigDecimal("50.00"), 1, new BigDecimal("1"));
        ResumoRequest pedido = new ResumoRequest(
                List.of(item), "RETIRADA_LOJA", null, "PIX", 2, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting("codigo")
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void parcelamentoInvalidoParaCartaoAcimaDe12xDaErro() {
        ItemRequest item = new ItemRequest("Produto", new BigDecimal("50.00"), 1, new BigDecimal("1"));
        ResumoRequest pedido = new ResumoRequest(
                List.of(item), "RETIRADA_LOJA", null, "CARTAO", 13, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting("codigo")
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }
}
