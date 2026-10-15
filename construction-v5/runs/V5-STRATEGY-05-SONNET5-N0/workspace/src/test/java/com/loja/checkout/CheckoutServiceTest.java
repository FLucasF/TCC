package com.loja.checkout;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CheckoutServiceTest {

    private final CheckoutService service = new CheckoutService();

    private static ItemRequest camiseta() {
        return new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    }

    private static ItemRequest tenis() {
        return new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    }

    private static ItemRequest fone() {
        return new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
    }

    private static ItemRequest meia() {
        return new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
    }

    @Test
    void exemplo1_expressaBemvindo10PixBronzeNorte() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta(), tenis()), "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        ResumoResponse resposta = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), resposta.subtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resposta.descontoCupom());
        assertEquals(new BigDecimal("33.10"), resposta.frete());
        assertEquals(2, resposta.prazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), resposta.seguro());
        assertEquals(new BigDecimal("-20.60"), resposta.ajustePagamento());
        assertEquals(new BigDecimal("391.47"), resposta.totalFinal());
        assertEquals(1, resposta.parcelas());
        assertEquals(new BigDecimal("391.47"), resposta.valorParcela());
        assertEquals(new BigDecimal("0.00"), resposta.creditoProximaCompra());
        assertFalse(resposta.brinde());
    }

    @Test
    void exemplo2_economicaSemCupomCartao6xPrataCentroOeste() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta(), tenis()), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse resposta = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), resposta.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resposta.descontoCupom());
        assertEquals(new BigDecimal("15.60"), resposta.frete());
        assertEquals(7, resposta.prazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), resposta.seguro());
        assertEquals(new BigDecimal("30.55"), resposta.ajustePagamento());
        assertEquals(new BigDecimal("462.00"), resposta.totalFinal());
        assertEquals(6, resposta.parcelas());
        assertEquals(new BigDecimal("77.00"), resposta.valorParcela());
        assertEquals(new BigDecimal("8.19"), resposta.creditoProximaCompra());
        assertFalse(resposta.brinde());
    }

    @Test
    void exemplo3_motoboyMenos50BoletoBronzeNordeste() {
        ResumoRequest request = new ResumoRequest(
                List.of(fone()), "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponse resposta = service.calcularResumo(request);

        assertEquals(new BigDecimal("399.80"), resposta.subtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resposta.descontoCupom());
        assertEquals(new BigDecimal("18.00"), resposta.frete());
        assertEquals(0, resposta.prazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), resposta.seguro());
        assertEquals(new BigDecimal("3.49"), resposta.ajustePagamento());
        assertEquals(new BigDecimal("379.29"), resposta.totalFinal());
        assertEquals(1, resposta.parcelas());
        assertEquals(new BigDecimal("379.29"), resposta.valorParcela());
        assertEquals(new BigDecimal("0.00"), resposta.creditoProximaCompra());
        assertFalse(resposta.brinde());
    }

    @Test
    void exemplo4_retiradaLeve3Pague2Cartao3xPrataSul() {
        ResumoRequest request = new ResumoRequest(
                List.of(meia(), camiseta()), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse resposta = service.calcularResumo(request);

        assertEquals(new BigDecimal("299.10"), resposta.subtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resposta.descontoCupom());
        assertEquals(new BigDecimal("0.00"), resposta.frete());
        assertEquals(1, resposta.prazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), resposta.seguro());
        assertEquals(new BigDecimal("0.00"), resposta.ajustePagamento());
        assertEquals(new BigDecimal("262.29"), resposta.totalFinal());
        assertEquals(3, resposta.parcelas());
        assertEquals(new BigDecimal("87.43"), resposta.valorParcela());
        assertEquals(new BigDecimal("5.98"), resposta.creditoProximaCompra());
        assertFalse(resposta.brinde());
    }

    @Test
    void exemplo5_expressaSemCupomPixOuroSudeste() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta(), tenis()), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resposta = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), resposta.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resposta.descontoCupom());
        assertEquals(new BigDecimal("0.00"), resposta.frete());
        assertEquals(2, resposta.prazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), resposta.seguro());
        assertEquals(new BigDecimal("-20.69"), resposta.ajustePagamento());
        assertEquals(new BigDecimal("393.11"), resposta.totalFinal());
        assertEquals(1, resposta.parcelas());
        assertEquals(new BigDecimal("393.11"), resposta.valorParcela());
        assertEquals(new BigDecimal("20.48"), resposta.creditoProximaCompra());
        assertFalse(resposta.brinde());
    }

    @Test
    void carrinhoVazioDevolvePedidoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(), "ECONOMICA", null, "PIX", null, "BRONZE", "SUDESTE");

        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("PEDIDO_INVALIDO", e.getCodigo());
    }

    @Test
    void itemComQuantidadeZeroDevolvePedidoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(new ItemRequest("Camiseta", new BigDecimal("79.90"), 0, new BigDecimal("0.30"))),
                "ECONOMICA", null, "PIX", null, "BRONZE", "SUDESTE");

        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("PEDIDO_INVALIDO", e.getCodigo());
    }

    @Test
    void nivelClubeInvalidoDevolveErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta()), "ECONOMICA", null, "PIX", null, "DIAMANTE", "SUDESTE");

        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("NIVEL_CLUBE_INVALIDO", e.getCodigo());
    }

    @Test
    void regiaoInvalidaDevolveErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta()), "ECONOMICA", null, "PIX", null, "BRONZE", "LUA");

        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("REGIAO_INVALIDA", e.getCodigo());
    }

    @Test
    void modalidadeInvalidaDevolveErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta()), "TELEPORTE", null, "PIX", null, "BRONZE", "SUDESTE");

        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("MODALIDADE_INVALIDA", e.getCodigo());
    }

    @Test
    void motoboyAcimaDoLimiteDevolveModalidadeIndisponivel() {
        ItemRequest pesado = new ItemRequest("Haltere", new BigDecimal("150.00"), 1, new BigDecimal("6.00"));
        ResumoRequest request = new ResumoRequest(
                List.of(pesado), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("MODALIDADE_INDISPONIVEL", e.getCodigo());
    }

    @Test
    void cupomInvalidoDevolveErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta()), "ECONOMICA", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE");

        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("CUPOM_INVALIDO", e.getCodigo());
    }

    @Test
    void cupomNaoAplicavelDevolveErro() {
        ItemRequest barato = new ItemRequest("Meia", new BigDecimal("19.90"), 1, new BigDecimal("0.10"));
        ResumoRequest request = new ResumoRequest(
                List.of(barato), "ECONOMICA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("CUPOM_NAO_APLICAVEL", e.getCodigo());
    }

    @Test
    void formaPagamentoInvalidaDevolveErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta()), "ECONOMICA", null, "CRIPTO", null, "BRONZE", "SUDESTE");

        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", e.getCodigo());
    }

    @Test
    void parcelamentoInvalidoNoPixDevolveErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta()), "ECONOMICA", null, "PIX", 2, "BRONZE", "SUDESTE");

        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("PARCELAMENTO_INVALIDO", e.getCodigo());
    }

    @Test
    void parcelamentoInvalidoNoCartaoAcimaDe12DevolveErro() {
        ResumoRequest request = new ResumoRequest(
                List.of(camiseta()), "ECONOMICA", null, "CARTAO", 13, "BRONZE", "SUDESTE");

        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("PARCELAMENTO_INVALIDO", e.getCodigo());
    }

    @Test
    void boletoAcimaDeMilDevolveFormaPagamentoIndisponivel() {
        ItemRequest caro = new ItemRequest("Jaqueta de couro", new BigDecimal("1200.00"), 1, new BigDecimal("1.00"));
        ResumoRequest request = new ResumoRequest(
                List.of(caro), "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        CheckoutException e = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", e.getCodigo());
    }

    @Test
    void ouroComSubtotalAcimaDe500GanhaBrinde() {
        ItemRequest caro = new ItemRequest("Jaqueta", new BigDecimal("600.00"), 1, new BigDecimal("1.00"));
        ResumoRequest request = new ResumoRequest(
                List.of(caro), "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resposta = service.calcularResumo(request);

        assertEquals(true, resposta.brinde());
    }
}
