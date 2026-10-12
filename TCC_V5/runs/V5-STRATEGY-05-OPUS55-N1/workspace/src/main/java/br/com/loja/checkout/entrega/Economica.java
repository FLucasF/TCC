package br.com.loja.checkout.entrega;

import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.pedido.Carrinho;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class Economica implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.centavos(new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(carrinho.pesoKg())));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
