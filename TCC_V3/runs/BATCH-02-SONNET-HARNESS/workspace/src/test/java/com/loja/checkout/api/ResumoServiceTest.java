package com.loja.checkout.api;

import com.loja.checkout.cupom.Bemvindo10;
import com.loja.checkout.cupom.CupomRegistry;
import com.loja.checkout.cupom.FreteGratis;
import com.loja.checkout.cupom.Leve3Pague2;
import com.loja.checkout.cupom.Menos50;
import com.loja.checkout.entrega.Economica;
import com.loja.checkout.entrega.Expressa;
import com.loja.checkout.entrega.ModalidadeEntregaRegistry;
import com.loja.checkout.entrega.Motoboy;
import com.loja.checkout.entrega.RetiradaLoja;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.erro.NegocioException;
import com.loja.checkout.pagamento.Boleto;
import com.loja.checkout.pagamento.Cartao;
import com.loja.checkout.pagamento.FormaPagamentoRegistry;
import com.loja.checkout.pagamento.Pix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumoServiceTest {

    private ResumoService service;

    @BeforeEach
    void montarService() {
        ModalidadeEntregaRegistry modalidades = new ModalidadeEntregaRegistry(
                List.of(new Economica(), new Expressa(), new RetiradaLoja(), new Motoboy()));
        CupomRegistry cupons = new CupomRegistry(
                List.of(new Bemvindo10(), new Menos50(), new FreteGratis(), new Leve3Pague2()));
        FormaPagamentoRegistry formasPagamento = new FormaPagamentoRegistry(
                List.of(new Pix(), new Cartao(), new Boleto()));
        service = new ResumoService(modalidades, cupons, formasPagamento);
    }

    private ItemRequest camiseta() {
        return new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    }

    private ItemRequest tenis() {
        return new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    }

    private ItemRequest fone() {
        return new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
    }

    private ItemRequest meia() {
        return new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
    }

    @Test
    void exemplo1_expressaComBemvindo10Pix() {
        ResumoRequest request = new ResumoRequest(List.of(camiseta(), tenis()), "EXPRESSA", "BEMVINDO10", "PIX", 1);

        ResumoResponse resposta = service.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("40.97");
        assertThat(resposta.frete()).isEqualByComparingTo("33.10");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(2);
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("-20.09");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("381.74");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("381.74");
    }

    @Test
    void exemplo2_economicaSemCupomCartao6x() {
        ResumoRequest request = new ResumoRequest(List.of(camiseta(), tenis()), "ECONOMICA", null, "CARTAO", 6);

        ResumoResponse resposta = service.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("409.70");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("0.00");
        assertThat(resposta.frete()).isEqualByComparingTo("15.60");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(7);
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("30.10");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("455.40");
        assertThat(resposta.parcelas()).isEqualTo(6);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("75.90");
    }

    @Test
    void exemplo3_motoboyComMenos50Boleto() {
        ResumoRequest request = new ResumoRequest(List.of(fone()), "MOTOBOY", "MENOS50", "BOLETO", null);

        ResumoResponse resposta = service.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("399.80");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("50.00");
        assertThat(resposta.frete()).isEqualByComparingTo("18.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(0);
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("3.49");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("371.29");
        assertThat(resposta.parcelas()).isEqualTo(1);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("371.29");
    }

    @Test
    void exemplo4_retiradaLojaComLeve3Pague2Cartao3x() {
        ResumoRequest request = new ResumoRequest(List.of(meia(), camiseta()), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3);

        ResumoResponse resposta = service.calcular(request);

        assertThat(resposta.subtotalProdutos()).isEqualByComparingTo("299.10");
        assertThat(resposta.descontoCupom()).isEqualByComparingTo("39.80");
        assertThat(resposta.frete()).isEqualByComparingTo("0.00");
        assertThat(resposta.prazoEntregaDias()).isEqualTo(1);
        assertThat(resposta.ajustePagamento()).isEqualByComparingTo("0.00");
        assertThat(resposta.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(resposta.parcelas()).isEqualTo(3);
        assertThat(resposta.valorParcela()).isEqualByComparingTo("86.43");
    }

    @Test
    void carrinhoVazioDevolvePedidoInvalido() {
        ResumoRequest request = new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", 1);

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(excecao -> ((NegocioException) excecao).getCodigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void itemComQuantidadeZeroDevolvePedidoInvalido() {
        ItemRequest itemInvalido = new ItemRequest("Boné", new BigDecimal("50.00"), 0, new BigDecimal("0.20"));
        ResumoRequest request = new ResumoRequest(List.of(itemInvalido), "EXPRESSA", null, "PIX", 1);

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(excecao -> ((NegocioException) excecao).getCodigo())
                .isEqualTo(CodigoErro.PEDIDO_INVALIDO);
    }

    @Test
    void modalidadeInexistenteDevolveModalidadeInvalida() {
        ResumoRequest request = new ResumoRequest(List.of(camiseta()), "TELEPORTE", null, "PIX", 1);

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(excecao -> ((NegocioException) excecao).getCodigo())
                .isEqualTo(CodigoErro.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDoPesoMaximoDevolveModalidadeIndisponivel() {
        ItemRequest itemPesado = new ItemRequest("Caixa", new BigDecimal("50.00"), 1, new BigDecimal("6.00"));
        ResumoRequest request = new ResumoRequest(List.of(itemPesado), "MOTOBOY", null, "PIX", 1);

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(excecao -> ((NegocioException) excecao).getCodigo())
                .isEqualTo(CodigoErro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInexistenteDevolveCupomInvalido() {
        ResumoRequest request = new ResumoRequest(List.of(camiseta()), "EXPRESSA", "NAOEXISTE", "PIX", 1);

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(excecao -> ((NegocioException) excecao).getCodigo())
                .isEqualTo(CodigoErro.CUPOM_INVALIDO);
    }

    @Test
    void menos50AbaixoDoMinimoDevolveCupomNaoAplicavel() {
        ResumoRequest request = new ResumoRequest(List.of(camiseta()), "EXPRESSA", "MENOS50", "PIX", 1);

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(excecao -> ((NegocioException) excecao).getCodigo())
                .isEqualTo(CodigoErro.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInexistenteDevolveFormaPagamentoInvalida() {
        ResumoRequest request = new ResumoRequest(List.of(camiseta()), "EXPRESSA", null, "CRIPTOMOEDA", 1);

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(excecao -> ((NegocioException) excecao).getCodigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void pixEmDuasParcelasDevolveParcelamentoInvalido() {
        ResumoRequest request = new ResumoRequest(List.of(camiseta()), "EXPRESSA", null, "PIX", 2);

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(excecao -> ((NegocioException) excecao).getCodigo())
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void cartaoEmTrezeParcelasDevolveParcelamentoInvalido() {
        ResumoRequest request = new ResumoRequest(List.of(camiseta()), "EXPRESSA", null, "CARTAO", 13);

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(excecao -> ((NegocioException) excecao).getCodigo())
                .isEqualTo(CodigoErro.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDeMilDevolveFormaPagamentoIndisponivel() {
        ItemRequest itemCaro = new ItemRequest("Notebook", new BigDecimal("1500.00"), 1, new BigDecimal("2.00"));
        ResumoRequest request = new ResumoRequest(List.of(itemCaro), "RETIRADA_LOJA", null, "BOLETO", 1);

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(NegocioException.class)
                .extracting(excecao -> ((NegocioException) excecao).getCodigo())
                .isEqualTo(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
    }
}
