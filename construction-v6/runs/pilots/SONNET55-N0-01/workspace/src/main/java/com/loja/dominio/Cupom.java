package com.loja.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/** Um cupom de desconto. Para criar um novo, basta uma nova classe @Component. */
public interface Cupom {

    record Linha(String sku, BigDecimal preco, int quantidade) {
    }

    record Contexto(List<Linha> linhas, BigDecimal subtotalProdutos, BigDecimal frete,
                    String modalidade, BooleanSupplier primeiraCompra) {
    }

    String codigo();

    /** @return o desconto, ou vazio se o pedido não cumpre a condição do cupom */
    Optional<BigDecimal> desconto(Contexto contexto);
}
