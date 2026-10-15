package br.tcc.checkout.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.tcc.checkout.dominio.Dinheiro;

/**
 * O cliente não paga o frete: no resumo o frete continua aparecendo e o desconto
 * do cupom fica igual a ele.
 */
@Component
public class CupomFreteGratis implements Cupom {

	@Override
	public String codigo() {
		return "FRETEGRATIS";
	}

	@Override
	public BigDecimal calcularDesconto(ContextoCupom contexto) {
		return Dinheiro.arredondar(contexto.frete());
	}
}
