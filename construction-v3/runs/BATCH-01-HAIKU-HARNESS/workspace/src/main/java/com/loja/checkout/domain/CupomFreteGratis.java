package com.loja.checkout.domain;

import java.util.List;

public class CupomFreteGratis implements Cupom {
    @Override
    public double calcularDesconto(double subtotalProdutos, double valorFrete, List<ItemRequisicao> itens) {
        return valorFrete;
    }

    @Override
    public boolean eAplicavel(double subtotalProdutos) {
        return true;
    }

    @Override
    public String getCodigo() {
        return "FRETEGRATIS";
    }
}
