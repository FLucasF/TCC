package com.loja.checkout.pagamento;

import com.loja.checkout.api.CodigoErro;
import com.loja.checkout.api.ErroPedidoException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PagamentoService {

    private final Map<String, PagamentoStrategy> estrategiasPorCodigo;

    public PagamentoService(List<PagamentoStrategy> estrategias) {
        this.estrategiasPorCodigo = estrategias.stream()
                .collect(Collectors.toMap(PagamentoStrategy::codigo, Function.identity()));
    }

    public PagamentoStrategy buscar(String codigo) {
        if (codigo == null || !estrategiasPorCodigo.containsKey(codigo)) {
            throw new ErroPedidoException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        return estrategiasPorCodigo.get(codigo);
    }

    public void validarParcelas(PagamentoStrategy pagamento, int parcelas) {
        if (!pagamento.parcelasValidas(parcelas)) {
            throw new ErroPedidoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
    }

    public void validarDisponibilidade(PagamentoStrategy pagamento, BigDecimal totalPedido) {
        if (!pagamento.disponivelPara(totalPedido)) {
            throw new ErroPedidoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
    }
}
