package com.loja.checkout.seguro;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Seguro contra extravio e roubo: percentual da regiao sobre o valor dos produtos. */
@Component
public class CalculadoraSeguro {

    public BigDecimal calcular(BigDecimal subtotalProdutos, Regiao regiao) {
        return Dinheiro.percentual(subtotalProdutos, regiao.percentualSeguro());
    }
}
