package loja.checkout.cupom;

import java.math.BigDecimal;
import loja.checkout.Carrinho;
import loja.checkout.Codificado;

public interface Cupom extends Codificado {
    boolean aplicavel(Carrinho carrinho);

    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
