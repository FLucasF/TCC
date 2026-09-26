package com.loja.service;

import com.loja.api.ResumoRequest;
import com.loja.api.ResumoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {
    private CheckoutService service;

    @BeforeEach
    void setUp() {
        service = new CheckoutService();
    }

    @Test
    void exemplo1_CamisetaTenisExpressaBemvindoPix() throws CheckoutService.CheckoutException {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new ResumoRequest.ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ResumoRequest.ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);

        ResumoResponse response = service.calcularResumo(request);

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
    void exemplo2_CamisetaTenisEconomicaSemCupomCartao6x() throws CheckoutService.CheckoutException {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new ResumoRequest.ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ResumoRequest.ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom(null);
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);

        ResumoResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("30.10"), response.getAjustePagamento());
        assertEquals(new BigDecimal("455.40"), response.getTotalFinal());
        assertEquals(6, response.getParcelas());
        assertTrue(response.getValorParcela().compareTo(new BigDecimal("75.89")) >= 0 &&
                   response.getValorParcela().compareTo(new BigDecimal("75.91")) <= 0);
    }

    @Test
    void exemplo3_FoneMotoboys_Menos50_Boleto() throws CheckoutService.CheckoutException {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new ResumoRequest.ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);

        ResumoResponse response = service.calcularResumo(request);

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
    void exemplo4_MeiasCamisetasRetiradaLoja_Leve3Pague2_Cartao3x() throws CheckoutService.CheckoutException {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new ResumoRequest.ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ResumoRequest.ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);

        ResumoResponse response = service.calcularResumo(request);

        assertEquals(new BigDecimal("299.10"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), response.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), response.getFrete());
        assertEquals(1, response.getPrazoEntregaDias());
        assertEquals(new BigDecimal("0.00"), response.getAjustePagamento());
        assertEquals(new BigDecimal("259.30"), response.getTotalFinal());
        assertEquals(3, response.getParcelas());
        assertEquals(new BigDecimal("86.43"), response.getValorParcela());
    }

    @Test
    void validacao_PedidoInvalido_CarrinhoVazio() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        assertThrows(CheckoutService.CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
    }

    @Test
    void validacao_PedidoInvalido_PrecoNegativo() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new ResumoRequest.ItemRequest("Camiseta", new BigDecimal("-10.00"), 1, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        CheckoutService.CheckoutException exception = assertThrows(CheckoutService.CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("PEDIDO_INVALIDO", exception.getCodigo());
    }

    @Test
    void validacao_ModalidadeInvalida() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new ResumoRequest.ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("INVALIDA");
        request.setFormaPagamento("PIX");

        CheckoutService.CheckoutException exception = assertThrows(CheckoutService.CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("MODALIDADE_INVALIDA", exception.getCodigo());
    }

    @Test
    void validacao_ModalidadeIndisponivel_MotoboySobrepeso() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new ResumoRequest.ItemRequest("Pacote", new BigDecimal("100.00"), 1, new BigDecimal("6.00"))
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");

        CheckoutService.CheckoutException exception = assertThrows(CheckoutService.CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigo());
    }

    @Test
    void validacao_CupomInvalido() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new ResumoRequest.ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("INVALIDO");
        request.setFormaPagamento("PIX");

        CheckoutService.CheckoutException exception = assertThrows(CheckoutService.CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("CUPOM_INVALIDO", exception.getCodigo());
    }

    @Test
    void validacao_CupomNaoAplicavel_Menos50SoParaComprasAcimaDe300() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new ResumoRequest.ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");

        CheckoutService.CheckoutException exception = assertThrows(CheckoutService.CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigo());
    }

    @Test
    void validacao_FormaPagamentoInvalida() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new ResumoRequest.ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("INVALIDA");

        CheckoutService.CheckoutException exception = assertThrows(CheckoutService.CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigo());
    }

    @Test
    void validacao_ParcelamentoInvalido_PixComParcelas() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new ResumoRequest.ItemRequest("Camiseta", new BigDecimal("79.90"), 1, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(3);

        CheckoutService.CheckoutException exception = assertThrows(CheckoutService.CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigo());
    }

    @Test
    void validacao_FormaPagamentoIndisponivel_BoletoAcimaDe1000() {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new ResumoRequest.ItemRequest("Produto", new BigDecimal("500.00"), 3, new BigDecimal("1.00"))
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);

        CheckoutService.CheckoutException exception = assertThrows(CheckoutService.CheckoutException.class, () -> {
            service.calcularResumo(request);
        });
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigo());
    }

    @Test
    void cupomFretegratis_DescontoIgualAoFrete() throws CheckoutService.CheckoutException {
        ResumoRequest request = new ResumoRequest();
        request.setItens(Arrays.asList(
                new ResumoRequest.ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ResumoRequest.ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("FRETEGRATIS");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);

        ResumoResponse response = service.calcularResumo(request);

        BigDecimal frete = new BigDecimal("33.10");
        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(frete, response.getFrete());
        assertEquals(frete, response.getDescontoCupom());
        BigDecimal descontoPix = response.getAjustePagamento().negate();
        BigDecimal esperado = response.getSubtotalProdutos().subtract(descontoPix);
        assertEquals(esperado, response.getTotalFinal());
    }
}
