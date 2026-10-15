package com.loja.checkout.domain;

import java.util.List;

public interface Cupom {
    double calcularDesconto(double subtotalProdutos, double valorFrete, List<ItemRequisicao> itens);
    boolean eAplicavel(double subtotalProdutos);
    String getCodigo();
}
