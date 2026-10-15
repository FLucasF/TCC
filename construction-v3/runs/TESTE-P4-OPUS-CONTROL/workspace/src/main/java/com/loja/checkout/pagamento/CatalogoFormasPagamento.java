package com.loja.checkout.pagamento;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Reune todas as formas de pagamento aceitas. */
@Component
public class CatalogoFormasPagamento {

    private final Map<String, FormaPagamento> porCodigo = new LinkedHashMap<>();

    public CatalogoFormasPagamento(List<FormaPagamento> formas) {
        formas.forEach(f -> porCodigo.put(f.codigo(), f));
    }

    public Optional<FormaPagamento> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
