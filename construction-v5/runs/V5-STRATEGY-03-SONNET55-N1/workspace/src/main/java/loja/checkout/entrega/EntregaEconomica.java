package loja.checkout.entrega;

import java.math.BigDecimal;
import loja.checkout.dominio.Carrinho;
import loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

@Component
public class EntregaEconomica implements Entrega {

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.arredondar(new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(carrinho.pesoKg())));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
