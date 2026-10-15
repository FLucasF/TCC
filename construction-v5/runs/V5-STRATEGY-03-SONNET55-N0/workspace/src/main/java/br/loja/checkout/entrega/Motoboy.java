package br.loja.checkout.entrega;

import br.loja.checkout.Carrinho;
import br.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Motoboy implements ModalidadeEntrega {
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    public String codigo() {
        return "MOTOBOY";
    }

    public int prazoDias() {
        return 0;
    }

    public boolean atende(Carrinho carrinho) {
        return carrinho.pesoKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }

    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.of("18.00");
    }
}
