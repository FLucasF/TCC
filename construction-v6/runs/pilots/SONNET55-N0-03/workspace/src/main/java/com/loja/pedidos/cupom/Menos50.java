package com.loja.pedidos.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.loja.pedidos.dominio.Dinheiro;
import com.loja.pedidos.erro.CodigoErro;
import com.loja.pedidos.erro.ErroNegocio;

@Component
public class Menos50 implements Cupom {

    private static final BigDecimal MINIMO = Dinheiro.valor("300.00");
    private static final BigDecimal DESCONTO = Dinheiro.valor("50.00");

    public String codigo() {
        return "MENOS50";
    }

    public BigDecimal desconto(ContextoCupom ctx) {
        if (ctx.subtotal().compareTo(MINIMO) < 0) {
            throw new ErroNegocio(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return DESCONTO;
    }
}
