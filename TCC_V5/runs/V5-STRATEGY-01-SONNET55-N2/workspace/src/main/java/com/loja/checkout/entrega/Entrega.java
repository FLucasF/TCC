package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Opcao;
import java.math.BigDecimal;

public interface Entrega extends Opcao {

    boolean disponivelPara(Carrinho carrinho);

    BigDecimal frete(Carrinho carrinho);

    int prazoDias();
}
