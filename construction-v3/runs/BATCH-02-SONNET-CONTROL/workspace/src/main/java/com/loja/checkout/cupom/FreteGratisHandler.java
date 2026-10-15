package com.loja.checkout.cupom;

import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FreteGratisHandler implements CupomHandler {

    @Override
    public String getCodigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(DadosPedido pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(DadosPedido pedido) {
        return Dinheiro.arredondar(pedido.frete());
    }
}
