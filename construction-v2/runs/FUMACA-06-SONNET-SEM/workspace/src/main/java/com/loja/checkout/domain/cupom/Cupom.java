package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.ItemPedido;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;

import java.math.BigDecimal;
import java.util.List;

/**
 * Estrategia de desconto. Cada promocao nova entra implementando esta interface
 * e registrando um bean, sem precisar alterar o servico de checkout.
 */
public interface Cupom {

    String codigo();

    /**
     * Verifica se o pedido cumpre a condicao do cupom (ex.: valor minimo de compra).
     * Lanca CUPOM_NAO_APLICAVEL quando nao cumpre.
     */
    default void validarAplicavel(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        // por padrao, todo cupom se aplica a qualquer pedido
    }

    BigDecimal calcularDesconto(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete);

    default void naoAplicavel() {
        throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
    }
}
