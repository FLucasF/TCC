package com.loja.checkout;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.model.ItemCarrinho;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ValidacaoTest {

    @Autowired
    private CheckoutService service;

    @Test
    void deveFalharQuandoCarrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest(
            Collections.emptyList(),
            "ECONOMICA",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals(CodigoErro.PEDIDO_INVALIDO, exception.getCodigo());
    }

    @Test
    void deveFalharQuandoItemComPrecoZero() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto", BigDecimal.ZERO, 1, new BigDecimal("1.0"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "ECONOMICA",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals(CodigoErro.PEDIDO_INVALIDO, exception.getCodigo());
    }

    @Test
    void deveFalharQuandoNivelClubeInvalido() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "ECONOMICA",
            null,
            "PIX",
            null,
            "PLATINA",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals(CodigoErro.NIVEL_CLUBE_INVALIDO, exception.getCodigo());
    }

    @Test
    void deveFalharQuandoRegiaoInvalida() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "ECONOMICA",
            null,
            "PIX",
            null,
            "BRONZE",
            "AUSTRALIA"
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals(CodigoErro.REGIAO_INVALIDA, exception.getCodigo());
    }

    @Test
    void deveFalharQuandoModalidadeInvalida() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "DRONE",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals(CodigoErro.MODALIDADE_INVALIDA, exception.getCodigo());
    }

    @Test
    void deveFalharQuandoMotoboyAcimaDe5Kg() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto Pesado", new BigDecimal("100.00"), 1, new BigDecimal("6.0"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "MOTOBOY",
            null,
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals(CodigoErro.MODALIDADE_INDISPONIVEL, exception.getCodigo());
    }

    @Test
    void deveFalharQuandoCupomInvalido() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "ECONOMICA",
            "CUPOMFAKE",
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals(CodigoErro.CUPOM_INVALIDO, exception.getCodigo());
    }

    @Test
    void deveFalharQuandoMenos50AbaixoDe300() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "ECONOMICA",
            "MENOS50",
            "PIX",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals(CodigoErro.CUPOM_NAO_APLICAVEL, exception.getCodigo());
    }

    @Test
    void deveFalharQuandoFormaPagamentoInvalida() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "ECONOMICA",
            null,
            "CRIPTO",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals(CodigoErro.FORMA_PAGAMENTO_INVALIDA, exception.getCodigo());
    }

    @Test
    void deveFalharQuandoPixParcela2Vezes() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "ECONOMICA",
            null,
            "PIX",
            2,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals(CodigoErro.PARCELAMENTO_INVALIDO, exception.getCodigo());
    }

    @Test
    void deveFalharQuandoBoletoAcimaDe1000() {
        List<ItemCarrinho> itens = List.of(
            new ItemCarrinho("Produto Caro", new BigDecimal("1500.00"), 1, new BigDecimal("1.0"))
        );

        CheckoutRequest request = new CheckoutRequest(
            itens,
            "ECONOMICA",
            null,
            "BOLETO",
            null,
            "BRONZE",
            "SUDESTE"
        );

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(request));
        assertEquals(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL, exception.getCodigo());
    }
}
