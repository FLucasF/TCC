package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Erro;
import com.loja.checkout.dominio.PedidoRecusadoException;

import java.math.BigDecimal;
import java.util.List;

/** Os cupons que valem hoje. Promocao nova entra nesta lista. */
public final class Cupons {

    private static final Catalogo<Cupom> CATALOGO = Catalogo.de(List.of(
            new Bemvindo10(),
            new Menos50(),
            new FreteGratis(),
            new Leve3Pague2()
    ), Cupom::codigo);

    private Cupons() {
    }

    /**
     * Desconto do codigo informado. Sem cupom, desconto zero; codigo
     * desconhecido ou condicao nao cumprida recusam o pedido.
     */
    public static BigDecimal desconto(String codigo, ContextoCupom contexto) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = CATALOGO.buscar(codigo)
                .orElseThrow(() -> new PedidoRecusadoException(Erro.CUPOM_INVALIDO));
        if (!cupom.aplicavel(contexto)) {
            throw new PedidoRecusadoException(Erro.CUPOM_NAO_APLICAVEL);
        }
        return cupom.desconto(contexto);
    }
}
