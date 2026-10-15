package com.loja.checkout.dominio.pagamento;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Catalogo das formas de pagamento aceitas. */
@Component
public class FormasPagamento {

    private final Map<String, FormaPagamento> porCodigo = new LinkedHashMap<>();

    public FormasPagamento(List<FormaPagamento> formas) {
        formas.forEach(forma -> porCodigo.put(forma.codigo(), forma));
    }

    public Optional<FormaPagamento> porCodigo(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
