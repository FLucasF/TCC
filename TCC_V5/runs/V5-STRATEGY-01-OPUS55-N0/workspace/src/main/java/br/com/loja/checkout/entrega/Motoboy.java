package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.reais("18.00");
    }

    @Override
    public boolean atende(Carrinho carrinho) {
        return carrinho.pesoTotalKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }
}
