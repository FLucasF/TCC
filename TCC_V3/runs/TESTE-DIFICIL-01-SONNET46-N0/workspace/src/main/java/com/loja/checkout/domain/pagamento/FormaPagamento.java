package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.clube.BeneficioClube;

import java.math.BigDecimal;

public interface FormaPagamento {

    String getCodigo();

    boolean isParcelasValida(int parcelas, BeneficioClube nivel);

    boolean isDisponivel(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas, BeneficioClube nivel);
}
