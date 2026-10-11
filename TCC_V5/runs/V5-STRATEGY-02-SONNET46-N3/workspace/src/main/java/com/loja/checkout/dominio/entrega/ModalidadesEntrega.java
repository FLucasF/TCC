package com.loja.checkout.dominio.entrega;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class ModalidadesEntrega {
    private final Map<String, ModalidadeEntrega> registro = Map.of(
        "ECONOMICA",     new EntregaEconomica(),
        "EXPRESSA",      new EntregaExpressa(),
        "RETIRADA_LOJA", new EntregaRetiradaLoja(),
        "MOTOBOY",       new EntregaMotoboy()
    );

    public Optional<ModalidadeEntrega> encontrar(String codigo) {
        return Optional.ofNullable(registro.get(codigo));
    }
}
