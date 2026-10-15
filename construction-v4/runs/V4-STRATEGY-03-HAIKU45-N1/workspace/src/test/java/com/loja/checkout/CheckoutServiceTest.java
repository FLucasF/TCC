package com.loja.checkout;

import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.exception.ResumoCheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class CheckoutServiceTest {

    @Autowired
    private CheckoutService checkoutService;

    @Test
    public void exemploFianceiro1() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
                new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemPedido("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        ResumoResponse resposta = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), resposta.getFrete());
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("10.24"), resposta.getSeguro());
        assertEquals(new BigDecimal("-20.60"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("391.47"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("391.47"), resposta.getValorParcela());
        assertEquals(new BigDecimal("0.00"), resposta.getCreditoProximaCompra());
        assertFalse(resposta.getBrinde());
    }

    @Test
    public void exemploFianceiro2() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
                new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemPedido("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom(null);
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);
        request.setNivelClube("PRATA");
        request.setRegiao("CENTRO_OESTE");

        ResumoResponse resposta = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), resposta.getFrete());
        assertEquals(7, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("6.15"), resposta.getSeguro());
        assertEquals(new BigDecimal("30.55"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("462.00"), resposta.getTotalFinal());
        assertEquals(6, resposta.getParcelas());
        assertEquals(new BigDecimal("77.00"), resposta.getValorParcela());
        assertEquals(new BigDecimal("8.19"), resposta.getCreditoProximaCompra());
        assertFalse(resposta.getBrinde());
    }

    @Test
    public void exemploFianceiro3() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
                new ItemPedido("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORDESTE");

        ResumoResponse resposta = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("399.80"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), resposta.getFrete());
        assertEquals(0, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("8.00"), resposta.getSeguro());
        assertEquals(new BigDecimal("3.49"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("379.29"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("379.29"), resposta.getValorParcela());
        assertEquals(new BigDecimal("0.00"), resposta.getCreditoProximaCompra());
        assertFalse(resposta.getBrinde());
    }

    @Test
    public void exemploFianceiro4() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
                new ItemPedido("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);
        request.setNivelClube("PRATA");
        request.setRegiao("SUL");

        ResumoResponse resposta = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("299.10"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resposta.getDescontoCupom());
        assertEquals(0, resposta.getFrete().compareTo(BigDecimal.ZERO));
        assertEquals(1, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("2.99"), resposta.getSeguro());
        assertEquals(0, resposta.getAjustePagamento().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("262.29"), resposta.getTotalFinal());
        assertEquals(3, resposta.getParcelas());
        assertEquals(new BigDecimal("87.43"), resposta.getValorParcela());
        assertEquals(new BigDecimal("5.98"), resposta.getCreditoProximaCompra());
        assertFalse(resposta.getBrinde());
    }

    @Test
    public void exemploFianceiro5() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
                new ItemPedido("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ItemPedido("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom(null);
        request.setFormaPagamento("PIX");
        request.setNivelClube("OURO");
        request.setRegiao("SUDESTE");

        ResumoResponse resposta = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(0, resposta.getDescontoCupom().compareTo(BigDecimal.ZERO));
        assertEquals(0, resposta.getFrete().compareTo(BigDecimal.ZERO));
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("4.10"), resposta.getSeguro());
        assertEquals(new BigDecimal("-20.69"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("393.11"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("393.11"), resposta.getValorParcela());
        assertEquals(new BigDecimal("20.48"), resposta.getCreditoProximaCompra());
        assertFalse(resposta.getBrinde());
    }

    @Test
    public void validacaoPedidoInvalido_CarrinhoVazio() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        ResumoCheckoutException exception = assertThrows(ResumoCheckoutException.class,
                () -> checkoutService.calcularResumo(request));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    public void validacaoNivelClubeInvalido() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
                new ItemPedido("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("INVALIDO");
        request.setRegiao("SUDESTE");

        ResumoCheckoutException exception = assertThrows(ResumoCheckoutException.class,
                () -> checkoutService.calcularResumo(request));
        assertEquals("NIVEL_CLUBE_INVALIDO", exception.getCodigoErro());
    }

    @Test
    public void validacaoRegiaoInvalida() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
                new ItemPedido("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("INVALIDA");

        ResumoCheckoutException exception = assertThrows(ResumoCheckoutException.class,
                () -> checkoutService.calcularResumo(request));
        assertEquals("REGIAO_INVALIDA", exception.getCodigoErro());
    }

    @Test
    public void validacaoModalidadeInvalida() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
                new ItemPedido("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega("INVALIDA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        ResumoCheckoutException exception = assertThrows(ResumoCheckoutException.class,
                () -> checkoutService.calcularResumo(request));
        assertEquals("MODALIDADE_INVALIDA", exception.getCodigoErro());
    }

    @Test
    public void validacaoModalidadeIndisponivel_MotoboySomacincokg() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
                new ItemPedido("Produto", new BigDecimal("100.00"), 1, new BigDecimal("5.5"))
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        ResumoCheckoutException exception = assertThrows(ResumoCheckoutException.class,
                () -> checkoutService.calcularResumo(request));
        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigoErro());
    }

    @Test
    public void validacaoCupomInvalido() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
                new ItemPedido("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("CUPOMINVALIDO");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        ResumoCheckoutException exception = assertThrows(ResumoCheckoutException.class,
                () -> checkoutService.calcularResumo(request));
        assertEquals("CUPOM_INVALIDO", exception.getCodigoErro());
    }

    @Test
    public void validacaoCupomNaoAplicavel_MENOS50() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
                new ItemPedido("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        ResumoCheckoutException exception = assertThrows(ResumoCheckoutException.class,
                () -> checkoutService.calcularResumo(request));
        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigoErro());
    }

    @Test
    public void validacaoFormaPagamentoInvalida() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
                new ItemPedido("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("INVALIDA");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        ResumoCheckoutException exception = assertThrows(ResumoCheckoutException.class,
                () -> checkoutService.calcularResumo(request));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigoErro());
    }

    @Test
    public void validacaoParcelamentoInvalido_PixMuliple() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
                new ItemPedido("Produto", new BigDecimal("100.00"), 1, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(2);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        ResumoCheckoutException exception = assertThrows(ResumoCheckoutException.class,
                () -> checkoutService.calcularResumo(request));
        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    public void validacaoFormaPagamentoIndisponivel_BoletoAcimaMil() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
                new ItemPedido("Produto", new BigDecimal("1000.00"), 2, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setFormaPagamento("BOLETO");
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");

        ResumoCheckoutException exception = assertThrows(ResumoCheckoutException.class,
                () -> checkoutService.calcularResumo(request));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigoErro());
    }

    @Test
    public void brindeOuroAcimaDe500() {
        PedidoRequest request = new PedidoRequest();
        request.setItens(Arrays.asList(
                new ItemPedido("Produto", new BigDecimal("250.00"), 2, new BigDecimal("0.5"))
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("OURO");
        request.setRegiao("SUDESTE");

        ResumoResponse resposta = checkoutService.calcularResumo(request);

        assertTrue(resposta.getBrinde());
    }
}
