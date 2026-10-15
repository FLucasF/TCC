package com.loja.checkout.cupom;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Reune os cupons que valem hoje na loja. */
@Component
public class CatalogoCupons {

    private final Map<String, Cupom> porCodigo = new LinkedHashMap<>();

    public CatalogoCupons(List<Cupom> cupons) {
        cupons.forEach(cupom -> porCodigo.put(cupom.codigo(), cupom));
    }

    public Optional<Cupom> porCodigo(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
