package com.loja.checkout.clube;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Opcao;
import java.math.BigDecimal;

public interface NivelClube extends Opcao {

    BigDecimal freteDevido(BigDecimal frete);

    BigDecimal creditoProximaCompra(Carrinho carrinho);

    boolean brinde(Carrinho carrinho);
}
