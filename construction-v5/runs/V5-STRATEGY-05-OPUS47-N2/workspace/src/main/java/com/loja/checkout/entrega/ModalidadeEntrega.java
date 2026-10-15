package com.loja.checkout.entrega;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public interface ModalidadeEntrega {
    BigDecimal custo(BigDecimal pesoKg);
    int prazoDias();
    boolean aceita(BigDecimal pesoKg);

    Map<String, ModalidadeEntrega> REGISTRO = carregar();

    private static Map<String, ModalidadeEntrega> carregar() {
        Map<String, ModalidadeEntrega> m = new LinkedHashMap<>();
        m.put("ECONOMICA", new EconomicaEntrega());
        m.put("EXPRESSA", new ExpressaEntrega());
        m.put("RETIRADA_LOJA", new RetiradaLojaEntrega());
        m.put("MOTOBOY", new MotoboyEntrega());
        return Map.copyOf(m);
    }

    static Optional<ModalidadeEntrega> de(String codigo) {
        if (codigo == null) return Optional.empty();
        return Optional.ofNullable(REGISTRO.get(codigo));
    }
}
