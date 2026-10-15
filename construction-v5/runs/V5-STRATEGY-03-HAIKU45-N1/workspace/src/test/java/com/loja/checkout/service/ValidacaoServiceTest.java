package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.Item;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ValidacaoServiceTest {

    @Autowired
    private ValidacaoService validacaoService;

    @Test
    public void pedidoInvalido_CarrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.emptyList());
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        assertEquals("PEDIDO_INVALIDO", validacaoService.validar(request));
    }

    @Test
    public void pedidoInvalido_PrecoZero() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Produto", BigDecimal.ZERO, 1, BigDecimal.valueOf(0.5))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        assertEquals("PEDIDO_INVALIDO", validacaoService.validar(request));
    }

    @Test
    public void pedidoInvalido_QuantidadeZero() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Produto", BigDecimal.valueOf(100), 0, BigDecimal.valueOf(0.5))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        assertEquals("PEDIDO_INVALIDO", validacaoService.validar(request));
    }

    @Test
    public void nivelClubeInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Produto", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(0.5))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("DIAMANTE");
        request.setRegiao("NORTE");

        assertEquals("NIVEL_CLUBE_INVALIDO", validacaoService.validar(request));
    }

    @Test
    public void regiaoInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Produto", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(0.5))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("INVALIDA");

        assertEquals("REGIAO_INVALIDA", validacaoService.validar(request));
    }

    @Test
    public void modalidadeInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Produto", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(0.5))
        ));
        request.setModalidadeEntrega("INVALIDA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        assertEquals("MODALIDADE_INVALIDA", validacaoService.validar(request));
    }

    @Test
    public void modalidadeIndisponivel_MotoboySobrepeso() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Produto", BigDecimal.valueOf(100), 10, BigDecimal.valueOf(1))
        ));
        request.setModalidadeEntrega("MOTOBOY");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        assertEquals("MODALIDADE_INDISPONIVEL", validacaoService.validar(request));
    }

    @Test
    public void cupomInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Produto", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(0.5))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");
        request.setCupom("INVALIDO");

        assertEquals("CUPOM_INVALIDO", validacaoService.validar(request));
    }

    @Test
    public void cupomNaoAplicavel_Menos50() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Produto", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(0.5))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");
        request.setCupom("MENOS50");

        assertEquals("CUPOM_NAO_APLICAVEL", validacaoService.validar(request));
    }

    @Test
    public void formaPagamentoInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Produto", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(0.5))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("INVALIDA");
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        assertEquals("FORMA_PAGAMENTO_INVALIDA", validacaoService.validar(request));
    }

    @Test
    public void parcelamentoInvalido_CartaoMenosDe1() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Produto", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(0.5))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(0);
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        assertEquals("PARCELAMENTO_INVALIDO", validacaoService.validar(request));
    }

    @Test
    public void parcelamentoInvalido_CartaoMaisDe12() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Produto", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(0.5))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("CARTAO");
        request.setParcelas(13);
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        assertEquals("PARCELAMENTO_INVALIDO", validacaoService.validar(request));
    }

    @Test
    public void parcelamentoInvalido_PixParcelado() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Produto", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(0.5))
        ));
        request.setModalidadeEntrega("EXPRESSA");
        request.setFormaPagamento("PIX");
        request.setParcelas(2);
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        assertEquals("PARCELAMENTO_INVALIDO", validacaoService.validar(request));
    }

    @Test
    public void formaPagamentoIndisponivel_BoletoAcima1000() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Produto", BigDecimal.valueOf(3000), 1, BigDecimal.valueOf(0.5))
        ));
        request.setModalidadeEntrega("RETIRADA_LOJA");
        request.setFormaPagamento("BOLETO");
        request.setParcelas(1);
        request.setNivelClube("BRONZE");
        request.setRegiao("NORTE");

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", validacaoService.validar(request));
    }
}
