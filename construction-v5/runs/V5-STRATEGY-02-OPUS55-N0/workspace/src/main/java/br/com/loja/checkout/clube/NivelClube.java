package br.com.loja.checkout.clube;

import br.com.loja.checkout.calculo.Carrinho;
import br.com.loja.checkout.catalogo.Codificado;
import java.math.BigDecimal;

/**
 * Um nível do clube da loja e suas vantagens. Para criar um nível novo, crie um
 * {@code @Component} que implemente esta interface.
 */
public interface NivelClube extends Codificado {

    /** Porcentagem dos produtos devolvida como crédito para a próxima compra (ex.: 2 para 2%). */
    default BigDecimal porcentagemCredito() {
        return BigDecimal.ZERO;
    }

    default boolean isentaFrete() {
        return false;
    }

    default boolean ganhaBrinde(Carrinho carrinho) {
        return false;
    }
}
