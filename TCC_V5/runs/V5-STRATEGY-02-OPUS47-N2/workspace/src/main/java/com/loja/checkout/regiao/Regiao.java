package com.loja.checkout.regiao;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.Pedido;
import java.math.BigDecimal;
import java.util.Optional;

public enum Regiao {
    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal taxa;

    Regiao(String taxa) {
        this.taxa = new BigDecimal(taxa);
    }

    public BigDecimal seguro(Pedido pedido) {
        return Dinheiro.emCentavos(pedido.subtotal().multiply(taxa));
    }

    public static Optional<Regiao> buscar(String codigo) {
        if (codigo == null) return Optional.empty();
        try {
            return Optional.of(Regiao.valueOf(codigo));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
