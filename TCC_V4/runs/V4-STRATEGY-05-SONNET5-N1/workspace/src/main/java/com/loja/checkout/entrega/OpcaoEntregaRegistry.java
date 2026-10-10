package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OpcaoEntregaRegistry {

    private final Map<String, OpcaoEntrega> opcoesPorCodigo;

    public OpcaoEntregaRegistry(List<OpcaoEntrega> opcoes) {
        this.opcoesPorCodigo = opcoes.stream()
                .collect(Collectors.toMap(OpcaoEntrega::codigo, Function.identity()));
    }

    public OpcaoEntrega buscar(String codigo) {
        return opcoesPorCodigo.get(codigo);
    }
}
