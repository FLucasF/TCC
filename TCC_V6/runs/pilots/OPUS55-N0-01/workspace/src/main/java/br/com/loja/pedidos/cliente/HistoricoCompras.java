package br.com.loja.pedidos.cliente;

public interface HistoricoCompras {

    /** Se o CPF já tem algum pedido pago, enviado ou entregue. */
    boolean jaComprou(String cpf);
}
