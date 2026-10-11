package com.loja.checkout.dominio;

import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.clube.Niveis;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.cupom.Cupons;
import com.loja.checkout.dominio.entrega.Entregas;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.Cobranca;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.Pagamentos;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * A ordem do cálculo e a ordem da conferência dos erros: é o que não muda de
 * caso para caso. O que muda está nas implementações de cada eixo.
 */
@Service
public class CalculadoraResumo {

    public Resumo calcular(Solicitacao solicitacao) {
        Pedido pedido = Carrinho.validar(solicitacao.itens());
        NivelClube nivel = Niveis.CATALOGO.exigir(solicitacao.nivelClube());
        Regiao regiao = Regiao.CATALOGO.exigir(solicitacao.regiao());

        ModalidadeEntrega entrega = Entregas.CATALOGO.exigir(solicitacao.modalidadeEntrega());
        if (!entrega.atende(pedido)) {
            throw new ErroPedido("MODALIDADE_INDISPONIVEL");
        }

        Cupom cupom = cupomDe(solicitacao);
        if (cupom != null && !cupom.aplicavelA(pedido)) {
            throw new ErroPedido("CUPOM_NAO_APLICAVEL");
        }

        FormaPagamento pagamento = Pagamentos.CATALOGO.exigir(solicitacao.formaPagamento());
        int parcelas = solicitacao.parcelas() == null ? 1 : solicitacao.parcelas();
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new ErroPedido("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = nivel.freteCobrado(entrega.frete(pedido));
        BigDecimal descontoCupom = cupom == null ? Dinheiro.ZERO : cupom.desconto(pedido, frete);
        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);

        if (!pagamento.atende(totalPedido)) {
            throw new ErroPedido("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        Cobranca cobranca = pagamento.cobrar(totalPedido, parcelas);

        return new Resumo(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                Dinheiro.centavos(cobranca.totalFinal().subtract(totalPedido)),
                Dinheiro.centavos(cobranca.totalFinal()),
                parcelas,
                Dinheiro.centavos(cobranca.valorParcela()),
                nivel.creditoProximaCompra(subtotalProdutos),
                nivel.temBrinde(subtotalProdutos));
    }

    private Cupom cupomDe(Solicitacao solicitacao) {
        String codigo = solicitacao.cupom();
        if (codigo == null) {
            return null;
        }
        return Cupons.CATALOGO.exigir(codigo);
    }
}
