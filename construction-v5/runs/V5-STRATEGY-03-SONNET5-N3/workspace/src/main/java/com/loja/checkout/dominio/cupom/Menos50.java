package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ContextoPedido;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.erro.PedidoInvalidoException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Menos50 implements Cupom {

    private static final BigDecimal SUBTOTAL_MINIMO = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public void validarAplicavel(ContextoPedido contexto) {
        if (contexto.subtotalProdutos().compareTo(SUBTOTAL_MINIMO) < 0) {
            throw new PedidoInvalidoException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
    }

    @Override
    public BigDecimal calcularDesconto(ContextoPedido contexto, BigDecimal frete) {
        return DESCONTO;
    }
}
