package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Identificavel;
import java.math.BigDecimal;

public interface Cupom extends Identificavel {

    boolean aplicavel(ContextoCupom contexto);

    BigDecimal desconto(ContextoCupom contexto);
}
