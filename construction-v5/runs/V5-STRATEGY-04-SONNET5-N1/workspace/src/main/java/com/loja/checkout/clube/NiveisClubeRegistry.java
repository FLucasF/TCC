package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NiveisClubeRegistry {

    private final Map<String, NivelClube> porNome;

    public NiveisClubeRegistry(List<NivelClube> niveis) {
        this.porNome = niveis.stream()
                .collect(Collectors.toMap(NivelClube::nome, Function.identity()));
    }

    public Optional<NivelClube> buscar(String nome) {
        return Optional.ofNullable(porNome.get(nome));
    }
}
