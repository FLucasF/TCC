package com.loja.checkout.dominio.entrega;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Acha a política de entrega pelo código. Toda implementação registrada entra
 * sozinha no mapa, então uma opção nova de entrega só precisa ser criada como
 * classe — a escolha continua sendo um lookup, nunca uma cadeia de condições.
 */
@Component
public class PoliticaEntregaRegistry {

    private final Map<String, PoliticaEntrega> porCodigo;

    public PoliticaEntregaRegistry(List<PoliticaEntrega> politicas) {
        this.porCodigo = politicas.stream()
                .collect(Collectors.toMap(PoliticaEntrega::codigo, Function.identity()));
    }

    public Optional<PoliticaEntrega> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
