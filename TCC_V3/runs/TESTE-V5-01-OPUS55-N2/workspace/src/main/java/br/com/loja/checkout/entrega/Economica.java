package br.com.loja.checkout.entrega;

import br.com.loja.checkout.Carrinho;
import br.com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Economica implements ModalidadeEntrega {

    private static final BigDecimal BASE = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.centavos(BASE.add(POR_KG.multiply(carrinho.pesoKg())));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
