package br.loja.checkout.entrega;

import br.loja.checkout.Carrinho;
import java.math.BigDecimal;

/** Uma opção de entrega. Para criar uma nova, basta implementar e anotar com @Component. */
public interface ModalidadeEntrega {
    String codigo();

    int prazoDias();

    /** Se a opção atende o pedido (limite de peso etc.). */
    default boolean atende(Carrinho carrinho) {
        return true;
    }

    /** Frete já arredondado para centavos. */
    BigDecimal frete(Carrinho carrinho);
}
