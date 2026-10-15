package com.loja.checkout.domain.cupom;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Reune os cupons validos hoje. */
@Component
public class CatalogoCupons {

    private final List<Cupom> cupons;

    public CatalogoCupons(List<Cupom> cupons) {
        this.cupons = List.copyOf(cupons);
    }

    public Optional<Cupom> porCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return cupons.stream().filter(c -> c.codigo().equals(codigo)).findFirst();
    }
}
