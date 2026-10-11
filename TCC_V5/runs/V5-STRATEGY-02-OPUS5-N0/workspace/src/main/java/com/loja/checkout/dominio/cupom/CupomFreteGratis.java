package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.BaseCalculo;
import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * O cliente nao paga o frete: no resumo o frete aparece normalmente e o
 * desconto do cupom fica igual ao valor do frete.
 */
@Component
public class CupomFreteGratis implements PoliticaCupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(BaseCalculo base, BigDecimal frete) {
        return Dinheiro.arredondar(frete);
    }
}
