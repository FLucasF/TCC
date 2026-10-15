package com.loja.checkout.cupom;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CupomRegistro {

    private final Map<String, Cupom> porCodigo;

    public CupomRegistro(List<Cupom> cupons) {
        this.porCodigo = cupons.stream()
                .collect(Collectors.toMap(Cupom::codigo, Function.identity()));
    }

    public Optional<Cupom> buscar(String codigo) {
        return Optional.ofNullable(codigo == null ? null : porCodigo.get(codigo));
    }
}
