package com.loja.checkout.pagamento;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Reune as formas de pagamento aceitas. */
@Component
public class CatalogoPagamentos {

    private final Map<String, FormaPagamento> porCodigo;

    public CatalogoPagamentos(List<FormaPagamento> formas) {
        this.porCodigo = formas.stream()
                .collect(Collectors.toUnmodifiableMap(FormaPagamento::codigo, Function.identity()));
    }

    public Optional<FormaPagamento> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
