package com.loja;

import com.loja.model.CheckoutRequest;
import com.loja.model.ItemCarrinho;
import com.loja.service.CheckoutService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CheckoutServiceValidacaoTest {

    private final CheckoutService service = new CheckoutService();

    @Test
    void deveRejeitarCarrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest(
            Collections.emptyList(),
            "ECONOMICA",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutService.CheckoutException ex = assertThrows(
            CheckoutService.CheckoutException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("PEDIDO_INVALIDO", ex.getMessage());
    }

    @Test
    void deveRejeitarItemComPrecoInvalido() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(new ItemCarrinho("Produto", BigDecimal.ZERO, 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutService.CheckoutException ex = assertThrows(
            CheckoutService.CheckoutException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("PEDIDO_INVALIDO", ex.getMessage());
    }

    @Test
    void deveRejeitarNivelClubeInvalido() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(new ItemCarrinho("Produto", new BigDecimal("100"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            null,
            "PIX",
            1,
            "DIAMANTE",
            "SUDESTE"
        );

        CheckoutService.CheckoutException ex = assertThrows(
            CheckoutService.CheckoutException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("NIVEL_CLUBE_INVALIDO", ex.getMessage());
    }

    @Test
    void deveRejeitarRegiaoInvalida() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(new ItemCarrinho("Produto", new BigDecimal("100"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            null,
            "PIX",
            1,
            "BRONZE",
            "CARIBE"
        );

        CheckoutService.CheckoutException ex = assertThrows(
            CheckoutService.CheckoutException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("REGIAO_INVALIDA", ex.getMessage());
    }

    @Test
    void deveRejeitarModalidadeInvalida() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(new ItemCarrinho("Produto", new BigDecimal("100"), 1, new BigDecimal("1.0"))),
            "DRONE",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutService.CheckoutException ex = assertThrows(
            CheckoutService.CheckoutException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("MODALIDADE_INVALIDA", ex.getMessage());
    }

    @Test
    void deveRejeitarMotoboyAcimaDe5kg() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(new ItemCarrinho("Produto Pesado", new BigDecimal("100"), 1, new BigDecimal("6.0"))),
            "MOTOBOY",
            null,
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutService.CheckoutException ex = assertThrows(
            CheckoutService.CheckoutException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("MODALIDADE_INDISPONIVEL", ex.getMessage());
    }

    @Test
    void deveRejeitarCupomInvalido() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(new ItemCarrinho("Produto", new BigDecimal("100"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            "CUPOMINEXISTENTE",
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutService.CheckoutException ex = assertThrows(
            CheckoutService.CheckoutException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("CUPOM_INVALIDO", ex.getMessage());
    }

    @Test
    void deveRejeitarCupomNaoAplicavel() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(new ItemCarrinho("Produto", new BigDecimal("100"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            "MENOS50",
            "PIX",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutService.CheckoutException ex = assertThrows(
            CheckoutService.CheckoutException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("CUPOM_NAO_APLICAVEL", ex.getMessage());
    }

    @Test
    void deveRejeitarFormaPagamentoInvalida() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(new ItemCarrinho("Produto", new BigDecimal("100"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            null,
            "CRIPTO",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutService.CheckoutException ex = assertThrows(
            CheckoutService.CheckoutException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("FORMA_PAGAMENTO_INVALIDA", ex.getMessage());
    }

    @Test
    void deveRejeitarParcelamentoInvalido() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(new ItemCarrinho("Produto", new BigDecimal("100"), 1, new BigDecimal("1.0"))),
            "ECONOMICA",
            null,
            "PIX",
            3,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutService.CheckoutException ex = assertThrows(
            CheckoutService.CheckoutException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("PARCELAMENTO_INVALIDO", ex.getMessage());
    }

    @Test
    void deveRejeitarBoletoAcimaDe1000() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(new ItemCarrinho("Produto Caro", new BigDecimal("1500"), 1, new BigDecimal("1.0"))),
            "RETIRADA_LOJA",
            null,
            "BOLETO",
            1,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutService.CheckoutException ex = assertThrows(
            CheckoutService.CheckoutException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ex.getMessage());
    }
}
