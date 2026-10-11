package br.com.loja.checkout.pagamento;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class Boleto implements FormaPagamento {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

    @Override
    public String codigo() {
        return "BOLETO";
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        return Cobranca.aVista(totalPedido.add(TARIFA));
    }
}
