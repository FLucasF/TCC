package com.loja.resumo.cupom;

import com.loja.resumo.dominio.Pedido;

import java.math.BigDecimal;
import java.util.Optional;

public interface Cupom {

    boolean aplicavel(Pedido pedido, BigDecimal freteExibido);

    BigDecimal desconto(Pedido pedido, BigDecimal freteExibido);

    static Optional<Cupom> encontrar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(CupomPadrao.valueOf(codigo));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
