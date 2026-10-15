package com.loja.checkout.dominio.cupom;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RegistroCupons {

    private final Map<String, Cupom> cuponsPorCodigo;

    public RegistroCupons(List<Cupom> cupons) {
        this.cuponsPorCodigo = cupons.stream()
                .collect(Collectors.toUnmodifiableMap(Cupom::codigo, Function.identity()));
    }

    public Optional<Cupom> buscar(String codigo) {
        return Optional.ofNullable(cuponsPorCodigo.get(codigo));
    }
}
