package com.loja.checkout;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
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

class CheckoutSpecialCasesTest {

    private CheckoutService service;

    @BeforeEach
    void setup() {
        service = new CheckoutService();
    }

    @Test
    void ouroComBrinde() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Premium", new BigDecimal("600.00"), 1, new BigDecimal("1.0"))
            ),
            ModalidadeEntrega.RETIRADA_LOJA,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.OURO,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        CheckoutResponse response = (CheckoutResponse) resultado;
        assertTrue(response.brinde());
    }

    @Test
    void ouroSemBrinde() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Normal", new BigDecimal("400.00"), 1, new BigDecimal("1.0"))
            ),
            ModalidadeEntrega.RETIRADA_LOJA,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.OURO,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        CheckoutResponse response = (CheckoutResponse) resultado;
        assertFalse(response.brinde());
    }

    @Test
    void ouroFretGratis() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", new BigDecimal("100.00"), 2, new BigDecimal("2.0"))
            ),
            ModalidadeEntrega.EXPRESSA,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.OURO,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        CheckoutResponse response = (CheckoutResponse) resultado;
        assertEquals(new BigDecimal("0.00"), response.frete());
    }

    @Test
    void prataCreditoCalculado() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))
            ),
            ModalidadeEntrega.RETIRADA_LOJA,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.PRATA,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        CheckoutResponse response = (CheckoutResponse) resultado;
        assertEquals(new BigDecimal("2.00"), response.creditoProximaCompra());
    }

    @Test
    void freteGratisCupom() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", new BigDecimal("100.00"), 1, new BigDecimal("1.0"))
            ),
            ModalidadeEntrega.EXPRESSA,
            "FRETEGRATIS",
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        CheckoutResponse response = (CheckoutResponse) resultado;
        assertEquals(new BigDecimal("0.00"), response.frete());
        assertEquals(new BigDecimal("29.50"), response.descontoCupom());
    }

    @Test
    void cartaoJuros4Parcelas() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))
            ),
            ModalidadeEntrega.RETIRADA_LOJA,
            null,
            FormaPagamento.CARTAO,
            4,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        CheckoutResponse response = (CheckoutResponse) resultado;
        assertEquals(4, response.parcelas());
        assertTrue(response.ajustePagamento().signum() > 0);
    }

    @Test
    void pixDesconto5Porcento() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", new BigDecimal("200.00"), 1, new BigDecimal("0.5"))
            ),
            ModalidadeEntrega.RETIRADA_LOJA,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        CheckoutResponse response = (CheckoutResponse) resultado;
        assertEquals(new BigDecimal("-10.10"), response.ajustePagamento());
    }

    @Test
    void boletoTarifa349() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", new BigDecimal("500.00"), 1, new BigDecimal("0.5"))
            ),
            ModalidadeEntrega.RETIRADA_LOJA,
            null,
            FormaPagamento.BOLETO,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        CheckoutResponse response = (CheckoutResponse) resultado;
        assertEquals(new BigDecimal("3.49"), response.ajustePagamento());
    }

    @Test
    void leve3Pague2MultiplosItens() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Item1", new BigDecimal("10.00"), 3, new BigDecimal("0.1")),
                new ItemRequest("Item2", new BigDecimal("20.00"), 6, new BigDecimal("0.2"))
            ),
            ModalidadeEntrega.RETIRADA_LOJA,
            "LEVE3PAGUE2",
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        CheckoutResponse response = (CheckoutResponse) resultado;
        BigDecimal esperado = new BigDecimal("10.00").add(new BigDecimal("40.00"));
        assertEquals(esperado, response.descontoCupom());
    }

    @Test
    void seguroNorte() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", new BigDecimal("1000.00"), 1, new BigDecimal("1.0"))
            ),
            ModalidadeEntrega.RETIRADA_LOJA,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.NORTE
        );

        Object resultado = service.processar(request);
        CheckoutResponse response = (CheckoutResponse) resultado;
        assertEquals(new BigDecimal("25.00"), response.seguro());
    }

    @Test
    void seguroCentroOeste() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", new BigDecimal("1000.00"), 1, new BigDecimal("1.0"))
            ),
            ModalidadeEntrega.RETIRADA_LOJA,
            null,
            FormaPagamento.PIX,
            1,
            NivelClube.BRONZE,
            Regiao.CENTRO_OESTE
        );

        Object resultado = service.processar(request);
        CheckoutResponse response = (CheckoutResponse) resultado;
        assertEquals(new BigDecimal("15.00"), response.seguro());
    }

    @Test
    void cartaoSemJuros3Parcelas() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Teste", new BigDecimal("300.00"), 1, new BigDecimal("0.5"))
            ),
            ModalidadeEntrega.RETIRADA_LOJA,
            null,
            FormaPagamento.CARTAO,
            3,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        CheckoutResponse response = (CheckoutResponse) resultado;
        assertEquals(new BigDecimal("0.00"), response.ajustePagamento());
        assertEquals(3, response.parcelas());
        assertEquals(new BigDecimal("101.00"), response.valorParcela());
    }

    @Test
    void boletoNaoAcimaDE1000() {
        CheckoutRequest request = new CheckoutRequest(
            List.of(
                new ItemRequest("Caro", new BigDecimal("1000.00"), 1, new BigDecimal("1.0"))
            ),
            ModalidadeEntrega.RETIRADA_LOJA,
            null,
            FormaPagamento.BOLETO,
            1,
            NivelClube.BRONZE,
            Regiao.SUDESTE
        );

        Object resultado = service.processar(request);
        assertTrue(resultado instanceof CheckoutResponse);
    }
}
