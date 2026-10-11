package com.loja.cupom;

import com.loja.domain.Pedido;
import com.loja.util.Arredondador;
import java.math.BigDecimal;

public class BemVindo10 implements Cupom {

    @Override
    public boolean aplicavel(Pedido pedido, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(Pedido pedido, BigDecimal frete) {
        BigDecimal percentual = new BigDecimal("0.10");
        BigDecimal desconto = pedido.subtotalProdutos().multiply(percentual);
        return Arredondador.arredondar(desconto);
    }
}
