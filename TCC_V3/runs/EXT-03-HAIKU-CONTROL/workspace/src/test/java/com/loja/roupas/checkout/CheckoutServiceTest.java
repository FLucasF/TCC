package com.loja.roupas.checkout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class CheckoutServiceTest {
    private CheckoutService service;

    @BeforeEach
    public void setUp() {
        service = new CheckoutService();
    }

    @Test
    public void exemplo1() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemRequest("Camiseta", 79.90, 2, 0.30),
            new ItemRequest("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);
        request.setNivelClube("BRONZE");
        request.setRegiao(null);

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), response.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), response.getFrete());
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("-20.09"), response.getAjustePagamento());
        assertEquals(new BigDecimal("381.74"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("381.74"), response.getValorParcela());
    }

    @Test
    public void exemplo2() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemRequest("Camiseta", 79.90, 2, 0.30),
            new ItemRequest("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom(null);
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);
        request.setNivelClube("BRONZE");
        request.setRegiao(null);

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("30.10"), response.getAjustePagamento());
        assertEquals(new BigDecimal("455.40"), response.getTotalFinal());
        assertEquals(6, response.getParcelas());
        assertEquals(new BigDecimal("75.90"), response.getValorParcela());
    }

    @Test
    public void exemplo3() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new ItemRequest("Fone", 199.90, 2, 0.25)
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);
        request.setNivelClube("BRONZE");
        request.setRegiao(null);

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("399.80"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), response.getFrete());
        assertEquals(0, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("3.49"), response.getAjustePagamento());
        assertEquals(new BigDecimal("371.29"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("371.29"), response.getValorParcela());
    }

    @Test
    public void exemplo4() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemRequest("Meia", 19.90, 7, 0.10),
            new ItemRequest("Camiseta", 79.90, 2, 0.30)
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);
        request.setNivelClube("BRONZE");
        request.setRegiao(null);

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("299.10"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.getDescontoCupom());
        assertTrue(response.getFrete().compareTo(BigDecimal.ZERO) == 0);
        assertEquals(1, response.getPrazoEntregaDias());
        assertTrue(response.getAjustePagamento().compareTo(BigDecimal.ZERO) == 0);
        assertEquals(new BigDecimal("259.30"), response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(new BigDecimal("86.43"), response.getValorParcela());
    }

    @Test
    public void exemplo5() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemRequest("Camiseta", 79.90, 2, 0.30),
            new ItemRequest("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom(null);
        request.setFormaPagamento("PIX");
        request.setParcelas(1);
        request.setNivelClube("OURO");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertTrue(response.getDescontoCupom().compareTo(BigDecimal.ZERO) == 0);
        assertTrue(response.getFrete().compareTo(BigDecimal.ZERO) == 0);
        assertEquals(2, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("49.16"), response.getImposto());
        assertEquals(new BigDecimal("-22.94"), response.getAjustePagamento());
        assertEquals(new BigDecimal("435.92"), response.getTotalFinal());
        assertEquals(1, response.getParcelas());
        assertEquals(new BigDecimal("435.92"), response.getValorParcela());
        assertEquals(new BigDecimal("20.48"), response.getCreditoProximaCompra());
        assertFalse(response.getBrinde());
    }

    @Test
    public void validacaoPedidoInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.emptyList());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        assertThrows(CheckoutService.ValidationException.class, () -> {
            service.calcularResumo(request);
        });
    }

    @Test
    public void validacaoPrecoNegativo() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new ItemRequest("Teste", -10.0, 1, 0.5)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutService.ValidationException ex = assertThrows(CheckoutService.ValidationException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("PEDIDO_INVALIDO", ex.getCodigoErro());
    }

    @Test
    public void validacaoNivelClubeInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new ItemRequest("Teste", 10.0, 1, 0.5)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("INVALIDO");
        request.setRegiao("SUDESTE");

        CheckoutService.ValidationException ex = assertThrows(CheckoutService.ValidationException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("NIVEL_CLUBE_INVALIDO", ex.getCodigoErro());
    }

    @Test
    public void validacaoRegiaoInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new ItemRequest("Teste", 10.0, 1, 0.5)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("INVALIDA");

        CheckoutService.ValidationException ex = assertThrows(CheckoutService.ValidationException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("REGIAO_INVALIDA", ex.getCodigoErro());
    }

    @Test
    public void validacaoModalidadeInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new ItemRequest("Teste", 10.0, 1, 0.5)
        ));
        request.setModalidadeEntrega("INVALIDA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutService.ValidationException ex = assertThrows(CheckoutService.ValidationException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("MODALIDADE_INVALIDA", ex.getCodigoErro());
    }

    @Test
    public void validacaoMotoboySobreWeight() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new ItemRequest("Teste", 10.0, 1, 6.0)
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutService.ValidationException ex = assertThrows(CheckoutService.ValidationException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("MODALIDADE_INDISPONIVEL", ex.getCodigoErro());
    }

    @Test
    public void validacaoCupomInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new ItemRequest("Teste", 10.0, 1, 0.5)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("INVALIDO");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutService.ValidationException ex = assertThrows(CheckoutService.ValidationException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("CUPOM_INVALIDO", ex.getCodigoErro());
    }

    @Test
    public void validacaoCupomNaoAplicavel() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new ItemRequest("Teste", 100.0, 1, 0.5)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutService.ValidationException ex = assertThrows(CheckoutService.ValidationException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("CUPOM_NAO_APLICAVEL", ex.getCodigoErro());
    }

    @Test
    public void validacaoFormaPagamentoInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new ItemRequest("Teste", 10.0, 1, 0.5)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("INVALIDA");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutService.ValidationException ex = assertThrows(CheckoutService.ValidationException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("FORMA_PAGAMENTO_INVALIDA", ex.getCodigoErro());
    }

    @Test
    public void validacaoParcelamentoInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new ItemRequest("Teste", 10.0, 1, 0.5)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(2);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutService.ValidationException ex = assertThrows(CheckoutService.ValidationException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("PARCELAMENTO_INVALIDO", ex.getCodigoErro());
    }

    @Test
    public void validacaoBoletoAcimaDeLimit() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new ItemRequest("Teste", 1500.0, 1, 0.5)
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutService.ValidationException ex = assertThrows(CheckoutService.ValidationException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ex.getCodigoErro());
    }

    @Test
    public void testeFreteGratisCupom() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new ItemRequest("Camiseta", 79.90, 2, 0.30),
            new ItemRequest("Tênis", 249.90, 1, 1.20)
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("FRETEGRATIS");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("33.10"), response.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), response.getFrete());
    }

    @Test
    public void testeOuroComBrinde() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new ItemRequest("Produto", 600.0, 1, 1.0)
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom(null);
        request.setFormaPagamento("PIX");
        request.setParcelas(1);
        request.setNivelClube("OURO");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = service.calcularResumo(request);

        assertTrue(response.getBrinde());
        assertEquals(new BigDecimal("30.00"), response.getCreditoProximaCompra());
    }

    @Test
    public void testeParcelaPrata() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.singletonList(
            new ItemRequest("Produto", 100.0, 1, 0.5)
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom(null);
        request.setFormaPagamento("PIX");
        request.setParcelas(1);
        request.setNivelClube("PRATA");
        request.setRegiao("SUDESTE");

        CheckoutResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("2.00"), response.getCreditoProximaCompra());
    }
}
