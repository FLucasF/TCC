package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Codificado;
import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** Um nível do clube da loja. Para criar um nível novo, crie uma classe {@code @Component} que implemente esta interface. */
public interface NivelClube extends Codificado {

    /** Percentual dos produtos devolvido como crédito para a próxima compra (ex.: 0.02 para 2%). */
    default BigDecimal percentualCredito() {
        return BigDecimal.ZERO;
    }

    default boolean isentaFrete() {
        return false;
    }

    default boolean ganhaBrinde(Pedido pedido) {
        return false;
    }
}
