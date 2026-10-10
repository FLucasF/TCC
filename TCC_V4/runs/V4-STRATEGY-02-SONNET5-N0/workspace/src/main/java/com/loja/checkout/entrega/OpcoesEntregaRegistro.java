package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OpcoesEntregaRegistro {

    private final Map<String, OpcaoEntrega> opcoesPorCodigo;

    public OpcoesEntregaRegistro(List<OpcaoEntrega> opcoes) {
        this.opcoesPorCodigo = opcoes.stream()
                .collect(Collectors.toMap(OpcaoEntrega::getCodigo, Function.identity()));
    }

    public Optional<OpcaoEntrega> buscar(String codigo) {
        return Optional.ofNullable(opcoesPorCodigo.get(codigo));
    }
}
