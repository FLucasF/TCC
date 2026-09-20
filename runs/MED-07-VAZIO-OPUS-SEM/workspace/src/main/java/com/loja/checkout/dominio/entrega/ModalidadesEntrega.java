package com.loja.checkout.dominio.entrega;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Catalogo das opcoes de entrega disponiveis na loja. */
@Component
public class ModalidadesEntrega {

    private final Map<String, ModalidadeEntrega> porCodigo = new HashMap<>();

    public ModalidadesEntrega(List<ModalidadeEntrega> modalidades) {
        for (ModalidadeEntrega modalidade : modalidades) {
            porCodigo.put(modalidade.codigo(), modalidade);
        }
    }

    public Optional<ModalidadeEntrega> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
