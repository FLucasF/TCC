package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public interface FormaPagamento {

    boolean isParcelamentoValido(int parcelas);

    boolean isDisponivel(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    Map<String, FormaPagamento> REGISTRO = Map.of(
            "PIX", new Pix(),
            "CARTAO", new Cartao(),
            "BOLETO", new Boleto()
    );

    static Optional<FormaPagamento> buscar(String codigo) {
        return Optional.ofNullable(REGISTRO.get(codigo));
    }
}
