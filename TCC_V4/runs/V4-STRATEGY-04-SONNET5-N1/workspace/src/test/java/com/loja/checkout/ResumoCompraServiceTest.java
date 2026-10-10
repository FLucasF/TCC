package com.loja.checkout;

import com.loja.checkout.service.ResumoCompraService;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.PedidoException;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ResumoCompraServiceTest {

    @Autowired
    private ResumoCompraService resumoCompraService;

    private static final ItemRequest CAMISETA = new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    private static final ItemRequest TENIS = new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));

    @Test
    void exemplo1() {
        ResumoRequest requisicao = new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        ResumoResponse resposta = resumoCompraService.calcular(requisicao);

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
        ResumoRequest requisicao = new ResumoRequest(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse resposta = resumoCompraService.calcular(requisicao);

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
        ResumoRequest requisicao = new ResumoRequest(
                List.of(fone), "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponse resposta = resumoCompraService.calcular(requisicao);

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
        ResumoRequest requisicao = new ResumoRequest(
                List.of(meia, CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse resposta = resumoCompraService.calcular(requisicao);

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
        ResumoRequest requisicao = new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resposta = resumoCompraService.calcular(requisicao);

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
    void carrinhoVazioRetornaPedidoInvalido() {
        ResumoRequest requisicao = new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> resumoCompraService.calcular(requisicao))
                .isInstanceOf(PedidoException.class)
                .hasMessage("PEDIDO_INVALIDO");
    }

    @Test
    void motoboyAcimaDoLimiteRetornaModalidadeIndisponivel() {
        ItemRequest itemPesado = new ItemRequest("Caixa", new BigDecimal("10.00"), 1, new BigDecimal("6.00"));
        ResumoRequest requisicao = new ResumoRequest(
                List.of(itemPesado), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> resumoCompraService.calcular(requisicao))
                .isInstanceOf(PedidoException.class)
                .hasMessage("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void boletoAcimaDoLimiteRetornaFormaPagamentoIndisponivel() {
        ItemRequest itemCaro = new ItemRequest("Notebook", new BigDecimal("1200.00"), 1, new BigDecimal("2.00"));
        ResumoRequest requisicao = new ResumoRequest(
                List.of(itemCaro), "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> resumoCompraService.calcular(requisicao))
                .isInstanceOf(PedidoException.class)
                .hasMessage("FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void cupomMenos50AbaixoDoMinimoRetornaCupomNaoAplicavel() {
        ItemRequest item = new ItemRequest("Boné", new BigDecimal("50.00"), 1, new BigDecimal("0.20"));
        ResumoRequest requisicao = new ResumoRequest(
                List.of(item), "RETIRADA_LOJA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> resumoCompraService.calcular(requisicao))
                .isInstanceOf(PedidoException.class)
                .hasMessage("CUPOM_NAO_APLICAVEL");
    }
}
