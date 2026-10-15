package com.loja.checkout.coupon;

import com.loja.checkout.dto.ItemPedido;
import java.util.List;

public class CupomMenos50 implements Cupom {
    @Override
    public double calcularDesconto(double subtotalProdutos, List<ItemPedido> itens) {
        return 50.00;
    }

    @Override
    public boolean estaAplicavel(double subtotalProdutos, List<ItemPedido> itens) {
        return subtotalProdutos >= 300.00;
    }

    @Override
    public String getCodigo() {
        return "MENOS50";
    }
}
