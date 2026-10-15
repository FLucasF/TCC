package com.loja.checkout.validator;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.Item;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CheckoutValidatorTest {

    private final CheckoutValidator validator = new CheckoutValidator();

    private CheckoutRequest criarRequestValido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.00"))
        ));
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("PIX");
        request.setParcelas(1);
        request.setNivelClube("BRONZE");
        request.setRegiao("SUDESTE");
        return request;
    }

    @Test
    public void deveValidarCarrinhoVazio() {
        CheckoutRequest request = criarRequestValido();
        request.setItens(Collections.emptyList());

        assertEquals("PEDIDO_INVALIDO", validator.validar(request));
    }

    @Test
    public void deveValidarPrecoZero() {
        CheckoutRequest request = criarRequestValido();
        request.setItens(Arrays.asList(
            new Item("Produto", BigDecimal.ZERO, 1, new BigDecimal("1.00"))
        ));

        assertEquals("PEDIDO_INVALIDO", validator.validar(request));
    }

    @Test
    public void deveValidarQuantidadeZero() {
        CheckoutRequest request = criarRequestValido();
        request.setItens(Arrays.asList(
            new Item("Produto", new BigDecimal("100.00"), 0, new BigDecimal("1.00"))
        ));

        assertEquals("PEDIDO_INVALIDO", validator.validar(request));
    }

    @Test
    public void deveValidarPesoNegativo() {
        CheckoutRequest request = criarRequestValido();
        request.setItens(Arrays.asList(
            new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("-1.00"))
        ));

        assertEquals("PEDIDO_INVALIDO", validator.validar(request));
    }

    @Test
    public void deveValidarNivelClubeFaltando() {
        CheckoutRequest request = criarRequestValido();
        request.setNivelClube(null);

        assertEquals("NIVEL_CLUBE_INVALIDO", validator.validar(request));
    }

    @Test
    public void deveValidarNivelClubeInvalido() {
        CheckoutRequest request = criarRequestValido();
        request.setNivelClube("INVALIDO");

        assertEquals("NIVEL_CLUBE_INVALIDO", validator.validar(request));
    }

    @Test
    public void deveValidarRegiaoFaltando() {
        CheckoutRequest request = criarRequestValido();
        request.setRegiao(null);

        assertEquals("REGIAO_INVALIDA", validator.validar(request));
    }

    @Test
    public void deveValidarRegiaoInvalida() {
        CheckoutRequest request = criarRequestValido();
        request.setRegiao("INVALIDA");

        assertEquals("REGIAO_INVALIDA", validator.validar(request));
    }

    @Test
    public void deveValidarModalidadeFaltando() {
        CheckoutRequest request = criarRequestValido();
        request.setModalidadeEntrega(null);

        assertEquals("MODALIDADE_INVALIDA", validator.validar(request));
    }

    @Test
    public void deveValidarModalidadeInvalida() {
        CheckoutRequest request = criarRequestValido();
        request.setModalidadeEntrega("INVALIDA");

        assertEquals("MODALIDADE_INVALIDA", validator.validar(request));
    }

    @Test
    public void deveValidarMotoboyComPesoAlto() {
        CheckoutRequest request = criarRequestValido();
        request.setModalidadeEntrega("MOTOBOY");
        request.setItens(Arrays.asList(
            new Item("Produto", new BigDecimal("100.00"), 10, new BigDecimal("1.00"))
        ));

        assertEquals("MODALIDADE_INDISPONIVEL", validator.validar(request));
    }

    @Test
    public void deveValidarCupomInexistente() {
        CheckoutRequest request = criarRequestValido();
        request.setCupom("CUPOMINVALIDO");

        assertEquals("CUPOM_INVALIDO", validator.validar(request));
    }

    @Test
    public void deveValidarCupomMenos50ComPedidoPequeno() {
        CheckoutRequest request = criarRequestValido();
        request.setItens(Arrays.asList(
            new Item("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.00"))
        ));
        request.setCupom("MENOS50");

        assertEquals("CUPOM_NAO_APLICAVEL", validator.validar(request));
    }

    @Test
    public void deveValidarFormaPagamentoFaltando() {
        CheckoutRequest request = criarRequestValido();
        request.setFormaPagamento(null);

        assertEquals("FORMA_PAGAMENTO_INVALIDA", validator.validar(request));
    }

    @Test
    public void deveValidarFormaPagamentoInvalida() {
        CheckoutRequest request = criarRequestValido();
        request.setFormaPagamento("INVALIDA");

        assertEquals("FORMA_PAGAMENTO_INVALIDA", validator.validar(request));
    }

    @Test
    public void deveValidarParcelamentoPixInvalido() {
        CheckoutRequest request = criarRequestValido();
        request.setFormaPagamento("PIX");
        request.setParcelas(2);

        assertEquals("PARCELAMENTO_INVALIDO", validator.validar(request));
    }

    @Test
    public void deveValidarParcelamentoBoletoInvalido() {
        CheckoutRequest request = criarRequestValido();
        request.setFormaPagamento("BOLETO");
        request.setParcelas(2);

        assertEquals("PARCELAMENTO_INVALIDO", validator.validar(request));
    }

    @Test
    public void deveValidarParcelamentoCartaoInvalido() {
        CheckoutRequest request = criarRequestValido();
        request.setFormaPagamento("CARTAO");
        request.setParcelas(13);

        assertEquals("PARCELAMENTO_INVALIDO", validator.validar(request));
    }

    @Test
    public void deveValidarBoletoComTotalAlto() {
        CheckoutRequest request = criarRequestValido();
        request.setFormaPagamento("BOLETO");
        request.setItens(Arrays.asList(
            new Item("Produto", new BigDecimal("1001.00"), 1, new BigDecimal("1.00"))
        ));

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", validator.validar(request));
    }

    @Test
    public void deveValidarRequestValido() {
        CheckoutRequest request = criarRequestValido();

        assertNull(validator.validar(request));
    }
}
