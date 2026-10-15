package br.com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;
import java.util.List;

import br.com.loja.checkout.dominio.Catalogo;
import br.com.loja.checkout.dominio.MotivoRecusa;

/** Uma forma de pagamento, com o seu parcelamento e o seu ajuste no total. */
public interface FormaPagamento {

    Catalogo<FormaPagamento> CATALOGO = Catalogo.de(
            MotivoRecusa.FORMA_PAGAMENTO_INVALIDA,
            FormaPagamento::codigo,
            List.of(new Pix(), new Cartao(), new Boleto()));

    String codigo();

    boolean permiteParcelas(int parcelas);

    /** Se a forma de pagamento atende um pedido desse total. */
    boolean atende(BigDecimal totalPedido);

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
