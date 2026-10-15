package com.loja.checkout.dominio.cupom;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Acha o cupom pelo código exato. Uma promoção nova só precisa ser criada como
 * classe para passar a valer.
 */
@Component
public class CupomRegistry {

    private final Map<String, Cupom> porCodigo;

    public CupomRegistry(List<Cupom> cupons) {
        this.porCodigo = cupons.stream()
                .collect(Collectors.toMap(Cupom::codigo, Function.identity()));
    }

    public Optional<Cupom> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
