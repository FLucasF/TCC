package com.loja.checkout.cupom;

import com.loja.checkout.model.Pedido;
import com.loja.checkout.model.Resumo;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component("FRETEGRATIS")
public class FreteGratis implements Cupom {
    @Override
    public BigDecimal calcularDesconto(Pedido pedido, Resumo resumo) {
        return resumo.getFrete();
    }

    @Override
    public void validarAplicabilidade(Pedido pedido) {
    }
}
