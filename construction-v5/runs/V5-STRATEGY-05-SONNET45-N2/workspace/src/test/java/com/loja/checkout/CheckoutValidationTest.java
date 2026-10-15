package com.loja.checkout;

import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.regiao.Regiao;
import com.loja.checkout.model.request.CheckoutRequest;
import com.loja.checkout.model.request.ItemRequest;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutValidationTest {

    private final CheckoutService service = new CheckoutService();

    @Test
    void deveLancarErroPedidoInvalido_CarrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(new ArrayList<>());
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("PIX");
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("PEDIDO_INVALIDO", exception.getMessage());
    }

    @Test
    void deveLancarErroPedidoInvalido_PrecoNegativo() {
        CheckoutRequest request = criarRequestBasico();
        request.getItens().get(0).setPrecoUnitario(new BigDecimal("-10.00"));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("PEDIDO_INVALIDO", exception.getMessage());
    }

    @Test
    void deveLancarErroNivelClubeInvalido() {
        CheckoutRequest request = criarRequestBasico();
        request.setNivelClube(null);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("NIVEL_CLUBE_INVALIDO", exception.getMessage());
    }

    @Test
    void deveLancarErroRegiaoInvalida() {
        CheckoutRequest request = criarRequestBasico();
        request.setRegiao(null);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("REGIAO_INVALIDA", exception.getMessage());
    }

    @Test
    void deveLancarErroModalidadeInvalida() {
        CheckoutRequest request = criarRequestBasico();
        request.setModalidadeEntrega("INVALIDA");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("MODALIDADE_INVALIDA", exception.getMessage());
    }

    @Test
    void deveLancarErroModalidadeIndisponivel_MotoboyAcima5Kg() {
        CheckoutRequest request = criarRequestBasico();
        request.getItens().get(0).setPesoKg(new BigDecimal("6.00"));
        request.setModalidadeEntrega("MOTOBOY");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("MODALIDADE_INDISPONIVEL", exception.getMessage());
    }

    @Test
    void deveLancarErroCupomInvalido() {
        CheckoutRequest request = criarRequestBasico();
        request.setCupom("CUPOMINEXISTENTE");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("CUPOM_INVALIDO", exception.getMessage());
    }

    @Test
    void deveLancarErroCupomNaoAplicavel_Menos50Abaixo300() {
        CheckoutRequest request = criarRequestBasico();
        request.getItens().get(0).setPrecoUnitario(new BigDecimal("100.00"));
        request.setCupom("MENOS50");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("CUPOM_NAO_APLICAVEL", exception.getMessage());
    }

    @Test
    void deveLancarErroFormaPagamentoInvalida() {
        CheckoutRequest request = criarRequestBasico();
        request.setFormaPagamento("INVALIDA");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getMessage());
    }

    @Test
    void deveLancarErroParcelamentoInvalido_PixComParcelas() {
        CheckoutRequest request = criarRequestBasico();
        request.setFormaPagamento("PIX");
        request.setParcelas(3);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("PARCELAMENTO_INVALIDO", exception.getMessage());
    }

    @Test
    void deveLancarErroParcelamentoInvalido_CartaoAcima12Parcelas() {
        CheckoutRequest request = criarRequestBasico();
        request.setFormaPagamento("CARTAO");
        request.setParcelas(15);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("PARCELAMENTO_INVALIDO", exception.getMessage());
    }

    @Test
    void deveLancarErroFormaPagamentoIndisponivel_BoletoAcima1000() {
        CheckoutRequest request = criarRequestBasico();
        request.getItens().get(0).setPrecoUnitario(new BigDecimal("1100.00"));
        request.setFormaPagamento("BOLETO");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.calcularResumo(request)
        );

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getMessage());
    }

    private CheckoutRequest criarRequestBasico() {
        CheckoutRequest request = new CheckoutRequest();

        List<ItemRequest> itens = new ArrayList<>();
        ItemRequest item = new ItemRequest();
        item.setNome("Produto");
        item.setPrecoUnitario(new BigDecimal("100.00"));
        item.setQuantidade(1);
        item.setPesoKg(new BigDecimal("1.00"));
        itens.add(item);

        request.setItens(itens);
        request.setModalidadeEntrega("ECONOMICA");
        request.setFormaPagamento("PIX");
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        return request;
    }
}
