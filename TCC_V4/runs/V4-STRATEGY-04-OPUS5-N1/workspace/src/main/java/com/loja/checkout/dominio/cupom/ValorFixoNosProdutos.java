package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Centavos;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** Desconta um valor fixo dos produtos, a partir de um minimo em produtos. */
public record ValorFixoNosProdutos(String codigo, BigDecimal valor, BigDecimal minimoEmProdutos) implements Cupom {

    @Override
    public boolean aplicavel(Pedido pedido, BigDecimal frete) {
        return pedido.subtotalProdutos().compareTo(minimoEmProdutos) >= 0;
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Centavos.arredondar(valor);
    }
}
