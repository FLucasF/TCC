package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Opcao;
import java.math.BigDecimal;

public interface Cupom extends Opcao {

    boolean aplicavelA(Carrinho carrinho);

    /** O frete já considera as vantagens do clube, pois é o que aparece no resumo. */
    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
