package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Opcao;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

public interface Cupom extends Opcao {

    boolean aplicavel(Pedido pedido);

    BigDecimal desconto(Pedido pedido, BigDecimal frete);
}
