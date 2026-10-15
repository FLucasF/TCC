package br.com.loja.checkout.entrega;

import br.com.loja.checkout.Carrinho;
import br.com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Economica implements ModalidadeEntrega {

    public String codigo() {
        return "ECONOMICA";
    }

    public boolean atende(Carrinho carrinho) {
        return true;
    }

    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.arredondar(new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(carrinho.pesoKg())));
    }

    public int prazoDias() {
        return 7;
    }
}
