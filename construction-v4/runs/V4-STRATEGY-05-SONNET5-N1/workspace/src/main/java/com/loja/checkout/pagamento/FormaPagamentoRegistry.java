package com.loja.checkout.pagamento;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FormaPagamentoRegistry {

    private final Map<String, FormaPagamento> formasPorCodigo;

    public FormaPagamentoRegistry(List<FormaPagamento> formas) {
        this.formasPorCodigo = formas.stream()
                .collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
    }

    public FormaPagamento buscar(String codigo) {
        return formasPorCodigo.get(codigo);
    }
}
