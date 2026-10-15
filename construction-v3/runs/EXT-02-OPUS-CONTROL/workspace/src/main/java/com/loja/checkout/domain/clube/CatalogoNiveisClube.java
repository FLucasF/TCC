package com.loja.checkout.domain.clube;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Reune os niveis do clube da loja. */
@Component
public class CatalogoNiveisClube {

    private final List<NivelClube> niveis;

    public CatalogoNiveisClube(List<NivelClube> niveis) {
        this.niveis = List.copyOf(niveis);
    }

    public Optional<NivelClube> porCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return niveis.stream().filter(n -> n.codigo().equals(codigo)).findFirst();
    }
}
