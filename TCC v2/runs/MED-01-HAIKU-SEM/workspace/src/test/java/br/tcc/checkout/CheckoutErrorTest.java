package br.tcc.checkout;

import br.tcc.checkout.dto.ItemCarrinho;
import br.tcc.checkout.dto.RequisicaoResumo;
import br.tcc.checkout.exception.ErroCheckout;
import br.tcc.checkout.service.CheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CheckoutErrorTest {

    private CheckoutService checkoutService;

    @BeforeEach
    void setup() {
        checkoutService = new CheckoutService();
    }

    @Test
    void testPedidoInvalidoCarrinhoVazio() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList());
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setFormaPagamento("PIX");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> {
            checkoutService.calcularResumo(requisicao);
        });
        assertEquals("PEDIDO_INVALIDO", erro.getCodigo());
    }

    @Test
    void testPedidoInvalidoPrecoZero() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
                new ItemCarrinho("Camiseta", 0.0, 1, 0.30)
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setFormaPagamento("PIX");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> {
            checkoutService.calcularResumo(requisicao);
        });
        assertEquals("PEDIDO_INVALIDO", erro.getCodigo());
    }

    @Test
    void testModalidadeInvalida() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
                new ItemCarrinho("Camiseta", 79.90, 1, 0.30)
        ));
        requisicao.setModalidadeEntrega("INEXISTENTE");
        requisicao.setFormaPagamento("PIX");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> {
            checkoutService.calcularResumo(requisicao);
        });
        assertEquals("MODALIDADE_INVALIDA", erro.getCodigo());
    }

    @Test
    void testModalidadeIndisponivel() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
                new ItemCarrinho("Item Pesado", 100.00, 1, 6.0)
        ));
        requisicao.setModalidadeEntrega("MOTOBOY");
        requisicao.setFormaPagamento("PIX");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> {
            checkoutService.calcularResumo(requisicao);
        });
        assertEquals("MODALIDADE_INDISPONIVEL", erro.getCodigo());
    }

    @Test
    void testCupomInvalido() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
                new ItemCarrinho("Camiseta", 79.90, 1, 0.30)
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setCupom("CUPOMINVALIDO");
        requisicao.setFormaPagamento("PIX");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> {
            checkoutService.calcularResumo(requisicao);
        });
        assertEquals("CUPOM_INVALIDO", erro.getCodigo());
    }

    @Test
    void testCupomNaoAplicavel() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
                new ItemCarrinho("Camiseta", 79.90, 1, 0.30)
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setCupom("MENOS50");
        requisicao.setFormaPagamento("PIX");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> {
            checkoutService.calcularResumo(requisicao);
        });
        assertEquals("CUPOM_NAO_APLICAVEL", erro.getCodigo());
    }

    @Test
    void testFormaPagamentoInvalida() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
                new ItemCarrinho("Camiseta", 79.90, 1, 0.30)
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setFormaPagamento("INVALIDA");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> {
            checkoutService.calcularResumo(requisicao);
        });
        assertEquals("FORMA_PAGAMENTO_INVALIDA", erro.getCodigo());
    }

    @Test
    void testParcelamentoInvalido() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
                new ItemCarrinho("Camiseta", 79.90, 1, 0.30)
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setFormaPagamento("PIX");
        requisicao.setParcelas(2);

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> {
            checkoutService.calcularResumo(requisicao);
        });
        assertEquals("PARCELAMENTO_INVALIDO", erro.getCodigo());
    }

    @Test
    void testFormaPagamentoIndisponivel() {
        RequisicaoResumo requisicao = new RequisicaoResumo();
        requisicao.setItens(Arrays.asList(
                new ItemCarrinho("Item Caro", 1500.00, 1, 1.0)
        ));
        requisicao.setModalidadeEntrega("RETIRADA_LOJA");
        requisicao.setFormaPagamento("BOLETO");

        ErroCheckout erro = assertThrows(ErroCheckout.class, () -> {
            checkoutService.calcularResumo(requisicao);
        });
        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", erro.getCodigo());
    }
}
