package com.loja.checkout.coupon;

import com.loja.checkout.dto.ItemPedido;
import java.util.List;

public class CupomBemvindo10 implements Cupom {
    @Override
    public double calcularDesconto(double subtotalProdutos, List<ItemPedido> itens) {
        return subtotalProdutos * 0.10;
    }

    @Override
    public boolean estaAplicavel(double subtotalProdutos, List<ItemPedido> itens) {
        return true;
    }

    @Override
    public String getCodigo() {
        return "BEMVINDO10";
    }
}
