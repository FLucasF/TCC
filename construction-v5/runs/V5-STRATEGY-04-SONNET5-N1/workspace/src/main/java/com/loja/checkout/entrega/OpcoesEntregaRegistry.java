package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OpcoesEntregaRegistry {

    private final Map<String, OpcaoEntrega> porCodigo;

    public OpcoesEntregaRegistry(List<OpcaoEntrega> opcoes) {
        this.porCodigo = opcoes.stream()
                .collect(Collectors.toMap(OpcaoEntrega::codigo, Function.identity()));
    }

    public Optional<OpcaoEntrega> buscar(String codigo) {
        return Optional.ofNullable(porCodigo.get(codigo));
    }
}
