package com.loja.checkout.seguro;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Seguro contra extravio e roubo: a porcentagem da regiao sobre o valor dos produtos. */
@Component
public class CalculadoraSeguro {

    public BigDecimal seguro(Regiao regiao, BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, regiao.taxaSeguro());
    }
}
