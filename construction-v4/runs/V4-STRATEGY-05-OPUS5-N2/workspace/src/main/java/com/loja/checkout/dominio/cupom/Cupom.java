package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Erro;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Uma promocao: tem sua regra de desconto e sua condicao de uso. So vale um cupom
 * por pedido. Promocao nova e uma implementacao nova registrada no catalogo.
 */
public interface Cupom {

    Catalogo<Cupom> CATALOGO = Catalogo.de(Erro.CUPOM_INVALIDO, Map.of(
            "BEMVINDO10", new DescontoPercentualNosProdutos(new BigDecimal("10")),
            "MENOS50", new DescontoFixoNosProdutos(new BigDecimal("50.00"), new BigDecimal("300.00")),
            "FRETEGRATIS", new FreteGratis(),
            "LEVE3PAGUE2", new Leve3Pague2()));

    /** Nenhum cupom: o pedido fica sem desconto. */
    Cupom NENHUM = contexto -> Dinheiro.ZERO;

    BigDecimal desconto(ContextoCupom contexto);

    /** Vale para qualquer pedido, salvo quando a promocao disser o contrario. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }
}
