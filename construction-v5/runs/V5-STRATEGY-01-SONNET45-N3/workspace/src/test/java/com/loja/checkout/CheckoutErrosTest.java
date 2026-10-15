package com.loja.checkout;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.service.CalculadoraResumo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CheckoutErrosTest {

    @Autowired
    private CalculadoraResumo calculadora;

    @Test
    void deveFalharComCarrinhoVazio() {
        PedidoRequest request = new PedidoRequest(
            List.of(),
            "ECONOMICA",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class, () -> calculadora.calcular(request));
        assertEquals(CodigoErro.PEDIDO_INVALIDO, ex.getCodigo());
    }

    @Test
    void deveFalharComNivelClubeInvalido() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemRequest("Item", new BigDecimal("100.00"), 1, new BigDecimal("1.00"))),
            "ECONOMICA",
            null,
            "PIX",
            null,
            "PLATINA",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class, () -> calculadora.calcular(request));
        assertEquals(CodigoErro.NIVEL_CLUBE_INVALIDO, ex.getCodigo());
    }

    @Test
    void deveFalharComRegiaoInvalida() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemRequest("Item", new BigDecimal("100.00"), 1, new BigDecimal("1.00"))),
            "ECONOMICA",
            null,
            "PIX",
            null,
            "BRONZE",
            "EXTERIOR"
        );

        CheckoutException ex = assertThrows(CheckoutException.class, () -> calculadora.calcular(request));
        assertEquals(CodigoErro.REGIAO_INVALIDA, ex.getCodigo());
    }

    @Test
    void deveFalharComModalidadeInvalida() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemRequest("Item", new BigDecimal("100.00"), 1, new BigDecimal("1.00"))),
            "DRONE",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class, () -> calculadora.calcular(request));
        assertEquals(CodigoErro.MODALIDADE_INVALIDA, ex.getCodigo());
    }

    @Test
    void deveFalharComMotoboyAcimaDe5Kg() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemRequest("Item pesado", new BigDecimal("100.00"), 2, new BigDecimal("3.00"))),
            "MOTOBOY",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class, () -> calculadora.calcular(request));
        assertEquals(CodigoErro.MODALIDADE_INDISPONIVEL, ex.getCodigo());
    }

    @Test
    void deveFalharComCupomInvalido() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemRequest("Item", new BigDecimal("100.00"), 1, new BigDecimal("1.00"))),
            "ECONOMICA",
            "INEXISTENTE",
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class, () -> calculadora.calcular(request));
        assertEquals(CodigoErro.CUPOM_INVALIDO, ex.getCodigo());
    }

    @Test
    void deveFalharComMenos50AbaixoDoMinimo() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemRequest("Item", new BigDecimal("100.00"), 1, new BigDecimal("1.00"))),
            "ECONOMICA",
            "MENOS50",
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class, () -> calculadora.calcular(request));
        assertEquals(CodigoErro.CUPOM_NAO_APLICAVEL, ex.getCodigo());
    }

    @Test
    void deveFalharComFormaPagamentoInvalida() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemRequest("Item", new BigDecimal("100.00"), 1, new BigDecimal("1.00"))),
            "ECONOMICA",
            null,
            "CHEQUE",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class, () -> calculadora.calcular(request));
        assertEquals(CodigoErro.FORMA_PAGAMENTO_INVALIDA, ex.getCodigo());
    }

    @Test
    void deveFalharComPixParcelado() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemRequest("Item", new BigDecimal("100.00"), 1, new BigDecimal("1.00"))),
            "ECONOMICA",
            null,
            "PIX",
            2,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class, () -> calculadora.calcular(request));
        assertEquals(CodigoErro.PARCELAMENTO_INVALIDO, ex.getCodigo());
    }

    @Test
    void deveFalharComBoletoAcimaDe1000() {
        PedidoRequest request = new PedidoRequest(
            List.of(new ItemRequest("Item caro", new BigDecimal("1500.00"), 1, new BigDecimal("1.00"))),
            "ECONOMICA",
            null,
            "BOLETO",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException ex = assertThrows(CheckoutException.class, () -> calculadora.calcular(request));
        assertEquals(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL, ex.getCodigo());
    }
}
