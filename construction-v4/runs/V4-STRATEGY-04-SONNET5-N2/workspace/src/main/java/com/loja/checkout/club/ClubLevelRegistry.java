package com.loja.checkout.club;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ClubLevelRegistry {

    private final Map<String, ClubLevel> porCodigo;

    public ClubLevelRegistry(List<ClubLevel> niveis) {
        this.porCodigo = niveis.stream()
                .collect(Collectors.toMap(ClubLevel::getCodigo, Function.identity()));
    }

    public ClubLevel buscar(String codigo) {
        return codigo == null ? null : porCodigo.get(codigo);
    }
}
