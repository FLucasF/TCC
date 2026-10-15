package com.loja.checkout.domain;

import java.util.List;

public class CupomMenos50 implements Cupom {
    @Override
    public double calcularDesconto(double subtotalProdutos, double valorFrete, List<ItemRequisicao> itens) {
        return 50.00;
    }

    @Override
    public boolean eAplicavel(double subtotalProdutos) {
        return subtotalProdutos >= 300.00;
    }

    @Override
    public String getCodigo() {
        return "MENOS50";
    }
}
