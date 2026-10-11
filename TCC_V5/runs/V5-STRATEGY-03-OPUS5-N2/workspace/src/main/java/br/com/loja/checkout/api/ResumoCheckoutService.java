package br.com.loja.checkout.api;

import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.ContextoCupom;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.cupom.SemCupom;
import br.com.loja.checkout.dominio.Catalogo;
import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.Codificavel;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.ItemPedido;
import br.com.loja.checkout.dominio.Pedido;
import br.com.loja.checkout.dominio.PedidoRecusadoException;
import br.com.loja.checkout.dominio.Regiao;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Monta o resumo da compra na ordem em que a loja calcula o valor final. */
@Service
public class ResumoCheckoutService {

    private static final int PARCELAS_PADRAO = 1;

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveisClube;
    private final Catalogo<FormaPagamento> formasPagamento;
    private final Catalogo<Regiao> regioes;

    public ResumoCheckoutService(
            Catalogo<ModalidadeEntrega> modalidades,
            Catalogo<Cupom> cupons,
            Catalogo<NivelClube> niveisClube,
            Catalogo<FormaPagamento> formasPagamento,
            Catalogo<Regiao> regioes) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.niveisClube = niveisClube;
        this.formasPagamento = formasPagamento;
        this.regioes = regioes;
    }

    public ResumoResponse calcular(ResumoRequest requisicao) {
        Pedido pedido = montarPedido(requisicao.itens());
        NivelClube nivel = exigirOpcao(niveisClube, requisicao.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = exigirOpcao(regioes, requisicao.regiao(), CodigoErro.REGIAO_INVALIDA);

        ModalidadeEntrega modalidade =
                exigirOpcao(modalidades, requisicao.modalidadeEntrega(), CodigoErro.MODALIDADE_INVALIDA);
        exigir(modalidade.atende(pedido), CodigoErro.MODALIDADE_INDISPONIVEL);

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal frete = Dinheiro.centavos(nivel.frete(modalidade, pedido));

        Cupom cupom = resolverCupom(requisicao.cupom());
        ContextoCupom contexto = new ContextoCupom(pedido, frete);
        exigir(cupom.aplicavel(contexto), CodigoErro.CUPOM_NAO_APLICAVEL);
        BigDecimal descontoCupom = Dinheiro.centavos(cupom.desconto(contexto));

        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.centavos(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento pagamento =
                exigirOpcao(formasPagamento, requisicao.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = Optional.ofNullable(requisicao.parcelas()).orElse(PARCELAS_PADRAO);
        exigir(pagamento.parcelamentoPermitido(parcelas), CodigoErro.PARCELAMENTO_INVALIDO);
        exigir(pagamento.atende(totalPedido), CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        ResultadoPagamento resultado = pagamento.aplicar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                Dinheiro.centavos(resultado.totalFinal().subtract(totalPedido)),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                nivel.creditoProximaCompra(subtotalProdutos),
                nivel.brinde(subtotalProdutos));
    }

    private Pedido montarPedido(List<ItemRequest> itens) {
        exigir(itens != null && !itens.isEmpty(), CodigoErro.PEDIDO_INVALIDO);
        return new Pedido(itens.stream().map(this::montarItem).toList());
    }

    private ItemPedido montarItem(ItemRequest item) {
        exigir(item != null, CodigoErro.PEDIDO_INVALIDO);
        exigir(positivo(item.precoUnitario()), CodigoErro.PEDIDO_INVALIDO);
        exigir(item.quantidade() != null && item.quantidade() > 0, CodigoErro.PEDIDO_INVALIDO);
        exigir(positivo(item.pesoKg()), CodigoErro.PEDIDO_INVALIDO);
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private Cupom resolverCupom(String codigo) {
        return Objects.isNull(codigo)
                ? SemCupom.INSTANCIA
                : exigirOpcao(cupons, codigo, CodigoErro.CUPOM_INVALIDO);
    }

    private <T extends Codificavel> T exigirOpcao(Catalogo<T> catalogo, String codigo, CodigoErro erro) {
        return catalogo.buscar(codigo).orElseThrow(() -> new PedidoRecusadoException(erro));
    }

    private void exigir(boolean condicao, CodigoErro erro) {
        if (!condicao) {
            throw new PedidoRecusadoException(erro);
        }
    }
}
