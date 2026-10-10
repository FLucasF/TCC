package com.loja.checkout.dominio;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * A parte igual em todos os casos: a ordem do cálculo. Cada etapa pergunta ao caso escolhido
 * (entrega, cupom, clube, pagamento, região) o que é dele, e confere o que é dele no momento
 * em que já tem os valores de que precisa — daí sai a ordem em que os problemas são
 * encontrados.
 */
@Service
public class CalculadoraResumo {

    public ResumoCompra calcular(EntradaPedido entrada) {
        List<ItemPedido> itens = entrada.itens();              // 1 PEDIDO_INVALIDO
        NivelClube nivelClube = entrada.nivelClube();          // 2 NIVEL_CLUBE_INVALIDO
        Regiao regiao = entrada.regiao();                      // 3 REGIAO_INVALIDA

        BigDecimal subtotalProdutos = subtotal(itens);
        BigDecimal pesoKg = peso(itens);

        ModalidadeEntrega entrega = entrada.modalidadeEntrega();  // 4 MODALIDADE_INVALIDA
        if (!entrega.atende(pesoKg)) {                            // 5 MODALIDADE_INDISPONIVEL
            throw new PedidoRecusadoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        BigDecimal frete = nivelClube.frete(entrega.custo(pesoKg));

        // 6 CUPOM_INVALIDO e 7 CUPOM_NAO_APLICAVEL
        BigDecimal descontoCupom = descontoCupom(entrada, new ContextoCupom(itens, subtotalProdutos, frete));
        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento pagamento = entrada.formaPagamento();      // 8 FORMA_PAGAMENTO_INVALIDA
        int parcelas = entrada.parcelas();
        if (!pagamento.parcelasValidas(parcelas)) {                // 9 PARCELAMENTO_INVALIDO
            throw new PedidoRecusadoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!pagamento.disponivel(totalPedido)) {                  // 10 FORMA_PAGAMENTO_INDISPONIVEL
            throw new PedidoRecusadoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        ResultadoPagamento resultado = pagamento.cobrar(totalPedido, parcelas);

        return new ResumoCompra(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                resultado.totalFinal().subtract(totalPedido),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                nivelClube.creditoProximaCompra(subtotalProdutos),
                nivelClube.brinde(subtotalProdutos));
    }

    private BigDecimal descontoCupom(EntradaPedido entrada, ContextoCupom contexto) {
        return entrada.cupom()
                .map(cupom -> {
                    if (!cupom.aplicavel(contexto)) {
                        throw new PedidoRecusadoException(CodigoErro.CUPOM_NAO_APLICAVEL);
                    }
                    return cupom.desconto(contexto);
                })
                .orElse(Dinheiro.ZERO);
    }

    private BigDecimal subtotal(List<ItemPedido> itens) {
        // cada item já vem arredondado em centavos
        return itens.stream()
                .map(ItemPedido::total)
                .reduce(Dinheiro.ZERO, BigDecimal::add);
    }

    private BigDecimal peso(List<ItemPedido> itens) {
        return itens.stream()
                .map(ItemPedido::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
