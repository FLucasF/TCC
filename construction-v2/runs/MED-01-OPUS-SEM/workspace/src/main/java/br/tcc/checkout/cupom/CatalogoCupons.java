package br.tcc.checkout.cupom;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

/** Reúne os cupons que valem hoje, indexados pelo código. */
@Component
public class CatalogoCupons {

	private final Map<String, Cupom> porCodigo = new LinkedHashMap<>();

	public CatalogoCupons(List<Cupom> cupons) {
		for (Cupom cupom : cupons) {
			Cupom anterior = porCodigo.put(cupom.codigo(), cupom);
			if (anterior != null) {
				throw new IllegalStateException("Código de cupom duplicado: " + cupom.codigo());
			}
		}
	}

	public Optional<Cupom> buscar(String codigo) {
		return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
	}
}
