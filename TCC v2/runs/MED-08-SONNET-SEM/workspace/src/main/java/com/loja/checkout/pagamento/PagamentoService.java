package com.loja.checkout.pagamento;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PagamentoService {

    private final Map<String, FormaPagamento> formasPorCodigo;

    public PagamentoService(List<FormaPagamento> formas) {
        this.formasPorCodigo = formas.stream()
                .collect(Collectors.toUnmodifiableMap(FormaPagamento::codigo, Function.identity()));
    }

    public FormaPagamento buscar(String codigo, int parcelas, BigDecimal totalPedido) {
        if (codigo == null) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        FormaPagamento forma = formasPorCodigo.get(codigo);
        if (forma == null) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        if (!forma.parcelasValidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!forma.disponivelPara(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        return forma;
    }
}
