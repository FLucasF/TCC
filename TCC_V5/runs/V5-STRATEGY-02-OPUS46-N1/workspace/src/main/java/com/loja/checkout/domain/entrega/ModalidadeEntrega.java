package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public interface ModalidadeEntrega {

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    int getPrazoDias();

    boolean isDisponivel(BigDecimal pesoTotalKg);

    String getCodigo();

    Map<String, ModalidadeEntrega> REGISTRO = Map.of(
            "ECONOMICA", new Economica(),
            "EXPRESSA", new Expressa(),
            "RETIRADA_LOJA", new RetiradaLoja(),
            "MOTOBOY", new Motoboy()
    );

    static Optional<ModalidadeEntrega> buscar(String codigo) {
        return Optional.ofNullable(REGISTRO.get(codigo));
    }
}
