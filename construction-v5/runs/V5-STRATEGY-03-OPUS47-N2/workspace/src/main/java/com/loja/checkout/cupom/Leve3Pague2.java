package com.loja.checkout.cupom;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.Pedido;

import java.math.BigDecimal;

public class Leve3Pague2 implements Cupom {
    @Override
    public void validar(Pedido pedido, BigDecimal subtotal, BigDecimal frete) {}

    @Override
    public BigDecimal calcularDesconto(Pedido pedido, BigDecimal subtotal, BigDecimal frete) {
        BigDecimal total = BigDecimal.ZERO;
        for (Pedido.Item item : pedido.itens()) {
            int gratuitos = item.quantidade() / 3;
            if (gratuitos > 0) {
                total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratuitos)));
            }
        }
        return Dinheiro.arredonda(total);
    }
}
