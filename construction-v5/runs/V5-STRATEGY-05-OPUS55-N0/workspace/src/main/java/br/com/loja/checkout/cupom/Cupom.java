package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Codificado;
import java.math.BigDecimal;

/** Um cupom de desconto. Para criar uma promoção nova, crie uma classe {@code @Component} que implemente esta interface. */
public interface Cupom extends Codificado {

    /** Se o pedido cumpre a condição do cupom (ex.: valor mínimo). */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Valor do desconto; é arredondado para centavos por quem chama. */
    BigDecimal desconto(ContextoCupom contexto);
}
