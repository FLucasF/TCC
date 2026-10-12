package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ModalidadeEntregaRegistry {

    private final Map<String, ModalidadeEntrega> modalidades = Map.of(
            "ECONOMICA", new EntregaEconomica(),
            "EXPRESSA", new EntregaExpressa(),
            "RETIRADA_LOJA", new EntregaRetiradaLoja(),
            "MOTOBOY", new EntregaMotoboy()
    );

    public ModalidadeEntrega buscar(String codigo) {
        return modalidades.get(codigo);
    }
}
