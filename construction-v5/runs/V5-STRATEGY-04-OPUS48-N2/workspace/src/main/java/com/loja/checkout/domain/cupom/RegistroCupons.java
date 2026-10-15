package com.loja.checkout.domain.cupom;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Guarda os cupons por código. Achar o cupom é uma busca no mapa. */
@Component
public class RegistroCupons {

    private final Map<String, Cupom> porCodigo;

    public RegistroCupons(List<Cupom> cupons) {
        this.porCodigo = cupons.stream()
                .collect(Collectors.toUnmodifiableMap(Cupom::codigo, Function.identity()));
    }

    public Optional<Cupom> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
