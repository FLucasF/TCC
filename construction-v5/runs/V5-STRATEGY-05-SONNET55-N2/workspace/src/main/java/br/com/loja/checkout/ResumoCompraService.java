package br.com.loja.checkout;

import static br.com.loja.checkout.CodigoErro.*;

import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.entrega.Entrega;
import br.com.loja.checkout.pagamento.Cobranca;
import br.com.loja.checkout.pagamento.FormaPagamento;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ResumoCompraService {

    private final Catalogo<Entrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;

    public ResumoCompraService(Catalogo<Entrega> entregas, Catalogo<Cupom> cupons,
                               Catalogo<NivelClube> niveis, Catalogo<FormaPagamento> formasPagamento) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.niveis = niveis;
        this.formasPagamento = formasPagamento;
    }

    public ResumoCompra calcular(RequisicaoResumo req) {
        if (req.itens() == null || req.itens().isEmpty()
                || req.itens().stream().anyMatch(i -> i == null || !i.valido())) {
            throw new PedidoRecusadoException(PEDIDO_INVALIDO);
        }
        NivelClube nivel = exigir(niveis.buscar(req.nivelClube()), NIVEL_CLUBE_INVALIDO);
        Regiao regiao = exigir(Regiao.de(req.regiao()), REGIAO_INVALIDA);
        Entrega entrega = exigir(entregas.buscar(req.modalidadeEntrega()), MODALIDADE_INVALIDA);

        Pedido pedido = Pedido.de(req.itens());
        if (!entrega.atende(pedido)) {
            throw new PedidoRecusadoException(MODALIDADE_INDISPONIVEL);
        }

        BigDecimal frete = nivel.freteCobrado(entrega.frete(pedido));
        BigDecimal desconto = Dinheiro.ZERO;
        if (req.cupom() != null) {
            Cupom cupom = exigir(cupons.buscar(req.cupom()), CUPOM_INVALIDO);
            if (!cupom.aplicavel(pedido)) {
                throw new PedidoRecusadoException(CUPOM_NAO_APLICAVEL);
            }
            desconto = cupom.desconto(pedido, frete);
        }

        BigDecimal seguro = regiao.seguro(pedido.subtotal());
        BigDecimal total = pedido.subtotal().subtract(desconto).add(frete).add(seguro);

        FormaPagamento pagamento = exigir(formasPagamento.buscar(req.formaPagamento()), FORMA_PAGAMENTO_INVALIDA);
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new PedidoRecusadoException(PARCELAMENTO_INVALIDO);
        }
        if (!pagamento.disponivel(total)) {
            throw new PedidoRecusadoException(FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Cobranca cobranca = pagamento.cobrar(total, parcelas);

        return new ResumoCompra(
                pedido.subtotal(), desconto, frete, entrega.prazoDias(), seguro,
                cobranca.totalFinal().subtract(total), cobranca.totalFinal(),
                parcelas, cobranca.valorParcela(),
                nivel.credito(pedido.subtotal()), nivel.brinde(pedido.subtotal()));
    }

    private static <T> T exigir(Optional<T> valor, CodigoErro erro) {
        return valor.orElseThrow(() -> new PedidoRecusadoException(erro));
    }
}
