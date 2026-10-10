package com.loja.checkout.service;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.erro.PedidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraResumoServiceTest {

    private final CalculadoraResumoService servico = new CalculadoraResumoService();

    private static final ItemRequest CAMISETA = new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    private static final ItemRequest TENIS = new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        ResumoResponse resposta = servico.calcular(request);

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
    void exemplo2_economica_semCupom_cartao6x_prata_centroOeste() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse resposta = servico.calcular(request);

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
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        ItemRequest fone = new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
        ResumoRequest request = new ResumoRequest(
                List.of(fone), "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponse resposta = servico.calcular(request);

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
    void exemplo4_retiradaLoja_leve3pague2_cartao3x_prata_sul() {
        ItemRequest meia = new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
        ResumoRequest request = new ResumoRequest(
                List.of(meia, CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse resposta = servico.calcular(request);

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
    void exemplo5_expressa_semCupom_pix_ouro_sudeste() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resposta = servico.calcular(request);

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
    void carrinhoVazio_devolveErroPedidoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(), "ECONOMICA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> servico.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting(e -> ((PedidoException) e).getCodigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void motoboyAcimaDoLimite_devolveModalidadeIndisponivel() {
        ItemRequest itemPesado = new ItemRequest("Caixa", new BigDecimal("50.00"), 1, new BigDecimal("6.00"));
        ResumoRequest request = new ResumoRequest(
                List.of(itemPesado), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> servico.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting(e -> ((PedidoException) e).getCodigo())
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void boletoAcimaDoLimite_devolveFormaPagamentoIndisponivel() {
        ItemRequest itemCaro = new ItemRequest("Notebook", new BigDecimal("1500.00"), 1, new BigDecimal("2.00"));
        ResumoRequest request = new ResumoRequest(
                List.of(itemCaro), "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> servico.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting(e -> ((PedidoException) e).getCodigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void parcelamentoInvalidoParaPix() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", 2, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> servico.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting(e -> ((PedidoException) e).getCodigo())
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cupomNaoAplicavel_menos50AbaixoDoMinimo() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "RETIRADA_LOJA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> servico.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting(e -> ((PedidoException) e).getCodigo())
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void fretegratis_comOuro_descontoIgualAoFreteJaZerado() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resposta = servico.calcular(request);

        assertThat(resposta.frete()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resposta.descontoCupom()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void fretegratis_semOuro_descontoIgualAoFreteCobrado() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE");

        ResumoResponse resposta = servico.calcular(request);

        assertThat(resposta.frete()).isEqualTo(new BigDecimal("33.10"));
        assertThat(resposta.descontoCupom()).isEqualTo(new BigDecimal("33.10"));
    }

    @Test
    void ouro_comProdutosAcimaDe500_ganhaBrinde() {
        ItemRequest tenisCaro = new ItemRequest("Tênis", new BigDecimal("600.00"), 1, new BigDecimal("1.00"));
        ResumoRequest request = new ResumoRequest(
                List.of(tenisCaro), "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resposta = servico.calcular(request);

        assertThat(resposta.brinde()).isTrue();
    }

    @Test
    void motoboy_exatamente5kg_disponivel() {
        ItemRequest itemNoLimite = new ItemRequest("Caixa", new BigDecimal("50.00"), 1, new BigDecimal("5.00"));
        ResumoRequest request = new ResumoRequest(
                List.of(itemNoLimite), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        ResumoResponse resposta = servico.calcular(request);

        assertThat(resposta.frete()).isEqualTo(new BigDecimal("18.00"));
    }

    @Test
    void boleto_totalExatamente1000_disponivel() {
        ItemRequest item = new ItemRequest("Item", new BigDecimal("990.10"), 1, new BigDecimal("1.00"));
        ResumoRequest request = new ResumoRequest(
                List.of(item), "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        ResumoResponse resposta = servico.calcular(request);

        assertThat(resposta.seguro()).isEqualTo(new BigDecimal("9.90"));
        assertThat(resposta.totalFinal()).isEqualTo(new BigDecimal("1003.49"));
    }

    @Test
    void cupomVazio_tratadoComoSemCupom() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "RETIRADA_LOJA", "", "PIX", null, "BRONZE", "SUDESTE");

        ResumoResponse resposta = servico.calcular(request);

        assertThat(resposta.descontoCupom()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void cupomInvalido_devolveErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "RETIRADA_LOJA", "NAO_EXISTE", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> servico.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting(e -> ((PedidoException) e).getCodigo())
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void nivelClubeInvalido_devolveErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", null, "DIAMANTE", "SUDESTE");

        assertThatThrownBy(() -> servico.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting(e -> ((PedidoException) e).getCodigo())
                .isEqualTo(CodigoErro.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInvalida_devolveErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", null, "BRONZE", "LUA");

        assertThatThrownBy(() -> servico.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting(e -> ((PedidoException) e).getCodigo())
                .isEqualTo(CodigoErro.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInvalida_devolveErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "TELEPORTE", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> servico.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting(e -> ((PedidoException) e).getCodigo())
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void formaPagamentoInvalida_devolveErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "RETIRADA_LOJA", null, "CRIPTO", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> servico.calcular(request))
                .isInstanceOf(PedidoException.class)
                .extracting(e -> ((PedidoException) e).getCodigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }
}
