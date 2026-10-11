package loja.checkout.entrega;

import java.math.BigDecimal;
import loja.checkout.Carrinho;
import loja.checkout.Codificado;

public interface ModalidadeEntrega extends Codificado {
    boolean atende(Carrinho carrinho);

    BigDecimal frete(Carrinho carrinho);

    int prazoDias();
}
