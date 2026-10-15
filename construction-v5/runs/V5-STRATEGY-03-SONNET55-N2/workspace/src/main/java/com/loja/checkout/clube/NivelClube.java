package com.loja.checkout.clube;

import com.loja.checkout.dominio.Opcao;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

public interface NivelClube extends Opcao {

    BigDecimal frete(BigDecimal freteDaModalidade);

    BigDecimal credito(Pedido pedido);

    boolean brinde(Pedido pedido);
}
