package com.loja.checkout.dominio.entrega;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Catalogo das opcoes de entrega disponiveis na loja. */
@Component
public class ModalidadesEntrega {

    private final Map<String, ModalidadeEntrega> porCodigo = new LinkedHashMap<>();

    public ModalidadesEntrega(List<ModalidadeEntrega> modalidades) {
        modalidades.forEach(modalidade -> porCodigo.put(modalidade.codigo(), modalidade));
    }

    public Optional<ModalidadeEntrega> porCodigo(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
