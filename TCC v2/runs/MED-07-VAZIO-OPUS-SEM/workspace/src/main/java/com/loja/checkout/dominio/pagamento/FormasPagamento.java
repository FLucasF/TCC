package com.loja.checkout.dominio.pagamento;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Catalogo das formas de pagamento aceitas. */
@Component
public class FormasPagamento {

    private final Map<String, FormaPagamento> porCodigo = new HashMap<>();

    public FormasPagamento(List<FormaPagamento> formas) {
        for (FormaPagamento forma : formas) {
            porCodigo.put(forma.codigo(), forma);
        }
    }

    public Optional<FormaPagamento> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
