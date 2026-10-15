package com.loja.checkout.club;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ClubLevelRegistry {

    private final Map<String, ClubLevel> niveis;

    public ClubLevelRegistry(List<ClubLevel> niveis) {
        this.niveis = niveis.stream()
                .collect(Collectors.toMap(ClubLevel::codigo, Function.identity()));
    }

    public Optional<ClubLevel> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(niveis::get);
    }
}
