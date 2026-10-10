package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Centavos;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** Desconta uma porcentagem do valor dos produtos. */
public record PercentualNosProdutos(String codigo, BigDecimal taxa) implements Cupom {

    @Override
    public boolean aplicavel(Pedido pedido, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Centavos.percentual(pedido.subtotalProdutos(), taxa);
    }
}
