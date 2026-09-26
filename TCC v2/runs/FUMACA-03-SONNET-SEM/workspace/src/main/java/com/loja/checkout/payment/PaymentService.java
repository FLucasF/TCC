package com.loja.checkout.payment;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.ErroCodigo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final Map<String, FormaPagamentoStrategy> formasPorCodigo;

    public PaymentService(List<FormaPagamentoStrategy> formas) {
        this.formasPorCodigo = formas.stream()
                .collect(Collectors.toMap(FormaPagamentoStrategy::getCodigo, Function.identity()));
    }

    public FormaPagamentoStrategy resolver(String codigo, int parcelas, BigDecimal totalPedido) {
        if (codigo == null || codigo.isBlank()) {
            throw new CheckoutException(ErroCodigo.FORMA_PAGAMENTO_INVALIDA);
        }
        FormaPagamentoStrategy forma = formasPorCodigo.get(codigo);
        if (forma == null) {
            throw new CheckoutException(ErroCodigo.FORMA_PAGAMENTO_INVALIDA);
        }
        if (!forma.parcelasValidas(parcelas)) {
            throw new CheckoutException(ErroCodigo.PARCELAMENTO_INVALIDO);
        }
        if (!forma.disponivelPara(totalPedido)) {
            throw new CheckoutException(ErroCodigo.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        return forma;
    }
}
