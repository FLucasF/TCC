package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Carrinho;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MotoboyEntrega implements ModalidadeEntrega {

    private static final BigDecimal CUSTO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal custo(Carrinho carrinho) {
        return CUSTO;
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public boolean disponivel(Carrinho carrinho) {
        return carrinho.pesoTotalKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }
}
