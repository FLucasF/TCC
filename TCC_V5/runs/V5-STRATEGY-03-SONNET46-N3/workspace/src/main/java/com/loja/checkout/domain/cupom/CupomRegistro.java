package com.loja.checkout.domain.cupom;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CupomRegistro {

    private final Map<String, CupomStrategy> estrategias;

    public CupomRegistro(List<CupomStrategy> estrategias) {
        this.estrategias = estrategias.stream()
                .collect(Collectors.toMap(CupomStrategy::codigo, Function.identity()));
    }

    public CupomStrategy buscar(String codigo) {
        if (codigo == null) return null;
        return estrategias.get(codigo);
    }
}
