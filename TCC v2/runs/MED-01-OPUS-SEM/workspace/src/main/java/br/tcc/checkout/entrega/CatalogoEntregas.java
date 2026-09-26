package br.tcc.checkout.entrega;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

/** Reúne as modalidades de entrega disponíveis no sistema, indexadas pelo código. */
@Component
public class CatalogoEntregas {

	private final Map<String, ModalidadeEntrega> porCodigo = new LinkedHashMap<>();

	public CatalogoEntregas(List<ModalidadeEntrega> modalidades) {
		for (ModalidadeEntrega modalidade : modalidades) {
			ModalidadeEntrega anterior = porCodigo.put(modalidade.codigo(), modalidade);
			if (anterior != null) {
				throw new IllegalStateException("Código de entrega duplicado: " + modalidade.codigo());
			}
		}
	}

	public Optional<ModalidadeEntrega> buscar(String codigo) {
		return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
	}
}
