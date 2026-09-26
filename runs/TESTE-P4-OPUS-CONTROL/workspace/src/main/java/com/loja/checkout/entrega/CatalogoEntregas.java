package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Reune todas as opcoes de entrega registradas na aplicacao. */
@Component
public class CatalogoEntregas {

    private final Map<String, ModalidadeEntrega> porCodigo = new LinkedHashMap<>();

    public CatalogoEntregas(List<ModalidadeEntrega> modalidades) {
        modalidades.forEach(m -> porCodigo.put(m.codigo(), m));
    }

    public Optional<ModalidadeEntrega> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
