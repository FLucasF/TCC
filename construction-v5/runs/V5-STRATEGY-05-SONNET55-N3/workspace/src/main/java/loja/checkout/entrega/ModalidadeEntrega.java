package loja.checkout.entrega;

import java.math.BigDecimal;

import loja.checkout.Codificado;

public interface ModalidadeEntrega extends Codificado {

    /** Se a opção atende um pedido com este peso. */
    default boolean atende(BigDecimal pesoKg) {
        return true;
    }

    BigDecimal frete(BigDecimal pesoKg);

    int prazoDias();
}
