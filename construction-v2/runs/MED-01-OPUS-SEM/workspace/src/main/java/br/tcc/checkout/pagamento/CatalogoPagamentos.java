package br.tcc.checkout.pagamento;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

/** Reúne as formas de pagamento aceitas, indexadas pelo código. */
@Component
public class CatalogoPagamentos {

	private final Map<String, FormaPagamento> porCodigo = new LinkedHashMap<>();

	public CatalogoPagamentos(List<FormaPagamento> formas) {
		for (FormaPagamento forma : formas) {
			FormaPagamento anterior = porCodigo.put(forma.codigo(), forma);
			if (anterior != null) {
				throw new IllegalStateException("Código de pagamento duplicado: " + forma.codigo());
			}
		}
	}

	public Optional<FormaPagamento> buscar(String codigo) {
		return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
	}
}
