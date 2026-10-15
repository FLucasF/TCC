package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Regiao do cliente. O seguro do envio e sempre a mesma conta, um percentual
 * sobre o valor dos produtos; so o percentual muda de regiao para regiao.
 */
public enum Regiao {

    SUDESTE("1"),
    SUL("1"),
    CENTRO_OESTE("1.5"),
    NORTE("2.5"),
    NORDESTE("2");

    private final Percentual percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = Percentual.de(percentualSeguro);
    }

    public static Optional<Regiao> porCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(regiao -> regiao.name().equals(codigo))
                .findFirst();
    }

    public BigDecimal seguro(Pedido pedido) {
        return percentualSeguro.sobre(pedido.subtotalProdutos());
    }
}
