package com.loja.checkout.calculo;

import com.loja.checkout.api.ResumoRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import static com.loja.checkout.calculo.Dinheiro.ZERO;
import static com.loja.checkout.calculo.Dinheiro.centavos;

@Service
public class CalculadoraResumo {

    public ResumoCompra calcular(ResumoRequest pedido) {
        Carrinho carrinho = Carrinho.validar(pedido.itens());
        NivelClube nivel = buscar(NivelClube.class, pedido.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = buscar(Regiao.class, pedido.regiao(), CodigoErro.REGIAO_INVALIDA);
        ModalidadeEntrega entrega = buscar(ModalidadeEntrega.class, pedido.modalidadeEntrega(), CodigoErro.MODALIDADE_INVALIDA);
        if (!entrega.atende(carrinho.pesoKg())) {
            throw new PedidoRecusadoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        Cupom cupom = pedido.cupom() == null ? null : buscar(Cupom.class, pedido.cupom(), CodigoErro.CUPOM_INVALIDO);
        if (cupom != null && !cupom.aplicavel(carrinho)) {
            throw new PedidoRecusadoException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        FormaPagamento forma = buscar(FormaPagamento.class, pedido.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!forma.aceitaParcelas(parcelas)) {
            throw new PedidoRecusadoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.freteCobrado(centavos(entrega.frete(carrinho.pesoKg())));
        BigDecimal desconto = cupom == null ? ZERO : cupom.desconto(carrinho, frete);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);
        if (!forma.disponivel(total)) {
            throw new PedidoRecusadoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        BigDecimal totalFinal = forma.totalFinal(total, parcelas);
        BigDecimal valorParcela = totalFinal.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);

        return new ResumoCompra(
                subtotal,
                desconto,
                frete,
                entrega.prazoDias(),
                seguro,
                totalFinal.subtract(total),
                totalFinal,
                parcelas,
                valorParcela,
                nivel.credito(subtotal),
                nivel.temBrinde(subtotal));
    }

    private static <E extends Enum<E>> E buscar(Class<E> tipo, String nome, CodigoErro erro) {
        return Arrays.stream(tipo.getEnumConstants())
                .filter(constante -> constante.name().equals(nome))
                .findFirst()
                .orElseThrow(() -> new PedidoRecusadoException(erro));
    }
}
