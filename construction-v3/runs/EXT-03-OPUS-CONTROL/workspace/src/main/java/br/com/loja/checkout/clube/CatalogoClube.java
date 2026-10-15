package br.com.loja.checkout.clube;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Reune os niveis do clube da loja. */
@Component
public class CatalogoClube {

    private final Map<String, NivelClube> porCodigo;

    public CatalogoClube(List<NivelClube> niveis) {
        this.porCodigo = niveis.stream().collect(Collectors.toMap(
                NivelClube::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public Optional<NivelClube> porCodigo(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
