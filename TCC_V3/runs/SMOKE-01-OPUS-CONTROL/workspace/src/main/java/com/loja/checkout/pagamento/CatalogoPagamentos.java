package com.loja.checkout.pagamento;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Reune todas as formas de pagamento aceitas. */
@Component
public class CatalogoPagamentos {

    private final Map<String, FormaPagamento> porCodigo;

    public CatalogoPagamentos(List<FormaPagamento> formas) {
        Map<String, FormaPagamento> mapa = new LinkedHashMap<>();
        formas.forEach(f -> mapa.put(f.codigo(), f));
        this.porCodigo = Map.copyOf(mapa);
    }

    public Optional<FormaPagamento> buscar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(porCodigo.get(codigo));
    }
}
