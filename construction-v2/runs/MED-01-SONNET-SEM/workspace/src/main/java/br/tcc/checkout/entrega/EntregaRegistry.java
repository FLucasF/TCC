package br.tcc.checkout.entrega;

import br.tcc.checkout.exception.CheckoutErrorCode;
import br.tcc.checkout.exception.CheckoutException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EntregaRegistry {

	private final Map<String, OpcaoEntrega> opcoesPorCodigo;

	public EntregaRegistry(List<OpcaoEntrega> opcoesEntrega) {
		this.opcoesPorCodigo = opcoesEntrega.stream()
				.collect(Collectors.toMap(OpcaoEntrega::getCodigo, Function.identity()));
	}

	public OpcaoEntrega buscar(String codigo) {
		OpcaoEntrega opcao = codigo == null ? null : opcoesPorCodigo.get(codigo);
		if (opcao == null) {
			throw new CheckoutException(CheckoutErrorCode.MODALIDADE_INVALIDA);
		}
		return opcao;
	}
}
