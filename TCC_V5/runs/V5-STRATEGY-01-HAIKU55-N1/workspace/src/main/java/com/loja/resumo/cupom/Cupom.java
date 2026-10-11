package com.loja.resumo.cupom;

import com.loja.resumo.Codigado;
import com.loja.resumo.Contexto;
import java.math.BigDecimal;

public interface Cupom extends Codigado {

    BigDecimal desconto(Contexto contexto);

    default void verificarAplicavel(Contexto contexto) {
    }
}
