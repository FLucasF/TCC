package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NiveisClubeRegistry {

    private final Map<String, NivelClube> niveis;

    public NiveisClubeRegistry(List<NivelClube> lista) {
        this.niveis = lista.stream()
                .collect(Collectors.toMap(NivelClube::codigo, Function.identity()));
    }

    public NivelClube buscar(String codigo) {
        return codigo == null ? null : niveis.get(codigo);
    }
}
