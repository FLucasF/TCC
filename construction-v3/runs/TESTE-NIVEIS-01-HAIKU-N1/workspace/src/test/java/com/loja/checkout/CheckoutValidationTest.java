package com.loja.checkout;

import com.loja.checkout.model.*;
import com.loja.checkout.service.CheckoutService;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CheckoutValidationTest {

    private final CheckoutService service = new CheckoutService();

    @Test
    void pedidoInvalidoCarrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.emptyList());
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        Object resultado = service.processar(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("PEDIDO_INVALIDO", ((ErrorResponse) resultado).getErro());
    }

    @Test
    void pedidoInvalidoPrecoNegativo() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", -10.0, 1, 0.30)
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        Object resultado = service.processar(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("PEDIDO_INVALIDO", ((ErrorResponse) resultado).getErro());
    }

    @Test
    void nivelClubeInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 1, 0.30)
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(null);
        request.setRegiao(Regiao.SUDESTE);

        Object resultado = service.processar(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("NIVEL_CLUBE_INVALIDO", ((ErrorResponse) resultado).getErro());
    }

    @Test
    void regiaoInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 1, 0.30)
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(null);

        Object resultado = service.processar(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("REGIAO_INVALIDA", ((ErrorResponse) resultado).getErro());
    }

    @Test
    void modalidadeEntregaInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 1, 0.30)
        ));
        request.setModalidadeEntrega(null);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        Object resultado = service.processar(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("MODALIDADE_INVALIDA", ((ErrorResponse) resultado).getErro());
    }

    @Test
    void modalidadeIndisponivel() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Tela", 1000.0, 10, 1.0)
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.MOTOBOY);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        Object resultado = service.processar(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("MODALIDADE_INDISPONIVEL", ((ErrorResponse) resultado).getErro());
    }

    @Test
    void cupomInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 1, 0.30)
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setCupom("CUPOM_INEXISTENTE");
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        Object resultado = service.processar(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("CUPOM_INVALIDO", ((ErrorResponse) resultado).getErro());
    }

    @Test
    void cupomNaoAplicavel() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 1, 0.30)
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setCupom("MENOS50");
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        Object resultado = service.processar(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("CUPOM_NAO_APLICAVEL", ((ErrorResponse) resultado).getErro());
    }

    @Test
    void formaPagamentoInvalida() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 1, 0.30)
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(null);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        Object resultado = service.processar(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("FORMA_PAGAMENTO_INVALIDA", ((ErrorResponse) resultado).getErro());
    }

    @Test
    void parcelamentoInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 79.90, 1, 0.30)
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setParcelas(2);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        Object resultado = service.processar(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("PARCELAMENTO_INVALIDO", ((ErrorResponse) resultado).getErro());
    }

    @Test
    void formaPagamentoIndisponivel() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
            new Item("Camiseta", 1001.0, 1, 0.30)
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.EXPRESSA);
        request.setFormaPagamento(FormaPagamento.BOLETO);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        Object resultado = service.processar(request);
        assertInstanceOf(ErrorResponse.class, resultado);
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", ((ErrorResponse) resultado).getErro());
    }
}
