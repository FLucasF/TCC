package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Reune todas as modalidades de entrega cadastradas na aplicacao. */
@Component
public class CatalogoEntregas {

    private final Map<String, ModalidadeEntrega> porCodigo = new LinkedHashMap<>();

    public CatalogoEntregas(List<ModalidadeEntrega> modalidades) {
        for (ModalidadeEntrega modalidade : modalidades) {
            porCodigo.put(modalidade.codigo(), modalidade);
        }
    }

    public Optional<ModalidadeEntrega> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
