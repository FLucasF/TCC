package com.loja.checkout.pagamento;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Reune todas as formas de pagamento aceitas. */
@Component
public class CatalogoPagamentos {

    private final Map<String, FormaPagamento> porCodigo = new LinkedHashMap<>();

    public CatalogoPagamentos(List<FormaPagamento> formas) {
        for (FormaPagamento forma : formas) {
            porCodigo.put(forma.codigo(), forma);
        }
    }

    public Optional<FormaPagamento> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
