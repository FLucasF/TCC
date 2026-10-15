package br.tcc.checkout.entrega;

import br.tcc.checkout.dominio.DadosPedido;

import java.math.BigDecimal;

/**
 * Contrato de uma opção de entrega. Novas transportadoras/modalidades
 * entram no sistema apenas implementando esta interface e virando um
 * componente Spring - sem tocar no serviço de checkout.
 */
public interface OpcaoEntrega {

	String getCodigo();

	int getPrazoDias();

	boolean isDisponivel(DadosPedido pedido);

	BigDecimal calcularFrete(DadosPedido pedido);
}
