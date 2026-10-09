package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import java.util.List;

public interface CupomStrategy {

    String codigo();

    boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos);

    BigDecimal calcularDesconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal freteExibido);
}
