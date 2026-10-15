package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Codificado;
import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

public interface Cupom extends Codificado {

    /** Pedido sem cupom: não fica no catálogo, só evita tratar a ausência no fluxo. */
    Cupom NENHUM = new Cupom() {
        public String codigo() {
            return "";
        }

        public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            return Dinheiro.ZERO;
        }
    };

    default boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    /** @param frete frete que aparece no resumo (já com a vantagem do clube). */
    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
