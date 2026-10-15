package com.loja.checkout.domain;

import java.util.List;

public class CupomLeve3Pague2 implements Cupom {
    @Override
    public double calcularDesconto(double subtotalProdutos, double valorFrete, List<ItemRequisicao> itens) {
        double desconto = 0.0;
        for (ItemRequisicao item : itens) {
            long unidadesGratis = item.getQuantidade() / 3;
            double descontoItem = unidadesGratis * item.getPrecoUnitario();
            desconto += descontoItem;
        }
        return desconto;
    }

    @Override
    public boolean eAplicavel(double subtotalProdutos) {
        return true;
    }

    @Override
    public String getCodigo() {
        return "LEVE3PAGUE2";
    }
}
