package br.com.loja.pedidos.checkout;

import java.math.BigDecimal;

import br.com.loja.pedidos.catalogo.Produto;

public record ItemCarrinho(Produto produto, int quantidade) {

    public BigDecimal valor() {
        return produto.preco().multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal peso() {
        return produto.pesoKg().multiply(BigDecimal.valueOf(quantidade));
    }
}
