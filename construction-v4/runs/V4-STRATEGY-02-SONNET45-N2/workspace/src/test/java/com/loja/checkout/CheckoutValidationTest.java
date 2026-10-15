package com.loja.checkout;

import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CheckoutValidationTest {

    @Autowired
    private CheckoutService checkoutService;

    @Test
    void deveRejeitarCarrinhoVazio() {
        PedidoRequest request = new PedidoRequest(
            Collections.emptyList(),
            "ECONOMICA",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    void deveRejeitarItemComPrecoZero() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemCarrinho("Item", BigDecimal.ZERO, 1, new BigDecimal("0.5"))),
            "ECONOMICA",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    void deveRejeitarNivelClubeInvalido() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemCarrinho("Item", new BigDecimal("100"), 1, new BigDecimal("0.5"))),
            "ECONOMICA",
            null,
            "PIX",
            null,
            "PLATINA",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("NIVEL_CLUBE_INVALIDO", exception.getCodigo());
    }

    @Test
    void deveRejeitarRegiaoInvalida() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemCarrinho("Item", new BigDecimal("100"), 1, new BigDecimal("0.5"))),
            "ECONOMICA",
            null,
            "PIX",
            null,
            "BRONZE",
            "INTERNACIONAL"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("REGIAO_INVALIDA", exception.getCodigo());
    }

    @Test
    void deveRejeitarModalidadeInvalida() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemCarrinho("Item", new BigDecimal("100"), 1, new BigDecimal("0.5"))),
            "DRONE",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("MODALIDADE_INVALIDA", exception.getCodigo());
    }

    @Test
    void deveRejeitarMotoboyAcimaDe5Kg() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemCarrinho("Item Pesado", new BigDecimal("100"), 1, new BigDecimal("6"))),
            "MOTOBOY",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigo());
    }

    @Test
    void deveRejeitarCupomInvalido() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemCarrinho("Item", new BigDecimal("100"), 1, new BigDecimal("0.5"))),
            "ECONOMICA",
            "CUPOMINEXISTENTE",
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("CUPOM_INVALIDO", exception.getCodigo());
    }

    @Test
    void deveRejeitarCupomMenos50AbaixoDoMinimo() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemCarrinho("Item", new BigDecimal("100"), 1, new BigDecimal("0.5"))),
            "ECONOMICA",
            "MENOS50",
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigo());
    }

    @Test
    void deveRejeitarFormaPagamentoInvalida() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemCarrinho("Item", new BigDecimal("100"), 1, new BigDecimal("0.5"))),
            "ECONOMICA",
            null,
            "CRIPTOMOEDA",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigo());
    }

    @Test
    void deveRejeitarParcelamentoInvalidoParaPix() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemCarrinho("Item", new BigDecimal("100"), 1, new BigDecimal("0.5"))),
            "ECONOMICA",
            null,
            "PIX",
            3,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigo());
    }

    @Test
    void deveRejeitarBoletoAcimaDe1000() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemCarrinho("Item Caro", new BigDecimal("1500"), 1, new BigDecimal("0.5"))),
            "ECONOMICA",
            null,
            "BOLETO",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigo());
    }
}
