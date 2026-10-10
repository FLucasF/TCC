package com.loja.checkout.entrega;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CatalogoEntregas {

    private final Map<String, OpcaoEntrega> opcoes = Map.of(
            "ECONOMICA", new Economica(),
            "EXPRESSA", new Expressa(),
            "RETIRADA_LOJA", new RetiradaLoja(),
            "MOTOBOY", new Motoboy()
    );

    public OpcaoEntrega buscar(String modalidade) {
        return opcoes.get(modalidade);
    }
}
