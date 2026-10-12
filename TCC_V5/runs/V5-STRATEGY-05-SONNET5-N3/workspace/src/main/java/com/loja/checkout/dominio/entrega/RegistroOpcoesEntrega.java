package com.loja.checkout.dominio.entrega;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RegistroOpcoesEntrega {

    private final Map<String, OpcaoEntrega> opcoesPorCodigo;

    public RegistroOpcoesEntrega(List<OpcaoEntrega> opcoes) {
        this.opcoesPorCodigo = opcoes.stream()
                .collect(Collectors.toUnmodifiableMap(OpcaoEntrega::codigo, Function.identity()));
    }

    public Optional<OpcaoEntrega> buscar(String codigo) {
        return Optional.ofNullable(opcoesPorCodigo.get(codigo));
    }
}
