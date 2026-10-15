package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Codificavel;
import java.math.BigDecimal;

/**
 * Uma opção de entrega. Para oferecer uma transportadora nova, basta uma
 * classe nova com @Component: o catálogo a encontra sozinho.
 */
public interface ModalidadeEntrega extends Codificavel {

    /** Se a opção atende um pedido com esse peso. */
    default boolean atende(BigDecimal pesoKg) {
        return true;
    }

    /** O valor pode vir sem arredondar: quem monta o resumo arredonda. */
    Frete calcular(BigDecimal pesoKg);
}
