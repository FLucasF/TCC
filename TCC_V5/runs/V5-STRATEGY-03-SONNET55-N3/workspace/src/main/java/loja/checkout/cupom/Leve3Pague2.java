package loja.checkout.cupom;

import java.math.BigDecimal;
import loja.checkout.Carrinho;
import loja.checkout.Dinheiro;
import loja.checkout.Item;
import org.springframework.stereotype.Component;

@Component
public class Leve3Pague2 implements Cupom {
    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return carrinho.itens().stream()
                .map(this::valorDosGratis)
                .reduce(Dinheiro.ZERO, BigDecimal::add);
    }

    private BigDecimal valorDosGratis(Item item) {
        return Dinheiro.arredondar(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)));
    }
}
