package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Codificado;
import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/**
 * Uma opção de entrega. Para cadastrar uma transportadora nova, crie uma classe {@code @Component}
 * que implemente esta interface (ou estenda {@link FretePorPeso}).
 */
public interface ModalidadeEntrega extends Codificado {

    int prazoDias();

    /** Frete do pedido; o valor é arredondado para centavos por quem chama. */
    BigDecimal calcularFrete(Pedido pedido);

    /** Se esta opção consegue levar o pedido (limite de peso etc.). */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
