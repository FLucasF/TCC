package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Item;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Cupom {
    boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos);
    BigDecimal desconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete);

    Map<String, Cupom> REGISTRO = carregar();

    private static Map<String, Cupom> carregar() {
        Map<String, Cupom> m = new LinkedHashMap<>();
        m.put("BEMVINDO10", new Bemvindo10());
        m.put("MENOS50", new Menos50());
        m.put("FRETEGRATIS", new FreteGratis());
        m.put("LEVE3PAGUE2", new Leve3Pague2());
        return Map.copyOf(m);
    }

    static Optional<Cupom> de(String codigo) {
        if (codigo == null) return Optional.empty();
        return Optional.ofNullable(REGISTRO.get(codigo));
    }
}
