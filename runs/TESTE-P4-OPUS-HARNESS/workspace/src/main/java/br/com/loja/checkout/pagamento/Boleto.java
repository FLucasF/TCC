package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Boleto extends AVista {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    BigDecimal valorFinal(BigDecimal totalPedido) {
        return Dinheiro.centavos(totalPedido.add(TARIFA));
    }

    @Override
    public boolean atende(BigDecimal totalAntesDoImposto) {
        return totalAntesDoImposto.compareTo(TOTAL_MAXIMO) <= 0;
    }
}
