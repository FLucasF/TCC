package com.loja.checkout.domain.pagamento;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Reune as formas de pagamento aceitas pela loja. */
@Component
public class CatalogoFormasPagamento {

    private final List<FormaPagamento> formas;

    public CatalogoFormasPagamento(List<FormaPagamento> formas) {
        this.formas = List.copyOf(formas);
    }

    public Optional<FormaPagamento> porCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return formas.stream().filter(f -> f.codigo().equals(codigo)).findFirst();
    }
}
