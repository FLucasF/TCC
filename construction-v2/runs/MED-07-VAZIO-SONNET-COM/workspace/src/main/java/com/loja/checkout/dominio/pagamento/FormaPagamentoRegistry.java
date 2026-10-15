package com.loja.checkout.dominio.pagamento;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FormaPagamentoRegistry {

    private final Map<String, FormaPagamento> formas;

    public FormaPagamentoRegistry() {
        this(List.of(new Pix(), new Cartao(), new Boleto()));
    }

    public FormaPagamentoRegistry(List<FormaPagamento> formas) {
        this.formas = formas.stream()
                .collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
    }

    public Optional<FormaPagamento> buscar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(formas.get(codigo));
    }
}
