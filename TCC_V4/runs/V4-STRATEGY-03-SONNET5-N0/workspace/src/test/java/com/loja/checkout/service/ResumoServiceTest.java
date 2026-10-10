package com.loja.checkout.service;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.PedidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumoServiceTest {

    private final ResumoService service = new ResumoService();

    private static ItemRequest item(String nome, String preco, int quantidade, String pesoKg) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(pesoKg));
    }

    private static BigDecimal valor(String v) {
        return new BigDecimal(v);
    }

    @Test
    void exemplo1_expressaBemvindo10PixBronzeNorte() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        ResumoResponse resumo = service.calcular(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo(valor("409.70"));
        assertThat(resumo.descontoCupom()).isEqualTo(valor("40.97"));
        assertThat(resumo.frete()).isEqualTo(valor("33.10"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualTo(valor("10.24"));
        assertThat(resumo.ajustePagamento()).isEqualTo(valor("-20.60"));
        assertThat(resumo.totalFinal()).isEqualTo(valor("391.47"));
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualTo(valor("391.47"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(valor("0.00"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo2_economicaSemCupomCartao6xPrataCentroOeste() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse resumo = service.calcular(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo(valor("409.70"));
        assertThat(resumo.descontoCupom()).isEqualTo(valor("0.00"));
        assertThat(resumo.frete()).isEqualTo(valor("15.60"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.seguro()).isEqualTo(valor("6.15"));
        assertThat(resumo.ajustePagamento()).isEqualTo(valor("30.55"));
        assertThat(resumo.totalFinal()).isEqualTo(valor("462.00"));
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualTo(valor("77.00"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(valor("8.19"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo3_motoboyMenos50BoletoBronzeNordeste() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponse resumo = service.calcular(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo(valor("399.80"));
        assertThat(resumo.descontoCupom()).isEqualTo(valor("50.00"));
        assertThat(resumo.frete()).isEqualTo(valor("18.00"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(0);
        assertThat(resumo.seguro()).isEqualTo(valor("8.00"));
        assertThat(resumo.ajustePagamento()).isEqualTo(valor("3.49"));
        assertThat(resumo.totalFinal()).isEqualTo(valor("379.29"));
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualTo(valor("379.29"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(valor("0.00"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo4_retiradaLoja3pague2Cartao3xPrataSul() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse resumo = service.calcular(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo(valor("299.10"));
        assertThat(resumo.descontoCupom()).isEqualTo(valor("39.80"));
        assertThat(resumo.frete()).isEqualTo(valor("0.00"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.seguro()).isEqualTo(valor("2.99"));
        assertThat(resumo.ajustePagamento()).isEqualTo(valor("0.00"));
        assertThat(resumo.totalFinal()).isEqualTo(valor("262.29"));
        assertThat(resumo.parcelas()).isEqualTo(3);
        assertThat(resumo.valorParcela()).isEqualTo(valor("87.43"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(valor("5.98"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo5_expressaSemCupomPixOuroSudeste() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tenis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resumo = service.calcular(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo(valor("409.70"));
        assertThat(resumo.descontoCupom()).isEqualTo(valor("0.00"));
        assertThat(resumo.frete()).isEqualTo(valor("0.00"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualTo(valor("4.10"));
        assertThat(resumo.ajustePagamento()).isEqualTo(valor("-20.69"));
        assertThat(resumo.totalFinal()).isEqualTo(valor("393.11"));
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualTo(valor("393.11"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(valor("20.48"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void carrinhoVazioDaPedidoInvalido() {
        ResumoRequest request = new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting("codigo").isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void itemComPrecoZeroDaPedidoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "0", 1, "0.30")),
                "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting("codigo").isEqualTo("PEDIDO_INVALIDO");
    }

    @Test
    void nivelClubeInvalidoDaErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")),
                "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting("codigo").isEqualTo("NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoInvalidaDaErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")),
                "EXPRESSA", null, "PIX", null, "BRONZE", "LUA");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting("codigo").isEqualTo("REGIAO_INVALIDA");
    }

    @Test
    void modalidadeInvalidaDaErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")),
                "TELETRANSPORTE", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting("codigo").isEqualTo("MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyAcimaDe5kgDaModalidadeIndisponivel() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Caixa", "100.00", 1, "6.00")),
                "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting("codigo").isEqualTo("MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInvalidoDaErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")),
                "EXPRESSA", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting("codigo").isEqualTo("CUPOM_INVALIDO");
    }

    @Test
    void menos50AbaixoDe300DaCupomNaoAplicavel() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")),
                "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting("codigo").isEqualTo("CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInvalidaDaErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")),
                "EXPRESSA", null, "CRIPTOMOEDA", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting("codigo").isEqualTo("FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void pixEm2xDaParcelamentoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")),
                "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting("codigo").isEqualTo("PARCELAMENTO_INVALIDO");
    }

    @Test
    void cartaoEm13xDaParcelamentoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")),
                "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting("codigo").isEqualTo("PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDe1000DaFormaPagamentoIndisponivel() {
        ResumoRequest request = new ResumoRequest(
                List.of(item("Notebook", "2000.00", 1, "2.00")),
                "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting("codigo").isEqualTo("FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
