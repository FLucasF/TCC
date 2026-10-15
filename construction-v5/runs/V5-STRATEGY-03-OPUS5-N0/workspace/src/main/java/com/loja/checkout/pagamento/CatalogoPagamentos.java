package com.loja.checkout.pagamento;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Reune as formas de pagamento aceitas pela loja. */
@Component
public class CatalogoPagamentos {

    private final Map<String, FormaPagamento> porCodigo = new LinkedHashMap<>();

    public CatalogoPagamentos(List<FormaPagamento> formas) {
        formas.forEach(forma -> porCodigo.put(forma.codigo(), forma));
    }

    public Optional<FormaPagamento> porCodigo(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
