package com.loja.checkout.domain.entrega;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class RegistroEntregas {

    private final Map<String, ModalidadeEntrega> modalidades;

    public RegistroEntregas() {
        this.modalidades = Map.of(
                "ECONOMICA", new Economica(),
                "EXPRESSA", new Expressa(),
                "RETIRADA_LOJA", new RetiradaLoja(),
                "MOTOBOY", new Motoboy()
        );
    }

    public Optional<ModalidadeEntrega> buscar(String codigo) {
        return Optional.ofNullable(modalidades.get(codigo));
    }
}
