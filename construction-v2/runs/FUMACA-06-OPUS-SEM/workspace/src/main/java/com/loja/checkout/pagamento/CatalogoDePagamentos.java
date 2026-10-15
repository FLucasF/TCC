package com.loja.checkout.pagamento;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CatalogoDePagamentos {

    private final Map<String, FormaPagamento> porCodigo;

    public CatalogoDePagamentos(List<FormaPagamento> formas) {
        this.porCodigo = formas.stream()
                .collect(Collectors.toUnmodifiableMap(FormaPagamento::codigo, Function.identity()));
    }

    public Optional<FormaPagamento> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
