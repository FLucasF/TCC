package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Leve3Pague2 implements Cupom {

    public String codigo() {
        return "LEVE3PAGUE2";
    }

    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item item : carrinho.itens()) {
            int gratis = item.quantidade() / 3;
            total = total.add(Dinheiro.arredondar(item.precoUnitario().multiply(BigDecimal.valueOf(gratis))));
        }
        return Dinheiro.arredondar(total);
    }
}
