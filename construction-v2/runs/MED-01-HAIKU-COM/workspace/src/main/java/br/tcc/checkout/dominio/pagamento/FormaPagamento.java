package br.tcc.checkout.dominio.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    String getCodigo();

    boolean ehValida(Integer parcelas, BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, Integer parcelas);
}
