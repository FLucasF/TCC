package com.loja.checkout;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutValidationTest {

    private CheckoutService service;

    @BeforeEach
    void setup() {
        service = new CheckoutService();
    }

    @Test
    void pedidoVazio() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(),
            ModalidadeEntrega.EXPRESSA,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        assertEquals("PEDIDO_INVALIDO", ((CheckoutService.ErrorResponse) resultado).getErro());
    }

    @Test
    void itemComPrecoZero() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", BigDecimal.ZERO, 1, new BigDecimal("0.5"))
            ),
            ModalidadeEntrega.EXPRESSA,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        assertEquals("PEDIDO_INVALIDO", ((CheckoutService.ErrorResponse) resultado).getErro());
    }

    @Test
    void itemComQuantidadeZero() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", new BigDecimal("100"), 0, new BigDecimal("0.5"))
            ),
            ModalidadeEntrega.EXPRESSA,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        assertEquals("PEDIDO_INVALIDO", ((CheckoutService.ErrorResponse) resultado).getErro());
    }

    @Test
    void nivelClubeInvalido() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", new BigDecimal("100"), 1, new BigDecimal("0.5"))
            ),
            ModalidadeEntrega.EXPRESSA,
            null,
            FormaPagamento.PIX,
            1,
            null,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        assertEquals("NIVEL_CLUBE_INVALIDO", ((CheckoutService.ErrorResponse) resultado).getErro());
    }

    @Test
    void regiaoInvalida() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", new BigDecimal("100"), 1, new BigDecimal("0.5"))
            ),
            ModalidadeEntrega.EXPRESSA,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            null
        );

        Object resultado = service.processar(request);
        assertEquals("REGIAO_INVALIDA", ((CheckoutService.ErrorResponse) resultado).getErro());
    }

    @Test
    void modalidadeInvalida() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", new BigDecimal("100"), 1, new BigDecimal("0.5"))
            ),
            null,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        assertEquals("MODALIDADE_INVALIDA", ((CheckoutService.ErrorResponse) resultado).getErro());
    }

    @Test
    void motoboySuperaLimiteKg() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Pesado", new BigDecimal("100"), 10, new BigDecimal("1.0"))
            ),
            ModalidadeEntrega.MOTOBOY,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        assertEquals("MODALIDADE_INDISPONIVEL", ((CheckoutService.ErrorResponse) resultado).getErro());
    }

    @Test
    void cupomInvalido() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", new BigDecimal("100"), 1, new BigDecimal("0.5"))
            ),
            ModalidadeEntrega.EXPRESSA,
            "CUPOM_INEXISTENTE",
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        assertEquals("CUPOM_INVALIDO", ((CheckoutService.ErrorResponse) resultado).getErro());
    }

    @Test
    void cupomMenos50NaoAplicavel() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", new BigDecimal("100"), 1, new BigDecimal("0.5"))
            ),
            ModalidadeEntrega.EXPRESSA,
            "MENOS50",
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        assertEquals("CUPOM_NAO_APLICAVEL", ((CheckoutService.ErrorResponse) resultado).getErro());
    }

    @Test
    void formaPagamentoInvalida() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", new BigDecimal("100"), 1, new BigDecimal("0.5"))
            ),
            ModalidadeEntrega.EXPRESSA,
            null,
            null,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        assertEquals("FORMA_PAGAMENTO_INVALIDA", ((CheckoutService.ErrorResponse) resultado).getErro());
    }

    @Test
    void parcelasInvalidasPixNaoParcelado() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", new BigDecimal("100"), 1, new BigDecimal("0.5"))
            ),
            ModalidadeEntrega.EXPRESSA,
            null,
            FormaPagamento.PIX,
            3,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        assertEquals("PARCELAMENTO_INVALIDO", ((CheckoutService.ErrorResponse) resultado).getErro());
    }

    @Test
    void parcelasInvalidasCartaoAcima12() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", new BigDecimal("100"), 1, new BigDecimal("0.5"))
            ),
            ModalidadeEntrega.EXPRESSA,
            null,
            FormaPagamento.CARTAO,
            13,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        assertEquals("PARCELAMENTO_INVALIDO", ((CheckoutService.ErrorResponse) resultado).getErro());
    }

    @Test
    void boleto1001Reais() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Caro", new BigDecimal("1001.00"), 1, new BigDecimal("1.0"))
            ),
            ModalidadeEntrega.RETIRADA_LOJA,
            null,
            FormaPagamento.BOLETO,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ((CheckoutService.ErrorResponse) resultado).getErro());
    }

    @Test
    void motoboyExatamente5Kg() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", new BigDecimal("100"), 5, new BigDecimal("1.0"))
            ),
            ModalidadeEntrega.MOTOBOY,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        assertTrue(resultado instanceof com.loja.checkout.dto.CheckoutResponse);
    }

    @Test
    void cupomMenos50Aplicavel() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Caro", new BigDecimal("300.00"), 1, new BigDecimal("1.0"))
            ),
            ModalidadeEntrega.RETIRADA_LOJA,
            "MENOS50",
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        assertTrue(resultado instanceof com.loja.checkout.dto.CheckoutResponse);
    }
}
