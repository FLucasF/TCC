package br.com.loja.checkout.entrega;

import br.com.loja.checkout.catalogo.Codificado;
import java.math.BigDecimal;

/**
 * Uma opcao de entrega. Cada parceria nova entra como uma implementacao desta
 * interface, com seu jeito de cobrar, seu prazo e sua limitacao.
 */
public interface ModalidadeEntrega extends Codificado {

    /** Frete em centavos, para o peso do pedido. */
    BigDecimal frete(BigDecimal pesoKg);

    int prazoDias();

    /** Se a modalidade consegue levar este pedido. */
    default boolean atende(BigDecimal pesoKg) {
        return true;
    }
}
