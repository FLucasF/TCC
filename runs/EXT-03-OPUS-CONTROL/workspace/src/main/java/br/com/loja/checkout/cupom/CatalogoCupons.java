package br.com.loja.checkout.cupom;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Reune os cupons que estao valendo. */
@Component
public class CatalogoCupons {

    private final Map<String, Cupom> porCodigo;

    public CatalogoCupons(List<Cupom> cupons) {
        this.porCodigo = cupons.stream().collect(Collectors.toMap(
                Cupom::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public Optional<Cupom> porCodigo(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
