package com.loja.checkout.dominio.cupom;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Catalogo dos cupons que valem hoje. */
@Component
public class Cupons {

    private final Map<String, Cupom> porCodigo = new HashMap<>();

    public Cupons(List<Cupom> cupons) {
        for (Cupom cupom : cupons) {
            porCodigo.put(cupom.codigo(), cupom);
        }
    }

    public Optional<Cupom> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
