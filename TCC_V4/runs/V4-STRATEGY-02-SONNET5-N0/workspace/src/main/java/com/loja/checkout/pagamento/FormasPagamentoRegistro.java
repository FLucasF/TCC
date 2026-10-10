package com.loja.checkout.pagamento;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FormasPagamentoRegistro {

    private final Map<String, FormaPagamento> formasPorCodigo;

    public FormasPagamentoRegistro(List<FormaPagamento> formas) {
        this.formasPorCodigo = formas.stream()
                .collect(Collectors.toMap(FormaPagamento::getCodigo, Function.identity()));
    }

    public Optional<FormaPagamento> buscar(String codigo) {
        return Optional.ofNullable(formasPorCodigo.get(codigo));
    }
}
