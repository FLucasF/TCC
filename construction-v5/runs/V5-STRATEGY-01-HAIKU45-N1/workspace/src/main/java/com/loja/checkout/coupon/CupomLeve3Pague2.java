package com.loja.checkout.coupon;

import com.loja.checkout.dto.ItemPedido;
import java.util.List;

public class CupomLeve3Pague2 implements Cupom {
    @Override
    public double calcularDesconto(double subtotalProdutos, List<ItemPedido> itens) {
        double desconto = 0.0;
        for (ItemPedido item : itens) {
            int grupos = item.getQuantidade() / 3;
            desconto += grupos * item.getPrecoUnitario();
        }
        return desconto;
    }

    @Override
    public boolean estaAplicavel(double subtotalProdutos, List<ItemPedido> itens) {
        return true;
    }

    @Override
    public String getCodigo() {
        return "LEVE3PAGUE2";
    }
}
