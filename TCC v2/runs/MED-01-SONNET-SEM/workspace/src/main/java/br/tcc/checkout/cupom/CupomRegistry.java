package br.tcc.checkout.cupom;

import br.tcc.checkout.exception.CheckoutErrorCode;
import br.tcc.checkout.exception.CheckoutException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CupomRegistry {

	private final Map<String, Cupom> cuponsPorCodigo;

	public CupomRegistry(List<Cupom> cupons) {
		this.cuponsPorCodigo = cupons.stream()
				.collect(Collectors.toMap(Cupom::getCodigo, Function.identity()));
	}

	public Cupom buscar(String codigo) {
		Cupom cupom = cuponsPorCodigo.get(codigo);
		if (cupom == null) {
			throw new CheckoutException(CheckoutErrorCode.CUPOM_INVALIDO);
		}
		return cupom;
	}
}
