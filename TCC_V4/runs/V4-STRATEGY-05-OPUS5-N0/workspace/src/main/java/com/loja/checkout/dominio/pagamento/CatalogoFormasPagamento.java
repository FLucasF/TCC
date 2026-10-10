package com.loja.checkout.dominio.pagamento;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Reune todas as formas de pagamento aceitas pela loja. */
@Component
public class CatalogoFormasPagamento {

    private final Map<String, FormaPagamento> porCodigo;

    public CatalogoFormasPagamento(List<FormaPagamento> formas) {
        this.porCodigo = formas.stream().collect(Collectors.toMap(
                FormaPagamento::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public Optional<FormaPagamento> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
