package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** Novo cupom = nova classe @Component implementando esta interface. */
public interface Cupom {

    String codigo();

    boolean aplicavel(Pedido pedido);

    /** @param frete frete já calculado (após benefícios do clube) */
    BigDecimal desconto(Pedido pedido, BigDecimal frete);
}
