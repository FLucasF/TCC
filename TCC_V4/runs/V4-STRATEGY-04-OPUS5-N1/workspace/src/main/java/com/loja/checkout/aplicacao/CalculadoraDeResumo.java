package com.loja.checkout.aplicacao;

import com.loja.checkout.aplicacao.PedidoDoSite.ItemDoSite;
import com.loja.checkout.dominio.Centavos;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.cupom.Cupons;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.entrega.ModalidadesDeEntrega;
import com.loja.checkout.dominio.pagamento.Cobranca;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.FormasDePagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Monta o resumo da compra na ordem em que o valor final e calculado. */
@Service
public class CalculadoraDeResumo {

    private static final int PARCELA_UNICA = 1;

    public ResumoDaCompra calcular(PedidoDoSite entrada) {
        Pedido pedido = montarPedido(entrada);
        BigDecimal subtotalProdutos = pedido.subtotalProdutos();

        ModalidadeEntrega entrega = ModalidadesDeEntrega.CATALOGO.buscar(entrada.modalidadeEntrega());
        exigir(entrega.atende(pedido), CodigoErro.MODALIDADE_INDISPONIVEL);
        BigDecimal frete = pedido.nivelClube().fretePago(entrega.frete(pedido));

        BigDecimal descontoCupom = descontoDoCupom(entrada.cupom(), pedido, frete);
        BigDecimal seguro = pedido.regiao().seguro(subtotalProdutos);
        BigDecimal totalPedido = Centavos.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        int parcelas = Optional.ofNullable(entrada.parcelas()).orElse(PARCELA_UNICA);
        FormaPagamento pagamento = FormasDePagamento.CATALOGO.buscar(entrada.formaPagamento());
        exigir(pagamento.parcelamentoPermitido(parcelas), CodigoErro.PARCELAMENTO_INVALIDO);
        exigir(pagamento.atende(totalPedido), CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        Cobranca cobranca = pagamento.cobrar(totalPedido, parcelas);

        return new ResumoDaCompra(
                subtotalProdutos,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                Centavos.arredondar(cobranca.totalFinal().subtract(totalPedido)),
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela(),
                pedido.nivelClube().creditoProximaCompra(subtotalProdutos),
                pedido.nivelClube().temBrinde(subtotalProdutos));
    }

    private Pedido montarPedido(PedidoDoSite entrada) {
        exigir(entrada.itens() != null && !entrada.itens().isEmpty(), CodigoErro.PEDIDO_INVALIDO);
        List<ItemPedido> itens = entrada.itens().stream().map(this::montarItem).toList();
        NivelClube nivelClube = NivelClube.CATALOGO.buscar(entrada.nivelClube());
        Regiao regiao = Regiao.CATALOGO.buscar(entrada.regiao());
        return new Pedido(itens, nivelClube, regiao);
    }

    private ItemPedido montarItem(ItemDoSite item) {
        exigir(item != null, CodigoErro.PEDIDO_INVALIDO);
        exigir(positivo(item.precoUnitario()), CodigoErro.PEDIDO_INVALIDO);
        exigir(item.quantidade() != null && item.quantidade() > 0, CodigoErro.PEDIDO_INVALIDO);
        exigir(positivo(item.pesoKg()), CodigoErro.PEDIDO_INVALIDO);
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private BigDecimal descontoDoCupom(String codigo, Pedido pedido, BigDecimal frete) {
        return Optional.ofNullable(codigo)
                .filter(informado -> !informado.isBlank())
                .map(informado -> aplicar(Cupons.CATALOGO.buscar(informado), pedido, frete))
                .orElse(Centavos.ZERO);
    }

    private BigDecimal aplicar(Cupom cupom, Pedido pedido, BigDecimal frete) {
        exigir(cupom.aplicavel(pedido, frete), CodigoErro.CUPOM_NAO_APLICAVEL);
        return cupom.desconto(pedido, frete);
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private void exigir(boolean condicao, CodigoErro erro) {
        if (!condicao) {
            throw new PedidoRecusadoException(erro);
        }
    }
}
