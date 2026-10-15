package com.loja.checkout.dominio.pagamento;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Reune as formas de pagamento aceitas. */
@Component
public class CatalogoFormasPagamento {

    private final Map<String, FormaPagamento> porCodigo = new LinkedHashMap<>();

    public CatalogoFormasPagamento(List<FormaPagamento> formas) {
        formas.forEach(forma -> porCodigo.put(forma.codigo(), forma));
    }

    public Optional<FormaPagamento> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
