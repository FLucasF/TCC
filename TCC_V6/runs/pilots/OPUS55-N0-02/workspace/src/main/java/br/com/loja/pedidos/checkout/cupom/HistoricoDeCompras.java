package br.com.loja.pedidos.checkout.cupom;

public interface HistoricoDeCompras {

    /** Se o CPF já tem algum pedido com pagamento confirmado (pago, enviado ou entregue). */
    boolean jaComprou(String cpf);
}
