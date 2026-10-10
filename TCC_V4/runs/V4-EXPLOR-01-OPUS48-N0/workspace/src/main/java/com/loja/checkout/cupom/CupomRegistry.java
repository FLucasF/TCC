package com.loja.checkout.cupom;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Descobre todos os cupons registrados e os resolve por código. */
@Component
public class CupomRegistry {

    private final Map<String, Cupom> porCodigo;

    public CupomRegistry(List<Cupom> cupons) {
        this.porCodigo = cupons.stream()
                .collect(Collectors.toMap(Cupom::codigo, Function.identity()));
    }

    /** Busca o cupom pelo código; vazio quando o código não existe. */
    public Optional<Cupom> buscar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(porCodigo.get(codigo));
    }
}
