package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NivelClubeRegistro {

    private final Map<String, NivelClube> niveis;

    public NivelClubeRegistro(List<NivelClube> niveis) {
        this.niveis = niveis.stream()
                .collect(Collectors.toMap(NivelClube::getCodigo, Function.identity()));
    }

    public NivelClube buscar(String codigo) {
        return niveis.get(codigo);
    }
}
