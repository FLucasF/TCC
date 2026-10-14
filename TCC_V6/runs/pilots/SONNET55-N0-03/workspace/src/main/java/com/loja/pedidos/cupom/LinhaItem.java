package com.loja.pedidos.cupom;

import java.math.BigDecimal;

import com.loja.pedidos.dominio.Produto;

public record LinhaItem(Produto produto, int quantidade) {

    public BigDecimal total() {
        return produto.preco().multiply(BigDecimal.valueOf(quantidade));
    }
}
