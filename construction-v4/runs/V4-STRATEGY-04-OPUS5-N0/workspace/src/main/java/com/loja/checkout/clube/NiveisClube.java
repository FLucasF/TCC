package com.loja.checkout.clube;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Catalogo dos niveis do clube da loja. */
@Component
public class NiveisClube {

    private final Map<String, NivelClube> porCodigo = new LinkedHashMap<>();

    public NiveisClube(List<NivelClube> niveis) {
        for (NivelClube nivel : niveis) {
            porCodigo.put(nivel.codigo(), nivel);
        }
    }

    public Optional<NivelClube> porCodigo(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
