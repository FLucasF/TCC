package com.loja.checkout.modalidade;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class Modalidades {
    private final Map<String, Modalidade> porCodigo;

    public Modalidades(List<Modalidade> modalidades) {
        this.porCodigo = modalidades.stream().collect(Collectors.toMap(Modalidade::codigo, Function.identity()));
    }

    public Optional<Modalidade> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
