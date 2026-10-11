package com.loja.checkout;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.service.CheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CheckoutValidationTest {

    @Autowired
    private CheckoutService checkoutService;

    @Test
    void testPedidoInvalidoCarrinhoVazio() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Collections.emptyList());
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("PEDIDO_INVALIDO", exception.getErrorCode());
    }

    @Test
    void testModalidadeIndisponivelMotoboyAcima5kg() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemCarrinho("Item Pesado", new BigDecimal("100.00"), 1, new BigDecimal("6.00"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.MOTOBOY);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("MODALIDADE_INDISPONIVEL", exception.getErrorCode());
    }

    @Test
    void testCupomInvalido() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.00"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setCupom("CUPOM_NAO_EXISTE");
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("CUPOM_INVALIDO", exception.getErrorCode());
    }

    @Test
    void testCupomNaoAplicavelMenos50() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.00"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setCupom("MENOS50");
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("CUPOM_NAO_APLICAVEL", exception.getErrorCode());
    }

    @Test
    void testParcelamentoInvalidoPixMais1x() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemCarrinho("Produto", new BigDecimal("100.00"), 1, new BigDecimal("1.00"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setFormaPagamento(FormaPagamento.PIX);
        request.setParcelas(2);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("PARCELAMENTO_INVALIDO", exception.getErrorCode());
    }

    @Test
    void testFormaPagamentoIndisponivelBoletoAcima1000() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItens(Arrays.asList(
                new ItemCarrinho("Produto", new BigDecimal("1500.00"), 1, new BigDecimal("1.00"))
        ));
        request.setModalidadeEntrega(ModalidadeEntrega.ECONOMICA);
        request.setFormaPagamento(FormaPagamento.BOLETO);
        request.setNivelClube(NivelClube.BRONZE);
        request.setRegiao(Regiao.SUDESTE);

        CheckoutException exception = assertThrows(CheckoutException.class, () -> {
            checkoutService.calcularResumo(request);
        });

        assertEquals("FORMA_PAGAMENTO_INDISPONIVEL", exception.getErrorCode());
    }
}
