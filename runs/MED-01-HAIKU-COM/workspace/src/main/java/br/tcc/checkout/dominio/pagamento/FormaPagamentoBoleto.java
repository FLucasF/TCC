package br.tcc.checkout.dominio.pagamento;

import br.tcc.checkout.util.Arredondador;
import java.math.BigDecimal;

public class FormaPagamentoBoleto implements FormaPagamento {
    @Override
    public String getCodigo() {
        return "BOLETO";
    }

    @Override
    public boolean ehValida(Integer parcelas, BigDecimal totalPedido) {
        int p = parcelas != null ? parcelas : 1;
        return p == 1 && totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, Integer parcelas) {
        BigDecimal tarifa = new BigDecimal("3.49");
        BigDecimal totalFinal = totalPedido.add(tarifa);
        totalFinal = Arredondador.arredondarParaCentavos(totalFinal);

        return new ResultadoPagamento(totalFinal, totalFinal, tarifa);
    }
}
