package com.loja.checkout.clube;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public interface NivelClube {
    BigDecimal credito(BigDecimal subtotalProdutos);
    boolean pagaFrete();
    boolean temBrinde(BigDecimal subtotalProdutos);

    Map<String, NivelClube> REGISTRO = carregar();

    private static Map<String, NivelClube> carregar() {
        Map<String, NivelClube> m = new LinkedHashMap<>();
        m.put("BRONZE", new Bronze());
        m.put("PRATA", new Prata());
        m.put("OURO", new Ouro());
        return Map.copyOf(m);
    }

    static Optional<NivelClube> de(String codigo) {
        if (codigo == null) return Optional.empty();
        return Optional.ofNullable(REGISTRO.get(codigo));
    }
}
