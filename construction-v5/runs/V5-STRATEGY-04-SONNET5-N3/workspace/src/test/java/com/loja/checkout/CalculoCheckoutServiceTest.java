package com.loja.checkout;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoCheckoutRequest;
import com.loja.checkout.api.ResumoCheckoutResponse;
import com.loja.checkout.servico.CalculoCheckoutService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculoCheckoutServiceTest {

    private final CalculoCheckoutService service = new CalculoCheckoutService();

    private ItemRequest item(String nome, String preco, int quantidade, String peso) {
        return new ItemRequest(nome, new BigDecimal(preco), quantidade, new BigDecimal(peso));
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        ResumoCheckoutResponse resposta = service.calcular(request);

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
    void exemplo2_economica_sem_cupom_cartao6x_prata_centroOeste() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoCheckoutResponse resposta = service.calcular(request);

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
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Fone", "199.90", 2, "0.25")),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        ResumoCheckoutResponse resposta = service.calcular(request);

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
    void exemplo4_retiradaLoja_leve3pague2_cartao3x_prata_sul() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Meia", "19.90", 7, "0.10"), item("Camiseta", "79.90", 2, "0.30")),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoCheckoutResponse resposta = service.calcular(request);

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
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Camiseta", "79.90", 2, "0.30"), item("Tênis", "249.90", 1, "1.20")),
                "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoCheckoutResponse resposta = service.calcular(request);

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
    void carrinhoVazio_devolvePedidoInvalido() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(), "ECONOMICA", null, "PIX", null, "BRONZE", "SUDESTE");

        CalculoCheckoutException excecao = assertThrows(CalculoCheckoutException.class, () -> service.calcular(request));
        assertEquals("PEDIDO_INVALIDO", excecao.getCodigo());
    }

    @Test
    void itemComQuantidadeZero_devolvePedidoInvalido() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Camiseta", "79.90", 0, "0.30")), "ECONOMICA", null, "PIX", null, "BRONZE", "SUDESTE");

        CalculoCheckoutException excecao = assertThrows(CalculoCheckoutException.class, () -> service.calcular(request));
        assertEquals("PEDIDO_INVALIDO", excecao.getCodigo());
    }

    @Test
    void nivelClubeInvalido() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "ECONOMICA", null, "PIX", null, "DIAMANTE", "SUDESTE");

        CalculoCheckoutException excecao = assertThrows(CalculoCheckoutException.class, () -> service.calcular(request));
        assertEquals("NIVEL_CLUBE_INVALIDO", excecao.getCodigo());
    }

    @Test
    void regiaoInvalida() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "ECONOMICA", null, "PIX", null, "BRONZE", "LUA");

        CalculoCheckoutException excecao = assertThrows(CalculoCheckoutException.class, () -> service.calcular(request));
        assertEquals("REGIAO_INVALIDA", excecao.getCodigo());
    }

    @Test
    void modalidadeInvalida() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "TELEPORTE", null, "PIX", null, "BRONZE", "SUDESTE");

        CalculoCheckoutException excecao = assertThrows(CalculoCheckoutException.class, () -> service.calcular(request));
        assertEquals("MODALIDADE_INVALIDA", excecao.getCodigo());
    }

    @Test
    void motoboyAcimaDoLimite_devolveModalidadeIndisponivel() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Caixa pesada", "100.00", 1, "6.00")), "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        CalculoCheckoutException excecao = assertThrows(CalculoCheckoutException.class, () -> service.calcular(request));
        assertEquals("MODALIDADE_INDISPONIVEL", excecao.getCodigo());
    }

    @Test
    void cupomInvalido() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "ECONOMICA", "NAOEXISTE", "PIX", null, "BRONZE", "SUDESTE");

        CalculoCheckoutException excecao = assertThrows(CalculoCheckoutException.class, () -> service.calcular(request));
        assertEquals("CUPOM_INVALIDO", excecao.getCodigo());
    }

    @Test
    void cupomNaoAplicavel_menos50AbaixoDoMinimo() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "ECONOMICA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        CalculoCheckoutException excecao = assertThrows(CalculoCheckoutException.class, () -> service.calcular(request));
        assertEquals("CUPOM_NAO_APLICAVEL", excecao.getCodigo());
    }

    @Test
    void formaPagamentoInvalida() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "ECONOMICA", null, "CRIPTO", null, "BRONZE", "SUDESTE");

        CalculoCheckoutException excecao = assertThrows(CalculoCheckoutException.class, () -> service.calcular(request));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", excecao.getCodigo());
    }

    @Test
    void parcelamentoInvalido_pixEmMaisDeUmaVez() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "ECONOMICA", null, "PIX", 2, "BRONZE", "SUDESTE");

        CalculoCheckoutException excecao = assertThrows(CalculoCheckoutException.class, () -> service.calcular(request));
        assertEquals("PARCELAMENTO_INVALIDO", excecao.getCodigo());
    }

    @Test
    void formaPagamentoIndisponivel_boletoAcimaDeMil() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Notebook", "1500.00", 1, "2.00")), "ECONOMICA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        CalculoCheckoutException excecao = assertThrows(CalculoCheckoutException.class, () -> service.calcular(request));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", excecao.getCodigo());
    }

    @Test
    void cupomFreteGratis_descontaExatamenteOFrete() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")),
                "ECONOMICA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE");

        ResumoCheckoutResponse resposta = service.calcular(request);

        assertEquals(new BigDecimal("12.60"), resposta.frete());
        assertEquals(new BigDecimal("12.60"), resposta.descontoCupom());
        assertEquals(new BigDecimal("0.80"), resposta.seguro());
        assertEquals(new BigDecimal("-4.04"), resposta.ajustePagamento());
        assertEquals(new BigDecimal("76.66"), resposta.totalFinal());
    }

    @Test
    void cupomFreteGratis_comOuro_descontoFicaZeroPorqueFreteJaEraZero() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")),
                "EXPRESSA", "FRETEGRATIS", "PIX", null, "OURO", "SUDESTE");

        ResumoCheckoutResponse resposta = service.calcular(request);

        assertEquals(new BigDecimal("0.00"), resposta.frete());
        assertEquals(new BigDecimal("0.00"), resposta.descontoCupom());
        assertEquals(new BigDecimal("4.00"), resposta.creditoProximaCompra());
    }

    @Test
    void ouroComProdutosAcimaDe500_recebeBrinde() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Tênis", "249.90", 3, "1.20")),
                "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE");

        ResumoCheckoutResponse resposta = service.calcular(request);

        assertEquals(new BigDecimal("749.70"), resposta.subtotalProdutos());
        assertEquals(new BigDecimal("37.48"), resposta.creditoProximaCompra());
        assertEquals(true, resposta.brinde());
    }

    @Test
    void itemComPrecoNegativo_devolvePedidoInvalido() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Camiseta", "-10.00", 1, "0.30")), "ECONOMICA", null, "PIX", null, "BRONZE", "SUDESTE");

        CalculoCheckoutException excecao = assertThrows(CalculoCheckoutException.class, () -> service.calcular(request));
        assertEquals("PEDIDO_INVALIDO", excecao.getCodigo());
    }

    @Test
    void itemSemPeso_devolvePedidoInvalido() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(new ItemRequest("Camiseta", new BigDecimal("79.90"), 1, null)),
                "ECONOMICA", null, "PIX", null, "BRONZE", "SUDESTE");

        CalculoCheckoutException excecao = assertThrows(CalculoCheckoutException.class, () -> service.calcular(request));
        assertEquals("PEDIDO_INVALIDO", excecao.getCodigo());
    }

    @Test
    void cartaoComZeroParcelas_devolveParcelamentoInvalido() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "ECONOMICA", null, "CARTAO", 0, "BRONZE", "SUDESTE");

        CalculoCheckoutException excecao = assertThrows(CalculoCheckoutException.class, () -> service.calcular(request));
        assertEquals("PARCELAMENTO_INVALIDO", excecao.getCodigo());
    }

    @Test
    void cartaoComTrezePlusParcelas_devolveParcelamentoInvalido() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest(
                List.of(item("Camiseta", "79.90", 1, "0.30")), "ECONOMICA", null, "CARTAO", 13, "BRONZE", "SUDESTE");

        CalculoCheckoutException excecao = assertThrows(CalculoCheckoutException.class, () -> service.calcular(request));
        assertEquals("PARCELAMENTO_INVALIDO", excecao.getCodigo());
    }
}
