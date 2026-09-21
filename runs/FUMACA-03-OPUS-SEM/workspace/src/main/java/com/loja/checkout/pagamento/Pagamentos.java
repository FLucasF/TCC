package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.ErroCheckout;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Catalogo das formas de pagamento aceitas pela loja. */
@Component
public class Pagamentos {

    private final Map<String, FormaPagamento> porCodigo = new HashMap<>();

    public Pagamentos(List<FormaPagamento> formas) {
        for (FormaPagamento forma : formas) {
            porCodigo.put(forma.codigo(), forma);
        }
    }

    /** Resolve o codigo enviado pelo site, confere o parcelamento e a disponibilidade. */
    public FormaPagamento resolver(String codigo, int parcelas, BigDecimal totalPedido) {
        FormaPagamento forma = codigo == null ? null : porCodigo.get(codigo);
        if (forma == null) {
            throw new ErroCheckout(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        if (!forma.aceitaParcelas(parcelas)) {
            throw new ErroCheckout(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!forma.atende(totalPedido)) {
            throw new ErroCheckout(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        return forma;
    }
}
