package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.AjustePagamento;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public interface FormaPagamento {
    boolean parcelasValidas(int parcelas);
    boolean aceita(BigDecimal totalPedido);
    AjustePagamento calcular(BigDecimal totalPedido, int parcelas);

    Map<String, FormaPagamento> REGISTRO = carregar();

    private static Map<String, FormaPagamento> carregar() {
        Map<String, FormaPagamento> m = new LinkedHashMap<>();
        m.put("PIX", new Pix());
        m.put("CARTAO", new Cartao());
        m.put("BOLETO", new Boleto());
        return Map.copyOf(m);
    }

    static Optional<FormaPagamento> de(String codigo) {
        if (codigo == null) return Optional.empty();
        return Optional.ofNullable(REGISTRO.get(codigo));
    }
}
