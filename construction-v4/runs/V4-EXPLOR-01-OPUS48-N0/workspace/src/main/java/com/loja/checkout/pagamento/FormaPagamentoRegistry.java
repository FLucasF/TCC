package com.loja.checkout.pagamento;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Descobre todas as formas de pagamento registradas e as resolve por código. */
@Component
public class FormaPagamentoRegistry {

    private final Map<String, FormaPagamento> porCodigo;

    public FormaPagamentoRegistry(List<FormaPagamento> formas) {
        this.porCodigo = formas.stream()
                .collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
    }

    /** Busca a forma de pagamento pelo código; vazio quando o código não existe. */
    public Optional<FormaPagamento> buscar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(porCodigo.get(codigo));
    }
}
