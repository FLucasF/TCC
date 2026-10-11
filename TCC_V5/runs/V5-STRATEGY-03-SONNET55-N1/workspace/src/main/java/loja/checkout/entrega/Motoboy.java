package loja.checkout.entrega;

import java.math.BigDecimal;
import loja.checkout.dominio.Carrinho;
import loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

@Component
public class Motoboy implements Entrega {

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean atende(Carrinho carrinho) {
        return carrinho.pesoKg().compareTo(new BigDecimal("5")) <= 0;
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return new BigDecimal("18.00");
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
