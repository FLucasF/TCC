package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Carrinho;
import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.Item;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Leve3Pague2 implements Cupom {

    public String codigo() {
        return "LEVE3PAGUE2";
    }

    public boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        BigDecimal desconto = carrinho.itens().stream()
                .map(this::valorDasUnidadesGratis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.arredondar(desconto);
    }

    private BigDecimal valorDasUnidadesGratis(Item item) {
        return item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3));
    }
}
