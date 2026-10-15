package com.loja.checkout.domain;

import java.util.List;

public class CupomBemVindo10 implements Cupom {
    @Override
    public double calcularDesconto(double subtotalProdutos, double valorFrete, List<ItemRequisicao> itens) {
        return subtotalProdutos * 0.10;
    }

    @Override
    public boolean eAplicavel(double subtotalProdutos) {
        return true;
    }

    @Override
    public String getCodigo() {
        return "BEMVINDO10";
    }
}
