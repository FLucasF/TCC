package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ContextoPedido;
import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class BemVindo10 implements Cupom {

    @Override
    public boolean podeAplicar(ContextoPedido contexto, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoPedido contexto, BigDecimal frete) {
        BigDecimal desconto = contexto.getSubtotalProdutos().multiply(new BigDecimal("0.10"));
        return Arredondamento.arredondar(desconto);
    }
}
