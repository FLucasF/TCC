package com.loja.checkout.cupom;

import com.loja.checkout.Carrinho;
import com.loja.checkout.Codificado;
import java.math.BigDecimal;

public interface Cupom extends Codificado {
    boolean aplicavel(Carrinho carrinho);

    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
