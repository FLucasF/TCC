package com.loja.checkout.pagamento;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Reune todas as formas de pagamento aceitas pela loja. */
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
