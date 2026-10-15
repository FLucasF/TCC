package loja.checkout.entrega;

import java.math.BigDecimal;
import loja.checkout.Carrinho;
import loja.checkout.Dinheiro;
import org.springframework.stereotype.Component;

@Component
public class EntregaExpressa implements Entrega {
    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public boolean atende(Carrinho carrinho) {
        return true;
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.arredondar(new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(carrinho.pesoKg())));
    }

    @Override
    public int prazoDias() {
        return 2;
    }
}
