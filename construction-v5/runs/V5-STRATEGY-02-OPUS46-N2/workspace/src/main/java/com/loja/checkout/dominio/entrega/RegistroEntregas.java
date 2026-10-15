package com.loja.checkout.dominio.entrega;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class RegistroEntregas {

    private final Map<String, ModalidadeEntrega> modalidades = new LinkedHashMap<>();

    public RegistroEntregas() {
        registrar(new Economica());
        registrar(new Expressa());
        registrar(new RetiradaLoja());
        registrar(new Motoboy());
    }

    public void registrar(ModalidadeEntrega modalidade) {
        modalidades.put(modalidade.codigo(), modalidade);
    }

    public Optional<ModalidadeEntrega> buscar(String codigo) {
        return Optional.ofNullable(modalidades.get(codigo));
    }
}
