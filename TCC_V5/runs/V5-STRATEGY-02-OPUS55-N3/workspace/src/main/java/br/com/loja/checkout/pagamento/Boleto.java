package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

    public String codigo() {
        return "BOLETO";
    }

    public boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    public boolean atende(BigDecimal totalPedido) {
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }

    public Pagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.add(TARIFA);
        return new Pagamento(totalFinal, totalFinal);
    }
}
