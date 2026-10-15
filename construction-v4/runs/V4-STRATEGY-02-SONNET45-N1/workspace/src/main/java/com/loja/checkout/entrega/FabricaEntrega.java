package com.loja.checkout.entrega;

import com.loja.checkout.model.enums.ModalidadeEntrega;
import java.util.Map;

public class FabricaEntrega {
    private static final Map<ModalidadeEntrega, Entrega> ENTREGAS = Map.of(
        ModalidadeEntrega.ECONOMICA, new EntregaEconomica(),
        ModalidadeEntrega.EXPRESSA, new EntregaExpressa(),
        ModalidadeEntrega.RETIRADA_LOJA, new EntregaRetiradaLoja(),
        ModalidadeEntrega.MOTOBOY, new EntregaMotoboy()
    );

    public static Entrega criar(ModalidadeEntrega modalidade) {
        return ENTREGAS.get(modalidade);
    }
}
