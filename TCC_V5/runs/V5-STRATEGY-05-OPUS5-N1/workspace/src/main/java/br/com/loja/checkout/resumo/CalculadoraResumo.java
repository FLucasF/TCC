package br.com.loja.checkout.resumo;

import br.com.loja.checkout.clube.CatalogoNiveisClube;
import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.comum.CodigoErro;
import br.com.loja.checkout.comum.Dinheiro;
import br.com.loja.checkout.comum.PedidoRecusadoException;
import br.com.loja.checkout.cupom.CatalogoCupons;
import br.com.loja.checkout.cupom.ContextoCupom;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.entrega.CatalogoModalidades;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.CatalogoFormasPagamento;
import br.com.loja.checkout.pagamento.Cobranca;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.pedido.Carrinho;
import br.com.loja.checkout.pedido.PedidoBruto;
import br.com.loja.checkout.seguro.Regiao;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Monta o resumo da compra: confere o pedido na ordem combinada e soma as
 * partes. Cada parte sabe se calcular; aqui só ficam a ordem e as somas.
 */
@Service
public class CalculadoraResumo {

    private final CatalogoNiveisClube niveis;
    private final CatalogoModalidades modalidades;
    private final CatalogoCupons cupons;
    private final CatalogoFormasPagamento formasPagamento;

    public CalculadoraResumo(CatalogoNiveisClube niveis, CatalogoModalidades modalidades,
            CatalogoCupons cupons, CatalogoFormasPagamento formasPagamento) {
        this.niveis = niveis;
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.formasPagamento = formasPagamento;
    }

    public ResumoCompra calcular(PedidoBruto pedido) {
        Carrinho carrinho = Carrinho.conferir(pedido.itens());
        NivelClube nivel = exigir(niveis.buscar(pedido.nivelClube()), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = exigir(Regiao.buscar(pedido.regiao()), CodigoErro.REGIAO_INVALIDA);
        ModalidadeEntrega modalidade =
                exigir(modalidades.buscar(pedido.modalidadeEntrega()), CodigoErro.MODALIDADE_INVALIDA);

        BigDecimal pesoKg = carrinho.pesoKg();
        if (!modalidade.atende(pesoKg)) {
            throw new PedidoRecusadoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        Cupom cupom = pedido.cupom() == null
                ? null
                : exigir(cupons.buscar(pedido.cupom()), CodigoErro.CUPOM_INVALIDO);

        BigDecimal subtotalProdutos = Dinheiro.centavos(carrinho.subtotalProdutos());
        BigDecimal frete = nivel.isentaFrete()
                ? Dinheiro.ZERO
                : Dinheiro.centavos(modalidade.frete(pesoKg));

        BigDecimal descontoCupom = Dinheiro.ZERO;
        if (cupom != null) {
            ContextoCupom contexto = new ContextoCupom(carrinho.itens(), subtotalProdutos, frete);
            if (!cupom.aplicavel(contexto)) {
                throw new PedidoRecusadoException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = Dinheiro.centavos(cupom.desconto(contexto));
        }

        FormaPagamento formaPagamento =
                exigir(formasPagamento.buscar(pedido.formaPagamento()), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = pedido.parcelasOuUma();
        if (!formaPagamento.permiteParcelas(parcelas)) {
            throw new PedidoRecusadoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));
        if (!formaPagamento.atende(totalPedido)) {
            throw new PedidoRecusadoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        Cobranca cobranca = formaPagamento.cobrar(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.centavos(cobranca.totalFinal().subtract(totalPedido));

        return new ResumoCompra(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento,
                Dinheiro.centavos(cobranca.totalFinal()),
                parcelas,
                Dinheiro.centavos(cobranca.valorParcela()),
                nivel.creditoProximaCompra(subtotalProdutos),
                nivel.ganhaBrinde(subtotalProdutos));
    }

    private static <T> T exigir(Optional<T> opcao, CodigoErro codigo) {
        return opcao.orElseThrow(() -> new PedidoRecusadoException(codigo));
    }
}
