package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Codificado;
import java.math.BigDecimal;

public interface FormaPagamento extends Codificado {

    boolean parcelasPermitidas(int parcelas);

    boolean atende(BigDecimal totalPedido);

    Pagamento calcular(BigDecimal totalPedido, int parcelas);
}
