package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loja.checkout.servico.ResumoCompraService;
import com.loja.checkout.validacao.CheckoutException;
import com.loja.checkout.validacao.ErroCodigo;
import com.loja.checkout.web.dto.ItemRequestDTO;
import com.loja.checkout.web.dto.ResumoRequestDTO;
import com.loja.checkout.web.dto.ResumoResponseDTO;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ResumoCompraServiceTest {

    @Autowired
    private ResumoCompraService service;

    private ItemRequestDTO camiseta() {
        return new ItemRequestDTO("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    }

    private ItemRequestDTO tenis() {
        return new ItemRequestDTO("Tenis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    }

    @Test
    void exemplo1() {
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(camiseta(), tenis()), "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        ResumoResponseDTO resumo = service.calcular(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo(new BigDecimal("409.70"));
        assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("40.97"));
        assertThat(resumo.frete()).isEqualTo(new BigDecimal("33.10"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualTo(new BigDecimal("10.24"));
        assertThat(resumo.ajustePagamento()).isEqualTo(new BigDecimal("-20.60"));
        assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("391.47"));
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualTo(new BigDecimal("391.47"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo2() {
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(camiseta(), tenis()), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponseDTO resumo = service.calcular(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo(new BigDecimal("409.70"));
        assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resumo.frete()).isEqualTo(new BigDecimal("15.60"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(7);
        assertThat(resumo.seguro()).isEqualTo(new BigDecimal("6.15"));
        assertThat(resumo.ajustePagamento()).isEqualTo(new BigDecimal("30.55"));
        assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("462.00"));
        assertThat(resumo.parcelas()).isEqualTo(6);
        assertThat(resumo.valorParcela()).isEqualTo(new BigDecimal("77.00"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(new BigDecimal("8.19"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo3() {
        ItemRequestDTO fone = new ItemRequestDTO("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(fone), "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponseDTO resumo = service.calcular(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo(new BigDecimal("399.80"));
        assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("50.00"));
        assertThat(resumo.frete()).isEqualTo(new BigDecimal("18.00"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(0);
        assertThat(resumo.seguro()).isEqualTo(new BigDecimal("8.00"));
        assertThat(resumo.ajustePagamento()).isEqualTo(new BigDecimal("3.49"));
        assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("379.29"));
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualTo(new BigDecimal("379.29"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo4() {
        ItemRequestDTO meia = new ItemRequestDTO("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(meia, camiseta()), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponseDTO resumo = service.calcular(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo(new BigDecimal("299.10"));
        assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("39.80"));
        assertThat(resumo.frete()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(1);
        assertThat(resumo.seguro()).isEqualTo(new BigDecimal("2.99"));
        assertThat(resumo.ajustePagamento()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("262.29"));
        assertThat(resumo.parcelas()).isEqualTo(3);
        assertThat(resumo.valorParcela()).isEqualTo(new BigDecimal("87.43"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(new BigDecimal("5.98"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void exemplo5() {
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(camiseta(), tenis()), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponseDTO resumo = service.calcular(request);

        assertThat(resumo.subtotalProdutos()).isEqualTo(new BigDecimal("409.70"));
        assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resumo.frete()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resumo.prazoEntregaDias()).isEqualTo(2);
        assertThat(resumo.seguro()).isEqualTo(new BigDecimal("4.10"));
        assertThat(resumo.ajustePagamento()).isEqualTo(new BigDecimal("-20.69"));
        assertThat(resumo.totalFinal()).isEqualTo(new BigDecimal("393.11"));
        assertThat(resumo.parcelas()).isEqualTo(1);
        assertThat(resumo.valorParcela()).isEqualTo(new BigDecimal("393.11"));
        assertThat(resumo.creditoProximaCompra()).isEqualTo(new BigDecimal("20.48"));
        assertThat(resumo.brinde()).isFalse();
    }

    @Test
    void carrinhoVazioDevolvePedidoInvalido() {
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(), "ECONOMICA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo(ErroCodigo.PEDIDO_INVALIDO);
    }

    @Test
    void itemComQuantidadeZeroDevolvePedidoInvalido() {
        ItemRequestDTO itemInvalido = new ItemRequestDTO("Boné", new BigDecimal("50.00"), 0, new BigDecimal("0.10"));
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(itemInvalido), "ECONOMICA", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo(ErroCodigo.PEDIDO_INVALIDO);
    }

    @Test
    void nivelClubeInvalido() {
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(camiseta()), "ECONOMICA", null, "PIX", null, "DIAMANTE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo(ErroCodigo.NIVEL_CLUBE_INVALIDO);
    }

    @Test
    void regiaoInvalida() {
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(camiseta()), "ECONOMICA", null, "PIX", null, "BRONZE", "LUA");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo(ErroCodigo.REGIAO_INVALIDA);
    }

    @Test
    void modalidadeInvalida() {
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(camiseta()), "TELEPORTE", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo(ErroCodigo.MODALIDADE_INVALIDA);
    }

    @Test
    void motoboyAcimaDoLimiteDevolveModalidadeIndisponivel() {
        ItemRequestDTO itemPesado = new ItemRequestDTO("Haltere", new BigDecimal("100.00"), 1, new BigDecimal("10.00"));
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(itemPesado), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo(ErroCodigo.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void cupomInvalido() {
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(camiseta()), "ECONOMICA", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo(ErroCodigo.CUPOM_INVALIDO);
    }

    @Test
    void cupomNaoAplicavel() {
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(camiseta()), "ECONOMICA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo(ErroCodigo.CUPOM_NAO_APLICAVEL);
    }

    @Test
    void formaPagamentoInvalida() {
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(camiseta()), "ECONOMICA", null, "CRIPTO", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo(ErroCodigo.FORMA_PAGAMENTO_INVALIDA);
    }

    @Test
    void parcelamentoInvalidoNoPix() {
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(camiseta()), "ECONOMICA", null, "PIX", 2, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo(ErroCodigo.PARCELAMENTO_INVALIDO);
    }

    @Test
    void parcelamentoInvalidoNoCartaoAcimaDe12() {
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(camiseta()), "ECONOMICA", null, "CARTAO", 13, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo(ErroCodigo.PARCELAMENTO_INVALIDO);
    }

    @Test
    void boletoAcimaDoLimiteDevolveFormaPagamentoIndisponivel() {
        ItemRequestDTO itemCaro = new ItemRequestDTO("Notebook", new BigDecimal("1500.00"), 1, new BigDecimal("2.00"));
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(itemCaro), "ECONOMICA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo(ErroCodigo.FORMA_PAGAMENTO_INDISPONIVEL);
    }

    @Test
    void fretegratisComClienteOuroNaoDescontaPorqueFreteJaEstaZerado() {
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(camiseta(), tenis()), "EXPRESSA", "FRETEGRATIS", "PIX", null, "OURO", "SUDESTE");

        ResumoResponseDTO resumo = service.calcular(request);

        assertThat(resumo.frete()).isEqualTo(new BigDecimal("0.00"));
        assertThat(resumo.descontoCupom()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void motoboyIndisponivelPorPesoMesmoComClienteOuro() {
        ItemRequestDTO itemPesado = new ItemRequestDTO("Haltere", new BigDecimal("100.00"), 1, new BigDecimal("10.00"));
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(itemPesado), "MOTOBOY", null, "PIX", null, "OURO", "SUDESTE");

        assertThatThrownBy(() -> service.calcular(request))
                .isInstanceOf(CheckoutException.class)
                .extracting(e -> ((CheckoutException) e).codigo())
                .isEqualTo(ErroCodigo.MODALIDADE_INDISPONIVEL);
    }

    @Test
    void ouroComProdutosAcimaDe500GanhaBrinde() {
        ItemRequestDTO itemCaro = new ItemRequestDTO("Jaqueta", new BigDecimal("600.00"), 1, new BigDecimal("1.00"));
        ResumoRequestDTO request = new ResumoRequestDTO(
                List.of(itemCaro), "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponseDTO resumo = service.calcular(request);

        assertThat(resumo.brinde()).isTrue();
    }
}
