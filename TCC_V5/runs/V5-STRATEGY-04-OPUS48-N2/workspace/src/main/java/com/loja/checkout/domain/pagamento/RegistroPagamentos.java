package com.loja.checkout.domain.pagamento;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Guarda as formas de pagamento por código. Achar a forma é uma busca no mapa. */
@Component
public class RegistroPagamentos {

    private final Map<String, FormaPagamento> porCodigo;

    public RegistroPagamentos(List<FormaPagamento> formas) {
        this.porCodigo = formas.stream()
                .collect(Collectors.toUnmodifiableMap(FormaPagamento::codigo, Function.identity()));
    }

    public Optional<FormaPagamento> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
