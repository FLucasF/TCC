package com.loja.checkout.pagamento;

import java.math.BigDecimal;
import java.util.Map;

public interface FormaPagamento {

    boolean parcelasValidas(int parcelas);

    boolean disponivel(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    Map<String, FormaPagamento> REGISTRO = Map.of(
            "PIX", new Pix(),
            "CARTAO", new Cartao(),
            "BOLETO", new Boleto()
    );

    static FormaPagamento buscar(String codigo) {
        return codigo == null ? null : REGISTRO.get(codigo);
    }
}
