package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Reune todas as opcoes de entrega cadastradas na aplicacao. */
@Component
public class CatalogoEntregas {

    private final Map<String, ModalidadeEntrega> porCodigo;

    public CatalogoEntregas(List<ModalidadeEntrega> modalidades) {
        Map<String, ModalidadeEntrega> mapa = new LinkedHashMap<>();
        modalidades.forEach(m -> mapa.put(m.codigo(), m));
        this.porCodigo = Map.copyOf(mapa);
    }

    public Optional<ModalidadeEntrega> buscar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(porCodigo.get(codigo));
    }
}
