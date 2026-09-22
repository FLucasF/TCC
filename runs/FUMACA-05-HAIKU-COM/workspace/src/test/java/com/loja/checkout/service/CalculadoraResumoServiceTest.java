package com.loja.checkout.service;

import com.loja.checkout.dto.ItemCarrinhoRequest;
import com.loja.checkout.dto.ResumoCheckoutRequest;
import com.loja.checkout.dto.ResumoCheckoutResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraResumoServiceTest {

    private CalculadoraResumoService service;

    @BeforeEach
    void setup() {
        service = new CalculadoraResumoService();
    }

    @Test
    void teste1_CamisetaTenisExpressaBemvindoPix() throws ValidacaoException {
        var camiseta = new ItemCarrinhoRequest("Camiseta", 79.90, 2, 0.30);
        var tenis = new ItemCarrinhoRequest("Tênis", 249.90, 1, 1.20);
        var request = new ResumoCheckoutRequest(
            Arrays.asList(camiseta, tenis),
            "EXPRESSA",
            "BEMVINDO10",
            "PIX",
            1
        );

        ResumoCheckoutResponse response = service.calcular(request);

        assertEquals(409.70, response.getSubtotalProdutos(), 0.01);
        assertEquals(40.97, response.getDescontoCupom(), 0.01);
        assertEquals(33.10, response.getFrete(), 0.01);
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(-20.09, response.getAjustePagamento(), 0.01);
        assertEquals(381.74, response.getTotalFinal(), 0.01);
        assertEquals(1, response.getParcelas());
        assertEquals(381.74, response.getValorParcela(), 0.01);
    }

    @Test
    void teste2_CamisetaTenisEconomicaSemCupomCartao6x() throws ValidacaoException {
        var camiseta = new ItemCarrinhoRequest("Camiseta", 79.90, 2, 0.30);
        var tenis = new ItemCarrinhoRequest("Tênis", 249.90, 1, 1.20);
        var request = new ResumoCheckoutRequest(
            Arrays.asList(camiseta, tenis),
            "ECONOMICA",
            null,
            "CARTAO",
            6
        );

        ResumoCheckoutResponse response = service.calcular(request);

        assertEquals(409.70, response.getSubtotalProdutos(), 0.01);
        assertEquals(0.00, response.getDescontoCupom(), 0.01);
        assertEquals(15.60, response.getFrete(), 0.01);
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(30.10, response.getAjustePagamento(), 0.01);
        assertEquals(455.40, response.getTotalFinal(), 0.01);
        assertEquals(6, response.getParcelas());
        assertEquals(75.90, response.getValorParcela(), 0.01);
    }

    @Test
    void teste3_FoneMotoboyMenos50Boleto() throws ValidacaoException {
        var fone = new ItemCarrinhoRequest("Fone", 199.90, 2, 0.25);
        var request = new ResumoCheckoutRequest(
            Collections.singletonList(fone),
            "MOTOBOY",
            "MENOS50",
            "BOLETO",
            1
        );

        ResumoCheckoutResponse response = service.calcular(request);

        assertEquals(399.80, response.getSubtotalProdutos(), 0.01);
        assertEquals(50.00, response.getDescontoCupom(), 0.01);
        assertEquals(18.00, response.getFrete(), 0.01);
        assertEquals(0, response.getPrazoEntregaDias());
        assertEquals(3.49, response.getAjustePagamento(), 0.01);
        assertEquals(371.29, response.getTotalFinal(), 0.01);
        assertEquals(1, response.getParcelas());
        assertEquals(371.29, response.getValorParcela(), 0.01);
    }

    @Test
    void teste4_MeiaCamisetaRetiradaLojaLeve3Pague2Cartao3x() throws ValidacaoException {
        var meia = new ItemCarrinhoRequest("Meia", 19.90, 7, 0.10);
        var camiseta = new ItemCarrinhoRequest("Camiseta", 79.90, 2, 0.30);
        var request = new ResumoCheckoutRequest(
            Arrays.asList(meia, camiseta),
            "RETIRADA_LOJA",
            "LEVE3PAGUE2",
            "CARTAO",
            3
        );

        ResumoCheckoutResponse response = service.calcular(request);

        assertEquals(299.10, response.getSubtotalProdutos(), 0.01);
        assertEquals(39.80, response.getDescontoCupom(), 0.01);
        assertEquals(0.00, response.getFrete(), 0.01);
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(0.00, response.getAjustePagamento(), 0.01);
        assertEquals(259.30, response.getTotalFinal(), 0.01);
        assertEquals(3, response.getParcelas());
        assertEquals(86.43, response.getValorParcela(), 0.01);
    }

    @Test
    void validar_CarrinhVazio() {
        var request = new ResumoCheckoutRequest(
            Collections.emptyList(),
            "EXPRESSA",
            null,
            "PIX",
            1
        );

        ValidacaoException exception = assertThrows(ValidacaoException.class, () -> {
            service.calcular(request);
        });

        assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void validar_ModalidadeInvalida() {
        var camiseta = new ItemCarrinhoRequest("Camiseta", 79.90, 2, 0.30);
        var request = new ResumoCheckoutRequest(
            Collections.singletonList(camiseta),
            "INVALIDA",
            null,
            "PIX",
            1
        );

        ValidacaoException exception = assertThrows(ValidacaoException.class, () -> {
            service.calcular(request);
        });

        assertEquals("MODALIDADE_INVALIDA", exception.getCodigoErro());
    }

    @Test
    void validar_ModalidadeIndisponivel_MotoboySuperior5kg() {
        var item = new ItemCarrinhoRequest("Item", 100.00, 1, 6.0);
        var request = new ResumoCheckoutRequest(
            Collections.singletonList(item),
            "MOTOBOY",
            null,
            "PIX",
            1
        );

        ValidacaoException exception = assertThrows(ValidacaoException.class, () -> {
            service.calcular(request);
        });

        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigoErro());
    }

    @Test
    void validar_CupomInvalido() {
        var camiseta = new ItemCarrinhoRequest("Camiseta", 79.90, 2, 0.30);
        var request = new ResumoCheckoutRequest(
            Collections.singletonList(camiseta),
            "EXPRESSA",
            "CUPOMINVALIDO",
            "PIX",
            1
        );

        ValidacaoException exception = assertThrows(ValidacaoException.class, () -> {
            service.calcular(request);
        });

        assertEquals("CUPOM_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void validar_CupomNaoAplicavel_Menos50AbaixoDe300() {
        var camiseta = new ItemCarrinhoRequest("Camiseta", 79.90, 2, 0.30);
        var request = new ResumoCheckoutRequest(
            Collections.singletonList(camiseta),
            "EXPRESSA",
            "MENOS50",
            "PIX",
            1
        );

        ValidacaoException exception = assertThrows(ValidacaoException.class, () -> {
            service.calcular(request);
        });

        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigoErro());
    }

    @Test
    void validar_FormaPagamentoInvalida() {
        var camiseta = new ItemCarrinhoRequest("Camiseta", 79.90, 2, 0.30);
        var request = new ResumoCheckoutRequest(
            Collections.singletonList(camiseta),
            "EXPRESSA",
            null,
            "INVALIDA",
            1
        );

        ValidacaoException exception = assertThrows(ValidacaoException.class, () -> {
            service.calcular(request);
        });

        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigoErro());
    }

    @Test
    void validar_ParcelamentoInvalido_PixComParcelas() {
        var camiseta = new ItemCarrinhoRequest("Camiseta", 79.90, 2, 0.30);
        var request = new ResumoCheckoutRequest(
            Collections.singletonList(camiseta),
            "EXPRESSA",
            null,
            "PIX",
            2
        );

        ValidacaoException exception = assertThrows(ValidacaoException.class, () -> {
            service.calcular(request);
        });

        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void validar_FormaPagamentoIndisponivel_BoletoSuperior1000() {
        var item = new ItemCarrinhoRequest("Item", 500.00, 3, 0.10);
        var request = new ResumoCheckoutRequest(
            Collections.singletonList(item),
            "RETIRADA_LOJA",
            null,
            "BOLETO",
            1
        );

        ValidacaoException exception = assertThrows(ValidacaoException.class, () -> {
            service.calcular(request);
        });

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigoErro());
    }

}
