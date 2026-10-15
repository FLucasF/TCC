package com.loja.checkout.domain.entrega;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Reune todas as modalidades de entrega registradas na aplicacao. */
@Component
public class CatalogoModalidades {

    private final List<ModalidadeEntrega> modalidades;

    public CatalogoModalidades(List<ModalidadeEntrega> modalidades) {
        this.modalidades = List.copyOf(modalidades);
    }

    public Optional<ModalidadeEntrega> porCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return modalidades.stream().filter(m -> m.codigo().equals(codigo)).findFirst();
    }
}
