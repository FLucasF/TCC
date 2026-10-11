package br.com.loja.checkout.dominio;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import br.com.loja.checkout.dominio.clube.NivelClube;
import br.com.loja.checkout.dominio.cupom.ContextoCupom;
import br.com.loja.checkout.dominio.cupom.Cupom;
import br.com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import br.com.loja.checkout.dominio.pagamento.Cobranca;
import br.com.loja.checkout.dominio.pagamento.FormaPagamento;

/**
 * Monta o resumo da compra. A ordem das etapas e das conferências é a mesma
 * para todo pedido; quanto cada etapa vale é com o caso escolhido.
 */
@Service
public class CalculadoraResumo {

    public Resumo calcular(Compra compra) {
        Pedido pedido = Pedido.de(compra.itens());
        NivelClube nivel = NivelClube.CATALOGO.obrigatorio(compra.nivelClube());
        Regiao regiao = Regiao.CATALOGO.obrigatorio(compra.regiao());

        ModalidadeEntrega modalidade = ModalidadeEntrega.CATALOGO.obrigatorio(compra.modalidadeEntrega());
        if (!modalidade.atende(pedido.pesoKg())) {
            throw new PedidoRecusadoException(MotivoRecusa.MODALIDADE_INDISPONIVEL);
        }
        BigDecimal frete = nivel.isentaFrete()
                ? Dinheiro.ZERO
                : modalidade.frete(pedido.pesoKg());

        BigDecimal descontoCupom = descontoDo(compra.cupom(), new ContextoCupom(pedido, frete));
        BigDecimal seguro = regiao.seguro(pedido.subtotal());
        BigDecimal totalPedido = Dinheiro.centavos(pedido.subtotal()
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro));

        FormaPagamento pagamento = FormaPagamento.CATALOGO.obrigatorio(compra.formaPagamento());
        int parcelas = compra.parcelasOuUma();
        if (!pagamento.permiteParcelas(parcelas)) {
            throw new PedidoRecusadoException(MotivoRecusa.PARCELAMENTO_INVALIDO);
        }
        if (!pagamento.atende(totalPedido)) {
            throw new PedidoRecusadoException(MotivoRecusa.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Cobranca cobranca = pagamento.cobrar(totalPedido, parcelas);

        return new Resumo(
                pedido.subtotal(),
                descontoCupom,
                Dinheiro.centavos(frete),
                modalidade.prazoDias(),
                seguro,
                Dinheiro.centavos(cobranca.totalFinal().subtract(totalPedido)),
                cobranca.totalFinal(),
                cobranca.parcelas(),
                cobranca.valorParcela(),
                nivel.creditoProximaCompra(pedido.subtotal()),
                nivel.ganhaBrinde(pedido.subtotal()));
    }

    private BigDecimal descontoDo(String codigo, ContextoCupom contexto) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = Cupom.CATALOGO.obrigatorio(codigo);
        if (!cupom.aplicavel(contexto)) {
            throw new PedidoRecusadoException(MotivoRecusa.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.centavos(cupom.desconto(contexto));
    }
}
