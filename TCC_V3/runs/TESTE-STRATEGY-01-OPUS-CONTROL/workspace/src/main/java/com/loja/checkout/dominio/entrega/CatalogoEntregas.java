package com.loja.checkout.dominio.entrega;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Reune todas as modalidades de entrega registradas na aplicacao. */
@Component
public class CatalogoEntregas {

    private final Map<String, ModalidadeEntrega> porCodigo = new LinkedHashMap<>();

    public CatalogoEntregas(List<ModalidadeEntrega> modalidades) {
        modalidades.forEach(modalidade -> porCodigo.put(modalidade.codigo(), modalidade));
    }

    public Optional<ModalidadeEntrega> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
