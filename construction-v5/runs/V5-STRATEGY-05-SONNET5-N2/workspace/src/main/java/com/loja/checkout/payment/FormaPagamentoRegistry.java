package com.loja.checkout.payment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class FormaPagamentoRegistry {

    private final Map<String, FormaPagamento> porCodigo;

    public FormaPagamentoRegistry(List<FormaPagamento> formas) {
        this.porCodigo = formas.stream()
                .collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
    }

    public Optional<FormaPagamento> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
