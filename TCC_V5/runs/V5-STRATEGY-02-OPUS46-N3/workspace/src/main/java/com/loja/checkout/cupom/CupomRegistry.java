package com.loja.checkout.cupom;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CupomRegistry {

    private final Map<String, Cupom> cupons;

    public CupomRegistry(List<Cupom> lista) {
        this.cupons = lista.stream()
                .collect(Collectors.toMap(Cupom::codigo, Function.identity()));
    }

    public Cupom buscar(String codigo) {
        return codigo == null ? null : cupons.get(codigo);
    }
}
