package com.loja.checkout.cupom;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CupomRegistry {

    private final Map<String, Cupom> cuponsPorCodigo;

    public CupomRegistry(List<Cupom> cupons) {
        this.cuponsPorCodigo = cupons.stream()
                .collect(Collectors.toMap(Cupom::codigo, Function.identity()));
    }

    public Cupom buscar(String codigo) {
        return cuponsPorCodigo.get(codigo);
    }
}
