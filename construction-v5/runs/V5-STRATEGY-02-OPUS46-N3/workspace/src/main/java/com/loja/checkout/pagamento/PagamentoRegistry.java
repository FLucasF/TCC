package com.loja.checkout.pagamento;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PagamentoRegistry {

    private final Map<String, FormaPagamento> formas;

    public PagamentoRegistry(List<FormaPagamento> lista) {
        this.formas = lista.stream()
                .collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
    }

    public FormaPagamento buscar(String codigo) {
        return codigo == null ? null : formas.get(codigo);
    }
}
