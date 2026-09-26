package br.com.loja.checkout.pagamento;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Reune as formas de pagamento aceitas. */
@Component
public class CatalogoPagamentos {

    private final Map<String, FormaPagamento> porCodigo;

    public CatalogoPagamentos(List<FormaPagamento> formas) {
        this.porCodigo = formas.stream().collect(Collectors.toMap(
                FormaPagamento::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public Optional<FormaPagamento> porCodigo(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
