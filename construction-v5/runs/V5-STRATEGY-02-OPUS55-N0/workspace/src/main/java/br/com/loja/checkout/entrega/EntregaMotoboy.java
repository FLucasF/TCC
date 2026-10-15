package br.com.loja.checkout.entrega;

import br.com.loja.checkout.calculo.Carrinho;
import br.com.loja.checkout.calculo.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EntregaMotoboy implements ModalidadeEntrega {

    private static final BigDecimal VALOR = Dinheiro.reais("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal calcularFrete(Carrinho carrinho) {
        return VALOR;
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public boolean atende(Carrinho carrinho) {
        return carrinho.pesoKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }
}
