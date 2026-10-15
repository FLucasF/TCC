package com.loja.checkout.dominio.pagamento;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FormasPagamento {

    private final Map<String, FormaPagamento> porCodigo;

    public FormasPagamento(List<FormaPagamento> formas) {
        this.porCodigo = formas.stream()
                .collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
    }

    public Optional<FormaPagamento> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
