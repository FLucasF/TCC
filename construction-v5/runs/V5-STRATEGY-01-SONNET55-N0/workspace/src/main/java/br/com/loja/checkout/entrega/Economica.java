package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Economica implements ModalidadeEntrega {

    public String codigo() {
        return "ECONOMICA";
    }

    public int prazoDias() {
        return 7;
    }

    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.arredondar(new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(carrinho.pesoKg())));
    }
}
