package com.loja.checkout;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutValidationTest {

    private final CheckoutService service = new CheckoutService();

    @Test
    void pedidoInvalido_carrinhoVazio() {
        PedidoRequest pedido = new PedidoRequest();
        pedido.setItens(new ArrayList<>());

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(pedido));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void pedidoInvalido_precoZero() {
        PedidoRequest pedido = criarPedidoBase();
        pedido.getItens().get(0).setPrecoUnitario(BigDecimal.ZERO);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(pedido));
        assertEquals("PEDIDO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void nivelClubeInvalido() {
        PedidoRequest pedido = criarPedidoBase();
        pedido.setNivelClube("PLATINA");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(pedido));
        assertEquals("NIVEL_CLUBE_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void regiaoInvalida() {
        PedidoRequest pedido = criarPedidoBase();
        pedido.setRegiao("EXTERIOR");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(pedido));
        assertEquals("REGIAO_INVALIDA", exception.getCodigoErro());
    }

    @Test
    void modalidadeInvalida() {
        PedidoRequest pedido = criarPedidoBase();
        pedido.setModalidadeEntrega("DRONE");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(pedido));
        assertEquals("MODALIDADE_INVALIDA", exception.getCodigoErro());
    }

    @Test
    void modalidadeIndisponivel_motoboyAcima5kg() {
        PedidoRequest pedido = criarPedidoBase();
        pedido.getItens().get(0).setPesoKg(new BigDecimal("6.0"));
        pedido.setModalidadeEntrega("MOTOBOY");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(pedido));
        assertEquals("MODALIDADE_INDISPONIVEL", exception.getCodigoErro());
    }

    @Test
    void cupomInvalido() {
        PedidoRequest pedido = criarPedidoBase();
        pedido.setCupom("INVALIDO");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(pedido));
        assertEquals("CUPOM_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void cupomNaoAplicavel_menos50Abaixo300() {
        PedidoRequest pedido = criarPedidoBase();
        pedido.getItens().get(0).setPrecoUnitario(new BigDecimal("100.00"));
        pedido.setCupom("MENOS50");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(pedido));
        assertEquals("CUPOM_NAO_APLICAVEL", exception.getCodigoErro());
    }

    @Test
    void formaPagamentoInvalida() {
        PedidoRequest pedido = criarPedidoBase();
        pedido.setFormaPagamento("CHEQUE");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(pedido));
        assertEquals("FORMA_PAGAMENTO_INVALIDA", exception.getCodigoErro());
    }

    @Test
    void parcelamentoInvalido_pixComMais1Parcela() {
        PedidoRequest pedido = criarPedidoBase();
        pedido.setFormaPagamento("PIX");
        pedido.setParcelas(2);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(pedido));
        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void parcelamentoInvalido_cartaoAcima12() {
        PedidoRequest pedido = criarPedidoBase();
        pedido.setFormaPagamento("CARTAO");
        pedido.setParcelas(13);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(pedido));
        assertEquals("PARCELAMENTO_INVALIDO", exception.getCodigoErro());
    }

    @Test
    void formaPagamentoIndisponivel_boletoAcima1000() {
        PedidoRequest pedido = criarPedidoBase();
        pedido.getItens().get(0).setPrecoUnitario(new BigDecimal("1000.00"));
        pedido.setFormaPagamento("BOLETO");

        CheckoutException exception = assertThrows(CheckoutException.class, () -> service.calcularResumo(pedido));
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getCodigoErro());
    }

    private PedidoRequest criarPedidoBase() {
        PedidoRequest pedido = new PedidoRequest();

        List<ItemCarrinho> itens = new ArrayList<>();
        ItemCarrinho item = new ItemCarrinho();
        item.setNome("Produto");
        item.setPrecoUnitario(new BigDecimal("100.00"));
        item.setQuantidade(1);
        item.setPesoKg(new BigDecimal("1.0"));
        itens.add(item);

        pedido.setItens(itens);
        pedido.setModalidadeEntrega("RETIRADA_LOJA");
        pedido.setFormaPagamento("PIX");
        pedido.setNivelClube("BRONZE");
        pedido.setRegiao("SUDESTE");

        return pedido;
    }
}
