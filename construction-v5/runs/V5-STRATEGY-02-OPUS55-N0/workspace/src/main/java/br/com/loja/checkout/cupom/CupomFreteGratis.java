package br.com.loja.checkout.cupom;

import br.com.loja.checkout.calculo.Carrinho;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** O frete aparece normalmente no resumo e o desconto é igual a ele. */
@Component
public class CupomFreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal calcularDesconto(Carrinho carrinho, BigDecimal frete) {
        return frete;
    }
}
