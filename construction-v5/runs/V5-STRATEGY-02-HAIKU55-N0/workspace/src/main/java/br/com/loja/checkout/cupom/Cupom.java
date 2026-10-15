package br.com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;

import br.com.loja.checkout.dominio.ItemCompra;

public interface Cupom {

    String codigo();

    default boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }

    BigDecimal desconto(BigDecimal subtotalProdutos, List<ItemCompra> itens, BigDecimal frete);
}
