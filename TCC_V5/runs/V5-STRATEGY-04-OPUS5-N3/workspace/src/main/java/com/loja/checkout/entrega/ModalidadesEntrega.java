package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Registro;

import java.util.List;
import java.util.Optional;

/** As opções de entrega disponíveis hoje. */
public final class ModalidadesEntrega {

    private static final List<ModalidadeEntrega> CASOS = List.of(
            new FretePorPeso("ECONOMICA", "12.00", "2.00", 7),
            new FretePorPeso("EXPRESSA", "25.00", "4.50", 2),
            new RetiradaLoja(),
            new Motoboy());

    private ModalidadesEntrega() {
    }

    public static Optional<ModalidadeEntrega> porCodigo(String codigo) {
        return Registro.busca(CASOS, ModalidadeEntrega::codigo, codigo);
    }
}
