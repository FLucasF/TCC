package com.loja.checkout.cupom;

import com.loja.checkout.dominio.ItemPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class FreteGratisCupom implements CupomCalculadora {

    @Override
    public String getCodigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal freteAposClube) {
        return freteAposClube;
    }
}
