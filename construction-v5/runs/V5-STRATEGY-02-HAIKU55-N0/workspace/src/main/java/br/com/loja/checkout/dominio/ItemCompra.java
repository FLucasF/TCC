package br.com.loja.checkout.dominio;

import java.math.BigDecimal;

public record ItemCompra(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public ItemCompra {
        if (precoUnitario == null || precoUnitario.signum() <= 0
                || pesoKg == null || pesoKg.signum() <= 0
                || quantidade <= 0) {
            throw new ErroPedidoException(CodigoErro.PEDIDO_INVALIDO);
        }
    }

    public BigDecimal subtotal() {
        return Dinheiro.centavos(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
