package com.loja.checkout.dominio.pagamento;

import java.util.List;
import java.util.Optional;

public final class Pagamentos {
    private static final List<Pagamento> TODOS = List.of(new Pix(), new Cartao(), new Boleto());

    private Pagamentos() {}

    public static Optional<Pagamento> buscar(String codigo) {
        if (codigo == null) return Optional.empty();
        return TODOS.stream().filter(p -> p.codigo().equals(codigo)).findFirst();
    }
}
