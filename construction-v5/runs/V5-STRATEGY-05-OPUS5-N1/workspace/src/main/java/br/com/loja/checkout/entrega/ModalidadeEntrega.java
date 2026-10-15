package br.com.loja.checkout.entrega;

import br.com.loja.checkout.comum.Codificado;
import java.math.BigDecimal;

/**
 * Uma opção de entrega: quanto cobra, em quanto tempo chega e quais pedidos
 * ela aceita. Transportadora nova = uma classe nova que implementa isto.
 */
public interface ModalidadeEntrega extends Codificado {

    BigDecimal frete(BigDecimal pesoKg);

    int prazoDias();

    default boolean atende(BigDecimal pesoKg) {
        return true;
    }
}
