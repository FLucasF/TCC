package com.loja.checkout.pagamento;

import com.loja.checkout.erro.CheckoutException;
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
                .collect(Collectors.toMap(FormaPagamento::codigo, Function.identity()));
    }

    public ResultadoPagamento calcular(String codigo, int parcelas, BigDecimal totalPedido) {
        FormaPagamento forma = formasPorCodigo.get(codigo);
        if (forma == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        if (!forma.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        if (!forma.disponivelPara(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        return forma.calcular(totalPedido, parcelas);
    }
}
