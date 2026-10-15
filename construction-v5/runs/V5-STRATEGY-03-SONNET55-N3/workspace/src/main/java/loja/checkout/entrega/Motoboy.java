package loja.checkout.entrega;

import java.math.BigDecimal;
import loja.checkout.Carrinho;
import loja.checkout.Dinheiro;
import org.springframework.stereotype.Component;

@Component
public class Motoboy implements Entrega {
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean atende(Carrinho carrinho) {
        return carrinho.pesoKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.arredondar(new BigDecimal("18.00"));
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
