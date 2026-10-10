package com.loja.checkout;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.ResumoService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResumoServiceTest {

    private final ResumoService service = new ResumoService();

    private static final ItemRequest CAMISETA = new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    private static final ItemRequest TENIS = new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));

    @Test
    void exemplo1() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        ResumoResponse resp = service.calcular(request);

        assertEquals(new BigDecimal("409.70"), resp.subtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resp.descontoCupom());
        assertEquals(new BigDecimal("33.10"), resp.frete());
        assertEquals(2, resp.prazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), resp.seguro());
        assertEquals(new BigDecimal("-20.60"), resp.ajustePagamento());
        assertEquals(new BigDecimal("391.47"), resp.totalFinal());
        assertEquals(1, resp.parcelas());
        assertEquals(new BigDecimal("391.47"), resp.valorParcela());
        assertEquals(new BigDecimal("0.00"), resp.creditoProximaCompra());
        assertFalse(resp.brinde());
    }

    @Test
    void exemplo2() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA, TENIS), "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse resp = service.calcular(request);

        assertEquals(new BigDecimal("409.70"), resp.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resp.descontoCupom());
        assertEquals(new BigDecimal("15.60"), resp.frete());
        assertEquals(7, resp.prazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), resp.seguro());
        assertEquals(new BigDecimal("30.55"), resp.ajustePagamento());
        assertEquals(new BigDecimal("462.00"), resp.totalFinal());
        assertEquals(6, resp.parcelas());
        assertEquals(new BigDecimal("77.00"), resp.valorParcela());
        assertEquals(new BigDecimal("8.19"), resp.creditoProximaCompra());
        assertFalse(resp.brinde());
    }

    @Test
    void exemplo3() {
        ItemRequest fone = new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
        ResumoRequest request = new ResumoRequest(
                List.of(fone), "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoResponse resp = service.calcular(request);

        assertEquals(new BigDecimal("399.80"), resp.subtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resp.descontoCupom());
        assertEquals(new BigDecimal("18.00"), resp.frete());
        assertEquals(0, resp.prazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), resp.seguro());
        assertEquals(new BigDecimal("3.49"), resp.ajustePagamento());
        assertEquals(new BigDecimal("379.29"), resp.totalFinal());
        assertEquals(1, resp.parcelas());
        assertEquals(new BigDecimal("379.29"), resp.valorParcela());
        assertEquals(new BigDecimal("0.00"), resp.creditoProximaCompra());
        assertFalse(resp.brinde());
    }

    @Test
    void exemplo4() {
        ItemRequest meia = new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
        ResumoRequest request = new ResumoRequest(
                List.of(meia, CAMISETA), "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse resp = service.calcular(request);

        assertEquals(new BigDecimal("299.10"), resp.subtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resp.descontoCupom());
        assertEquals(new BigDecimal("0.00"), resp.frete());
        assertEquals(1, resp.prazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), resp.seguro());
        assertEquals(new BigDecimal("0.00"), resp.ajustePagamento());
        assertEquals(new BigDecimal("262.29"), resp.totalFinal());
        assertEquals(3, resp.parcelas());
        assertEquals(new BigDecimal("87.43"), resp.valorParcela());
        assertEquals(new BigDecimal("5.98"), resp.creditoProximaCompra());
        assertFalse(resp.brinde());
    }

    @Test
    void exemplo5() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA, TENIS), "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resp = service.calcular(request);

        assertEquals(new BigDecimal("409.70"), resp.subtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resp.descontoCupom());
        assertEquals(new BigDecimal("0.00"), resp.frete());
        assertEquals(2, resp.prazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), resp.seguro());
        assertEquals(new BigDecimal("-20.69"), resp.ajustePagamento());
        assertEquals(new BigDecimal("393.11"), resp.totalFinal());
        assertEquals(1, resp.parcelas());
        assertEquals(new BigDecimal("393.11"), resp.valorParcela());
        assertEquals(new BigDecimal("20.48"), resp.creditoProximaCompra());
        assertFalse(resp.brinde());
    }

    @Test
    void carrinhoVazioDevolvePedidoInvalido() {
        ResumoRequest request = new ResumoRequest(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("PEDIDO_INVALIDO", ex.getCodigo());
    }

    @Test
    void motoboyAcimaDoLimiteDevolveModalidadeIndisponivel() {
        ItemRequest pesado = new ItemRequest("Caixa", new BigDecimal("100.00"), 1, new BigDecimal("6.00"));
        ResumoRequest request = new ResumoRequest(
                List.of(pesado), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("MODALIDADE_INDISPONIVEL", ex.getCodigo());
    }

    @Test
    void boletoAcimaDoLimiteDevolveFormaPagamentoIndisponivel() {
        ItemRequest caro = new ItemRequest("Jaqueta", new BigDecimal("1200.00"), 1, new BigDecimal("1.00"));
        ResumoRequest request = new ResumoRequest(
                List.of(caro), "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ex.getCodigo());
    }

    @Test
    void itemComPrecoZeroDevolvePedidoInvalido() {
        ItemRequest invalido = new ItemRequest("Boné", BigDecimal.ZERO, 1, new BigDecimal("0.10"));
        ResumoRequest request = new ResumoRequest(
                List.of(invalido), "RETIRADA_LOJA", null, "PIX", null, "BRONZE", "SUDESTE");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("PEDIDO_INVALIDO", ex.getCodigo());
    }

    @Test
    void nivelClubeInvalidoDevolveNivelClubeInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", null, "DIAMANTE", "SUDESTE");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("NIVEL_CLUBE_INVALIDO", ex.getCodigo());
    }

    @Test
    void regiaoInvalidaDevolveRegiaoInvalida() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", null, "BRONZE", "LUA");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("REGIAO_INVALIDA", ex.getCodigo());
    }

    @Test
    void modalidadeInvalidaDevolveModalidadeInvalida() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "TELEPORTE", null, "PIX", null, "BRONZE", "SUDESTE");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("MODALIDADE_INVALIDA", ex.getCodigo());
    }

    @Test
    void motoboyComSomaDeItensAcimaDoLimiteDevolveModalidadeIndisponivel() {
        ItemRequest item1 = new ItemRequest("Caixa 1", new BigDecimal("50.00"), 2, new BigDecimal("2.00"));
        ItemRequest item2 = new ItemRequest("Caixa 2", new BigDecimal("50.00"), 1, new BigDecimal("2.50"));
        ResumoRequest request = new ResumoRequest(
                List.of(item1, item2), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("MODALIDADE_INDISPONIVEL", ex.getCodigo());
    }

    @Test
    void cupomInvalidoDevolveCupomInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "RETIRADA_LOJA", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("CUPOM_INVALIDO", ex.getCodigo());
    }

    @Test
    void cupomMenos50AbaixoDoMinimoDevolveCupomNaoAplicavel() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "RETIRADA_LOJA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("CUPOM_NAO_APLICAVEL", ex.getCodigo());
    }

    @Test
    void formaPagamentoInvalidaDevolveFormaPagamentoInvalida() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "RETIRADA_LOJA", null, "CRIPTO", null, "BRONZE", "SUDESTE");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", ex.getCodigo());
    }

    @Test
    void pixEmDuasParcelasDevolveParcelamentoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "RETIRADA_LOJA", null, "PIX", 2, "BRONZE", "SUDESTE");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("PARCELAMENTO_INVALIDO", ex.getCodigo());
    }

    @Test
    void cartaoEmTrezeParcelasDevolveParcelamentoInvalido() {
        ResumoRequest request = new ResumoRequest(
                List.of(CAMISETA), "RETIRADA_LOJA", null, "CARTAO", 13, "BRONZE", "SUDESTE");

        CheckoutException ex = assertThrows(CheckoutException.class, () -> service.calcular(request));
        assertEquals("PARCELAMENTO_INVALIDO", ex.getCodigo());
    }

    @Test
    void ouroComProdutosAcimaDe500GanhaBrinde() {
        ItemRequest tenisEmQuantidade = new ItemRequest("Tênis", new BigDecimal("249.90"), 3, new BigDecimal("1.20"));
        ResumoRequest request = new ResumoRequest(
                List.of(tenisEmQuantidade), "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoResponse resp = service.calcular(request);

        assertEquals(new BigDecimal("749.70"), resp.subtotalProdutos());
        assertEquals(true, resp.brinde());
    }
}
