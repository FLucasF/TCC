package loja.checkout.pagamento;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
class Boleto implements FormaPagamento {
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");
    private static final BigDecimal TARIFA = new BigDecimal("3.49");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(BigDecimal total) {
        return total.compareTo(LIMITE) <= 0;
    }

    @Override
    public Cobranca cobrar(BigDecimal total, int parcelas) {
        BigDecimal valor = total.add(TARIFA);
        return new Cobranca(valor, valor);
    }
}
