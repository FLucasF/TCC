package com.loja.checkout.entrega;

import java.math.BigDecimal;
import java.util.Map;

public interface ModalidadeEntrega {

    BigDecimal calcularFrete(BigDecimal pesoKg);

    int prazoDias();

    boolean disponivel(BigDecimal pesoKg);

    Map<String, ModalidadeEntrega> REGISTRO = Map.of(
            "ECONOMICA", new Economica(),
            "EXPRESSA", new Expressa(),
            "RETIRADA_LOJA", new RetiradaLoja(),
            "MOTOBOY", new Motoboy()
    );

    static ModalidadeEntrega buscar(String codigo) {
        return codigo == null ? null : REGISTRO.get(codigo);
    }
}
