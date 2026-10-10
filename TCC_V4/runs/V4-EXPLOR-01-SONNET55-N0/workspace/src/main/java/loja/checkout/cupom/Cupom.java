package loja.checkout.cupom;

import java.math.BigDecimal;
import loja.checkout.dominio.Pedido;

public interface Cupom {
    String codigo();

    boolean aplicavel(Pedido pedido);

    BigDecimal desconto(Pedido pedido, BigDecimal frete);
}
