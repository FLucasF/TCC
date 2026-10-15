package com.loja.resumo.entrega;

import com.loja.resumo.dominio.Pedido;

import java.math.BigDecimal;
import java.util.Optional;

public interface ModalidadeEntrega {

    BigDecimal calcularFrete(Pedido pedido);

    int prazoDias();

    boolean disponivelPara(Pedido pedido);

    static Optional<ModalidadeEntrega> encontrar(String nome) {
        if (nome == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(ModalidadeEntregaPadrao.valueOf(nome));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
