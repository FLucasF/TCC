package com.loja.checkout;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CheckoutServiceTest {

    @Autowired
    private CheckoutService checkoutService;

    @Test
    void exemplo1_CamisetaTenisExpressaBemvindo10Pix() throws CheckoutException {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Camiseta", 79.90, 2, 0.30));
        itens.add(new ItemPedido("Tênis", 249.90, 1, 1.20));
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);

        CheckoutResponse response = checkoutService.calcularResumo(request);

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
    void exemplo2_CamisetaTenisEconomicaSemCupomCartao6x() throws CheckoutException {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Camiseta", 79.90, 2, 0.30));
        itens.add(new ItemPedido("Tênis", 249.90, 1, 1.20));
        request.setItens(itens);
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom(null);
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), response.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), response.getFrete());
        assertEquals(7, response.getPrazoEntregaDias());
        assertTrue(response.getAjustePagamento().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(6, response.getParcelas());
    }

    @Test
    void exemplo3_FoneMotoboyCupomMenos50Boleto() throws CheckoutException {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Fone", 199.90, 2, 0.25));
        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);

        CheckoutResponse response = checkoutService.calcularResumo(request);

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
    void exemplo4_MeiasCamisetasRetidaLojaCupomLeve3Pague2Cartao3x() throws CheckoutException {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Meia", 19.90, 7, 0.10));
        itens.add(new ItemPedido("Camiseta", 79.90, 2, 0.30));
        request.setItens(itens);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);

        CheckoutResponse response = checkoutService.calcularResumo(request);

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
    void testPedidoInvalido_CarrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(new ArrayList<>());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void testPedidoInvalido_PrecoZero() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Camiseta", 0.0, 1, 0.30));
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void testPedidoInvalido_QuantidadeZero() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Camiseta", 79.90, 0, 0.30));
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void testModalidadeInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Camiseta", 79.90, 1, 0.30));
        request.setItens(itens);
        request.setModalidadeEntrega("INVALIDA");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("MODALIDADE_INVALIDA", exception.getCodigoErro());
    }

    @Test
    void testModalidadeIndisponivel_MotoboySobrePeso() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Caixote", 100.0, 1, 6.0));
        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigoErro());
    }

    @Test
    void testCupomInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Camiseta", 79.90, 1, 0.30));
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("CUPOMINVALIDO");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("CUPOM_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void testCupomNaoAplicavel_Menos50() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Camiseta", 79.90, 1, 0.30));
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigoErro());
    }

    @Test
    void testFormaPagamentoInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Camiseta", 79.90, 1, 0.30));
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("INVALIDA");

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigoErro());
    }

    @Test
    void testParcelamentoInvalido_PxDe2x() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Camiseta", 79.90, 1, 0.30));
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(2);

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void testParcelamentoInvalido_CartaoAcima12x() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Camiseta", 79.90, 1, 0.30));
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(13);

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void testFormaPagamentoIndisponivel_BoletoAcima1000() {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Notebook", 1500.0, 1, 2.0));
        request.setItens(itens);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);

        CheckoutException exception = assertThrows(CheckoutException.class,
            () -> checkoutService.calcularResumo(request));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigoErro());
    }

    @Test
    void testFretegratisComFretegratisCupom() throws CheckoutException {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Camiseta", 79.90, 2, 0.30));
        itens.add(new ItemPedido("Tênis", 249.90, 1, 1.20));
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("FRETEGRATIS");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), response.getSubtotalProdutos());
        assertEquals(new BigDecimal("33.10"), response.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), response.getFrete());
    }

    @Test
    void testDefaultParcelas() throws CheckoutException {
        CheckoutRequest request = new CheckoutRequest();
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido("Camiseta", 79.90, 1, 0.30));
        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(null);

        CheckoutResponse response = checkoutService.calcularResumo(request);

        assertEquals(1, response.getParcelas());
    }
}
