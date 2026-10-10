package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ContextoPedido;
import java.math.BigDecimal;

public class Menos50 implements Cupom {

    @Override
    public boolean podeAplicar(ContextoPedido contexto, BigDecimal frete) {
        return contexto.getSubtotalProdutos().compareTo(new BigDecimal("300.00")) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoPedido contexto, BigDecimal frete) {
        return new BigDecimal("50.00");
    }
}
