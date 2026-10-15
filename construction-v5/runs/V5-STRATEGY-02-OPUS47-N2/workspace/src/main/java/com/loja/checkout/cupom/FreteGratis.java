package com.loja.checkout.cupom;

import com.loja.checkout.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class FreteGratis implements Cupom {
    public String codigo() { return "FRETEGRATIS"; }
    public boolean aplicavel(Pedido p) { return true; }
    public BigDecimal desconto(Pedido p, BigDecimal frete) { return frete; }
}
