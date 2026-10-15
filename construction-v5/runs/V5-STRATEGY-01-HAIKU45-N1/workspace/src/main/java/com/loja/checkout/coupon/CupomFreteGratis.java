package com.loja.checkout.coupon;

import com.loja.checkout.dto.ItemPedido;
import java.util.List;

public class CupomFreteGratis implements Cupom {
    private double freteValue = 0.0;

    public void setFreteValue(double frete) {
        this.freteValue = frete;
    }

    @Override
    public double calcularDesconto(double subtotalProdutos, List<ItemPedido> itens) {
        return freteValue;
    }

    @Override
    public boolean estaAplicavel(double subtotalProdutos, List<ItemPedido> itens) {
        return true;
    }

    @Override
    public String getCodigo() {
        return "FRETEGRATIS";
    }
}
