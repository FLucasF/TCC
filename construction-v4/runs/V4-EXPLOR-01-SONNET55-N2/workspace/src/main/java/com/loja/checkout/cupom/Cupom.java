package com.loja.checkout.cupom;

import com.loja.checkout.Carrinho;
import com.loja.checkout.Identificavel;
import java.math.BigDecimal;

public interface Cupom extends Identificavel {
    boolean aplicavel(Carrinho carrinho);

    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
