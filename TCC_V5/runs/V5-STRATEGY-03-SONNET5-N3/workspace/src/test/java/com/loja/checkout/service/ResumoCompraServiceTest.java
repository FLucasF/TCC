package com.loja.checkout.service;

import com.loja.checkout.dto.ItemDTO;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.erro.PedidoInvalidoException;
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
    private ResumoCompraService service;

    private static ItemDTO camiseta(int quantidade) {
        return new ItemDTO("Camiseta", new BigDecimal("79.90"), quantidade, new BigDecimal("0.30"));
    }

    private static ItemDTO tenis() {
        return new ItemDTO("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    }

    private static ItemDTO fone(int quantidade) {
        return new ItemDTO("Fone", new BigDecimal("199.90"), quantidade, new BigDecimal("0.25"));
    }

    private static ItemDTO meia(int quantidade) {
        return new ItemDTO("Meia", new BigDecimal("19.90"), quantidade, new BigDecimal("0.10"));
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(camiseta(2), tenis()),
                "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        ResumoResponse resumo = service.calcular(pedido);

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
    void exemplo2_economica_semCupom_cartao6x_prata_centroOeste() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(camiseta(2), tenis()),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse resumo = service.calcular(pedido);

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
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(fone(2)),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponse resumo = service.calcular(pedido);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resumo.frete()).isEqualByComparingTo("18.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(0);
        assertThat(resumo.seguro()).isEqualByComparingTo("8.00");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("379.29");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("379.29");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("0.00");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo4_retiradaLoja_leve3pague2_cartao3x_prata_sul() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(meia(7), camiseta(2)),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse resumo = service.calcular(pedido);

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
    void exemplo5_expressa_semCupom_pix_ouro_sudeste() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(camiseta(2), tenis()),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resumo = service.calcular(pedido);

        assertThat(resumo.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resumo.frete()).isEqualByComparingTo("0.00");
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualByComparingTo("4.10");
        assertThat(resumo.ajustePagamento()).isEqualByComparingTo("-20.69");
        assertThat(resumo.totalFinal()).isEqualByComparingTo("393.11");
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualByComparingTo("393.11");
        assertThat(resumo.creditoProximaCompra()).isEqualByComparingTo("20.48");
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void carrinhoVazio_devolvePedidoInvalido() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(), "ECONOMICA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting(e -> ((PedidoInvalidoException) e).getCodigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void motoboyAcimaDoPeso_devolveModalidadeIndisponivel() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(new ItemDTO("Caixa", new BigDecimal("10.00"), 1, new BigDecimal("6.00"))),
                "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting(e -> ((PedidoInvalidoException) e).getCodigo())
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void boletoAcimaDoLimite_devolveFormaPagamentoIndisponivel() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(new ItemDTO("Notebook", new BigDecimal("1200.00"), 1, new BigDecimal("2.00"))),
                "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting(e -> ((PedidoInvalidoException) e).getCodigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void pixParcelado_devolveParcelamentoInvalido() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(camiseta(1)), "ECONOMICA", null, "PIX", 2, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting(e -> ((PedidoInvalidoException) e).getCodigo())
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void nivelClubeInexistente_devolveNivelClubeInvalido() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(camiseta(1)), "ECONOMICA", null, "PIX", null, "DIAMANTE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting(e -> ((PedidoInvalidoException) e).getCodigo())
                .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInexistente_devolveRegiaoInvalida() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(camiseta(1)), "ECONOMICA", null, "PIX", null, "BRONZE", "OCEANIA");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting(e -> ((PedidoInvalidoException) e).getCodigo())
                .isEqualTo(CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInexistente_devolveModalidadeInvalida() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(camiseta(1)), "TELEPORTE", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting(e -> ((PedidoInvalidoException) e).getCodigo())
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void cupomInexistente_devolveCupomInvalido() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(camiseta(1)), "ECONOMICA", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting(e -> ((PedidoInvalidoException) e).getCodigo())
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void menos50AbaixoDoMinimo_devolveCupomNaoAplicavel() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(camiseta(1)), "ECONOMICA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting(e -> ((PedidoInvalidoException) e).getCodigo())
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInexistente_devolveFormaPagamentoInvalida() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(camiseta(1)), "ECONOMICA", null, "CRIPTOMOEDA", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting(e -> ((PedidoInvalidoException) e).getCodigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void cartaoForaDoLimiteDeParcelas_devolveParcelamentoInvalido() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(camiseta(1)), "ECONOMICA", null, "CARTAO", 13, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(pedido))
                .isInstanceOf(PedidoInvalidoException.class)
                .extracting(e -> ((PedidoInvalidoException) e).getCodigo())
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void freteGratis_zeraDescontoIgualAoFrete() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(camiseta(2), tenis()),
                "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE");

        ResumoResponse resumo = service.calcular(pedido);

        assertThat(resumo.frete()).isEqualByComparingTo("33.10");
        assertThat(resumo.descontoCupom()).isEqualByComparingTo(resumo.frete());
    }

    @Test
    void ouroComProdutosAcimaDoLimite_concedeBrinde() {
        PedidoRequest pedido = new PedidoRequest(
                List.of(tenis(), tenis(), tenis()),
                "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resumo = service.calcular(pedido);

        assertThat(resumo.brinde()).isTrue();
    }
}
