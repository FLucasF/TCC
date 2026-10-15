package com.loja.checkout.domain.pagamento;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FormaPagamentoRegistro {

    private final Map<String, FormaPagamentoStrategy> estrategias;

    public FormaPagamentoRegistro(List<FormaPagamentoStrategy> estrategias) {
        this.estrategias = estrategias.stream()
                .collect(Collectors.toMap(FormaPagamentoStrategy::codigo, Function.identity()));
    }

    public FormaPagamentoStrategy buscar(String codigo) {
        if (codigo == null) return null;
        return estrategias.get(codigo);
    }
}
