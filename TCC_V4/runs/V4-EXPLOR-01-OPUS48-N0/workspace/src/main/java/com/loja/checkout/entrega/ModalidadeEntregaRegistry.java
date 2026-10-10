package com.loja.checkout.entrega;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Descobre todas as modalidades de entrega registradas e as resolve por código. */
@Component
public class ModalidadeEntregaRegistry {

    private final Map<String, ModalidadeEntrega> porCodigo;

    public ModalidadeEntregaRegistry(List<ModalidadeEntrega> modalidades) {
        this.porCodigo = modalidades.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
    }

    /** Busca a modalidade pelo código; vazio quando o código não existe. */
    public Optional<ModalidadeEntrega> buscar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(porCodigo.get(codigo));
    }
}
