package br.tcc.checkout.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.tcc.checkout.dto.ItemCarrinho;
import br.tcc.checkout.dto.ResumoCheckoutRequest;
import br.tcc.checkout.dto.ResumoCheckoutResponse;
import br.tcc.checkout.exception.CheckoutException;

public class CheckoutServiceTest {

    private CheckoutService checkoutService;

    @BeforeEach
    public void setup() {
        checkoutService = new CheckoutService();
    }

    @Test
    public void exemplo1() throws CheckoutException {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        List<ItemCarrinho> itens = new ArrayList<>();

        ItemCarrinho item1 = new ItemCarrinho();
        item1.setNome("Camiseta");
        item1.setPrecoUnitario(new BigDecimal("79.90"));
        item1.setQuantidade(2);
        item1.setPesoKg(new BigDecimal("0.30"));
        itens.add(item1);

        ItemCarrinho item2 = new ItemCarrinho();
        item2.setNome("Tênis");
        item2.setPrecoUnitario(new BigDecimal("249.90"));
        item2.setQuantidade(1);
        item2.setPesoKg(new BigDecimal("1.20"));
        itens.add(item2);

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("BEMVINDO10");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);

        ResumoCheckoutResponse resposta = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("40.97"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("33.10"), resposta.getFrete());
        assertEquals(2, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("-20.09"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("381.74"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("381.74"), resposta.getValorParcela());
    }

    @Test
    public void exemplo2() throws CheckoutException {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        List<ItemCarrinho> itens = new ArrayList<>();

        ItemCarrinho item1 = new ItemCarrinho();
        item1.setNome("Camiseta");
        item1.setPrecoUnitario(new BigDecimal("79.90"));
        item1.setQuantidade(2);
        item1.setPesoKg(new BigDecimal("0.30"));
        itens.add(item1);

        ItemCarrinho item2 = new ItemCarrinho();
        item2.setNome("Tênis");
        item2.setPrecoUnitario(new BigDecimal("249.90"));
        item2.setQuantidade(1);
        item2.setPesoKg(new BigDecimal("1.20"));
        itens.add(item2);

        request.setItens(itens);
        request.setModalidadeEntrega("ECONOMICA");
        request.setCupom(null);
        request.setFormaPagamento("CARTAO");
        request.setParcelas(6);

        ResumoCheckoutResponse resposta = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("409.70"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("0.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("15.60"), resposta.getFrete());
        assertEquals(7, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("30.10"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("455.40"), resposta.getTotalFinal());
        assertEquals(6, resposta.getParcelas());
        assertEquals(new BigDecimal("75.90"), resposta.getValorParcela());
    }

    @Test
    public void exemplo3() throws CheckoutException {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        List<ItemCarrinho> itens = new ArrayList<>();

        ItemCarrinho item1 = new ItemCarrinho();
        item1.setNome("Fone");
        item1.setPrecoUnitario(new BigDecimal("199.90"));
        item1.setQuantidade(2);
        item1.setPesoKg(new BigDecimal("0.25"));
        itens.add(item1);

        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setCupom("MENOS50");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);

        ResumoCheckoutResponse resposta = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("399.80"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("50.00"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("18.00"), resposta.getFrete());
        assertEquals(0, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("3.49"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("371.29"), resposta.getTotalFinal());
        assertEquals(1, resposta.getParcelas());
        assertEquals(new BigDecimal("371.29"), resposta.getValorParcela());
    }

    @Test
    public void exemplo4() throws CheckoutException {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        List<ItemCarrinho> itens = new ArrayList<>();

        ItemCarrinho item1 = new ItemCarrinho();
        item1.setNome("Meia");
        item1.setPrecoUnitario(new BigDecimal("19.90"));
        item1.setQuantidade(7);
        item1.setPesoKg(new BigDecimal("0.10"));
        itens.add(item1);

        ItemCarrinho item2 = new ItemCarrinho();
        item2.setNome("Camiseta");
        item2.setPrecoUnitario(new BigDecimal("79.90"));
        item2.setQuantidade(2);
        item2.setPesoKg(new BigDecimal("0.30"));
        itens.add(item2);

        request.setItens(itens);
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setCupom("LEVE3PAGUE2");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(3);

        ResumoCheckoutResponse resposta = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("299.10"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("39.80"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("0.00"), resposta.getFrete());
        assertEquals(1, resposta.getPrazoEntregaDias());
        assertEquals(new BigDecimal("0.00"), resposta.getAjustePagamento());
        assertEquals(new BigDecimal("259.30"), resposta.getTotalFinal());
        assertEquals(3, resposta.getParcelas());
        assertEquals(new BigDecimal("86.43"), resposta.getValorParcela());
    }

    @Test
    public void testeCarrinhoVazio() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        request.setItens(new ArrayList<>());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    public void testeModalidadeInvalida() throws CheckoutException {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho item = new ItemCarrinho();
        item.setNome("Teste");
        item.setPrecoUnitario(new BigDecimal("10.00"));
        item.setQuantidade(1);
        item.setPesoKg(new BigDecimal("0.1"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega("INVALIDA");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("MODALIDADE_INVALIDA", exception.getCodigoErro());
    }

    @Test
    public void testeModalidadeIndisponivel() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho item = new ItemCarrinho();
        item.setNome("Teste");
        item.setPrecoUnitario(new BigDecimal("10.00"));
        item.setQuantidade(10);
        item.setPesoKg(new BigDecimal("1.0"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigoErro());
    }

    @Test
    public void testeCupomInvalido() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho item = new ItemCarrinho();
        item.setNome("Teste");
        item.setPrecoUnitario(new BigDecimal("10.00"));
        item.setQuantidade(1);
        item.setPesoKg(new BigDecimal("0.1"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("CUPOM_INVALIDO");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("CUPOM_INVALIDO", exception.getCodigoErro());
    }

    @Test
    public void testeCupomNaoAplicavel() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho item = new ItemCarrinho();
        item.setNome("Teste");
        item.setPrecoUnitario(new BigDecimal("10.00"));
        item.setQuantidade(1);
        item.setPesoKg(new BigDecimal("0.1"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("MENOS50");
        request.setFormaPagamento("PIX");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigoErro());
    }

    @Test
    public void testeFormaPagamentoInvalida() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho item = new ItemCarrinho();
        item.setNome("Teste");
        item.setPrecoUnitario(new BigDecimal("10.00"));
        item.setQuantidade(1);
        item.setPesoKg(new BigDecimal("0.1"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("INVALIDA");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigoErro());
    }

    @Test
    public void testeParcelamentoInvalido() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho item = new ItemCarrinho();
        item.setNome("Teste");
        item.setPrecoUnitario(new BigDecimal("10.00"));
        item.setQuantidade(1);
        item.setPesoKg(new BigDecimal("0.1"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(2);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    public void testeFormaPagamentoIndisponivel() {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho item = new ItemCarrinho();
        item.setNome("Teste");
        item.setPrecoUnitario(new BigDecimal("500.00"));
        item.setQuantidade(3);
        item.setPesoKg(new BigDecimal("0.1"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("BOLETO");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigoErro());
    }

    @Test
    public void testeFretegratisCupom() throws CheckoutException {
        ResumoCheckoutRequest request = new ResumoCheckoutRequest();
        List<ItemCarrinho> itens = new ArrayList<>();

        ItemCarrinho item = new ItemCarrinho();
        item.setNome("Teste");
        item.setPrecoUnitario(new BigDecimal("100.00"));
        item.setQuantidade(1);
        item.setPesoKg(new BigDecimal("1.0"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega("EXPRESSA");
        request.setCupom("FRETEGRATIS");
        request.setFormaPagamento("PIX");

        ResumoCheckoutResponse resposta = checkoutService.calcularResumo(request);

        assertEquals(new BigDecimal("100.00"), resposta.getSubtotalProdutos());
        assertEquals(new BigDecimal("29.50"), resposta.getDescontoCupom());
        assertEquals(new BigDecimal("29.50"), resposta.getFrete());
    }
}
