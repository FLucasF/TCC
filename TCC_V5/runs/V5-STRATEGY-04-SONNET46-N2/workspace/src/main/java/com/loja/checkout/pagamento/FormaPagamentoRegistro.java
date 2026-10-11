package com.loja.checkout.pagamento;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FormaPagamentoRegistro {

    private final Map<String, FormaPagamento> formas;

    public FormaPagamentoRegistro(List<FormaPagamento> formas) {
        this.formas = formas.stream()
                .collect(Collectors.toMap(FormaPagamento::getCodigo, Function.identity()));
    }

    public FormaPagamento buscar(String codigo) {
        return formas.get(codigo);
    }
}
