package com.loja.dominio;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum SituacaoPedido {
    AGUARDANDO_PAGAMENTO,
    PAGO,
    ENVIADO,
    ENTREGUE,
    CANCELADO;

    private static final Map<SituacaoPedido, Set<SituacaoPedido>> TRANSICOES = new EnumMap<>(SituacaoPedido.class);

    static {
        TRANSICOES.put(AGUARDANDO_PAGAMENTO, EnumSet.of(PAGO, CANCELADO));
        TRANSICOES.put(PAGO, EnumSet.of(ENVIADO, CANCELADO));
        TRANSICOES.put(ENVIADO, EnumSet.of(ENTREGUE));
        TRANSICOES.put(ENTREGUE, EnumSet.noneOf(SituacaoPedido.class));
        TRANSICOES.put(CANCELADO, EnumSet.noneOf(SituacaoPedido.class));
    }

    public boolean permite(SituacaoPedido destino) {
        return TRANSICOES.get(this).contains(destino);
    }
}
