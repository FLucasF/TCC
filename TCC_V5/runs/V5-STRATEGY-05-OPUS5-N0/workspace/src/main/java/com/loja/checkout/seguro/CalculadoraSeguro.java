package com.loja.checkout.seguro;

import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Seguro contra extravio e roubo: percentual da regiao sobre o valor dos
 * produtos, sem desconto e sem frete.
 */
@Component
public class CalculadoraSeguro {

    public BigDecimal calcular(Pedido pedido) {
        return Dinheiro.percentual(pedido.subtotalProdutos(), pedido.regiao().percentualSeguro());
    }
}
