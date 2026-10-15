package com.loja.checkout.dominio.cupom;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Catalogo dos cupons que valem hoje. */
@Component
public class Cupons {

    private final Map<String, Cupom> porCodigo = new LinkedHashMap<>();

    public Cupons(List<Cupom> cupons) {
        cupons.forEach(cupom -> porCodigo.put(cupom.codigo(), cupom));
    }

    public Optional<Cupom> porCodigo(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
