package com.loja.checkout.cupom;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Coleta todas as implementações de {@link CupomCalculadora} registradas como
 * componentes Spring e monta o mapa de lookup por código.
 */
@Component
public class CupomRegistry {

    private final Map<String, CupomCalculadora> porCodigo;

    public CupomRegistry(List<CupomCalculadora> cupons) {
        this.porCodigo = cupons.stream()
                .collect(Collectors.toUnmodifiableMap(CupomCalculadora::getCodigo, Function.identity()));
    }

    public Optional<CupomCalculadora> buscar(String codigo) {
        return Optional.ofNullable(porCodigo.get(codigo));
    }
}
