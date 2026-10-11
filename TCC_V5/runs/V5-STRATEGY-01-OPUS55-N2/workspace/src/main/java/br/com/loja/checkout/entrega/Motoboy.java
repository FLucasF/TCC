package br.com.loja.checkout.entrega;

import br.com.loja.checkout.resumo.Carrinho;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");
    private static final BigDecimal VALOR = new BigDecimal("18.00");

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
        return VALOR;
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
