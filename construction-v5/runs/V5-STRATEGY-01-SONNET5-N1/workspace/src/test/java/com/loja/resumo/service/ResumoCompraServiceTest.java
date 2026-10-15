package com.loja.resumo.service;

import com.loja.resumo.web.ItemRequest;
import com.loja.resumo.web.ResumoRequest;
import com.loja.resumo.web.ResumoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResumoCompraServiceTest {

    private final ResumoCompraService service = new ResumoCompraService();

    private static final List<ItemRequest> CAMISETA_TENIS = List.of(
            new ItemRequest("Camiseta", 79.90, 2, 0.30),
            new ItemRequest("Tênis", 249.90, 1, 1.20)
    );

    @Test
    void exemplo1_expressaBemvindo10PixBronzeNorte() {
        ResumoResponse resposta = service.calcular(new ResumoRequest(
                CAMISETA_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"));

        assertThat(resposta.subtotalProdutos()).isEqualTo(new BigDecimal("409.70"));
        assertThat(resposta.descontoCupom()).isEqualTo(new BigDecimal("40.97"));
        assertThat(resposta.frete()).isEqualTo(new BigDecimal("33.10"));
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.seguro()).isEqualTo(new BigDecimal("10.24"));
        assertThat(resposta.ajustePagamento()).isEqualTo(new BigDecimal("-20.60"));
        assertThat(resposta.totalFinal()).isEqualTo(new BigDecimal("391.47"));
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualTo(new BigDecimal("391.47"));
        assertThat(resposta.creditoProximaCompra()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo2_economicaSemCupomCartao6xPrataCentroOeste() {
        ResumoResponse resposta = service.calcular(new ResumoRequest(
                CAMISETA_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"));

        assertThat(resposta.subtotalProdutos()).isEqualTo(new BigDecimal("409.70"));
        assertThat(resposta.descontoCupom()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resposta.frete()).isEqualTo(new BigDecimal("15.60"));
        assertThat(resposta.prazoEntregaDias()).isEqualTo(7);
        assertThat(resposta.seguro()).isEqualTo(new BigDecimal("6.15"));
        assertThat(resposta.ajustePagamento()).isEqualTo(new BigDecimal("30.55"));
        assertThat(resposta.totalFinal()).isEqualTo(new BigDecimal("462.00"));
        assertThat(resposta.parcelas()).isEqualTo(6);
        assertThat(resposta.valorParcela()).isEqualTo(new BigDecimal("77.00"));
        assertThat(resposta.creditoProximaCompra()).isEqualTo(new BigDecimal("8.19"));
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo3_motoboyMenos50BoletoBronzeNordeste() {
        List<ItemRequest> itens = List.of(new ItemRequest("Fone", 199.90, 2, 0.25));

        ResumoResponse resposta = service.calcular(new ResumoRequest(
                itens, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"));

        assertThat(resposta.subtotalProdutos()).isEqualTo(new BigDecimal("399.80"));
        assertThat(resposta.descontoCupom()).isEqualTo(new BigDecimal("50.00"));
        assertThat(resposta.frete()).isEqualTo(new BigDecimal("18.00"));
        assertThat(resposta.prazoEntregaDias()).isEqualTo(0);
        assertThat(resposta.seguro()).isEqualTo(new BigDecimal("8.00"));
        assertThat(resposta.ajustePagamento()).isEqualTo(new BigDecimal("3.49"));
        assertThat(resposta.totalFinal()).isEqualTo(new BigDecimal("379.29"));
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualTo(new BigDecimal("379.29"));
        assertThat(resposta.creditoProximaCompra()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo4_retiradaLeve3Pague2Cartao3xPrataSul() {
        List<ItemRequest> itens = List.of(
                new ItemRequest("Meia", 19.90, 7, 0.10),
                new ItemRequest("Camiseta", 79.90, 2, 0.30)
        );

        ResumoResponse resposta = service.calcular(new ResumoRequest(
                itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"));

        assertThat(resposta.subtotalProdutos()).isEqualTo(new BigDecimal("299.10"));
        assertThat(resposta.descontoCupom()).isEqualTo(new BigDecimal("39.80"));
        assertThat(resposta.frete()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resposta.prazoEntregaDias()).isEqualTo(1);
        assertThat(resposta.seguro()).isEqualTo(new BigDecimal("2.99"));
        assertThat(resposta.ajustePagamento()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resposta.totalFinal()).isEqualTo(new BigDecimal("262.29"));
        assertThat(resposta.parcelas()).isEqualTo(3);
        assertThat(resposta.valorParcela()).isEqualTo(new BigDecimal("87.43"));
        assertThat(resposta.creditoProximaCompra()).isEqualTo(new BigDecimal("5.98"));
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void exemplo5_expressaSemCupomPixOuroSudeste() {
        ResumoResponse resposta = service.calcular(new ResumoRequest(
                CAMISETA_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"));

        assertThat(resposta.subtotalProdutos()).isEqualTo(new BigDecimal("409.70"));
        assertThat(resposta.descontoCupom()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resposta.frete()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.seguro()).isEqualTo(new BigDecimal("4.10"));
        assertThat(resposta.ajustePagamento()).isEqualTo(new BigDecimal("-20.69"));
        assertThat(resposta.totalFinal()).isEqualTo(new BigDecimal("393.11"));
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualTo(new BigDecimal("393.11"));
        assertThat(resposta.creditoProximaCompra()).isEqualTo(new BigDecimal("20.48"));
        assertThat(resposta.brinde()).isFalse();
    }

    @Test
    void carrinhoVazioDevolvePedidoInvalido() {
        ErroPedidoException ex = org.junit.jupiter.api.Assertions.assertThrows(ErroPedidoException.class,
                () -> service.calcular(new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE")));
        assertThat(ex.getCodigo()).isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void motoboyAcimaDoLimiteDevolveModalidadeIndisponivel() {
        List<ItemRequest> itens = List.of(new ItemRequest("Caixa", 100.0, 1, 6.0));
        ErroPedidoException ex = org.junit.jupiter.api.Assertions.assertThrows(ErroPedidoException.class,
                () -> service.calcular(new ResumoRequest(itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE")));
        assertThat(ex.getCodigo()).isEqualTo("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void boletoAcimaDoLimiteDevolveFormaPagamentoIndisponivel() {
        List<ItemRequest> itens = List.of(new ItemRequest("Caro", 2000.0, 1, 1.0));
        ErroPedidoException ex = org.junit.jupiter.api.Assertions.assertThrows(ErroPedidoException.class,
                () -> service.calcular(new ResumoRequest(itens, "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE")));
        assertThat(ex.getCodigo()).isEqualTo("FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
