package loja.checkout;

import java.math.BigDecimal;
import java.util.List;
import loja.checkout.clube.NivelClube;
import loja.checkout.cupom.Cupom;
import loja.checkout.dominio.Carrinho;
import loja.checkout.dominio.Catalogo;
import loja.checkout.dominio.CodigoErro;
import loja.checkout.dominio.Dinheiro;
import loja.checkout.dominio.Item;
import loja.checkout.dominio.PedidoRecusadoException;
import loja.checkout.dominio.Regiao;
import loja.checkout.entrega.Entrega;
import loja.checkout.pagamento.Cobranca;
import loja.checkout.pagamento.FormaPagamento;
import loja.checkout.web.ResumoRequest;
import loja.checkout.web.ResumoRequest.ItemRequest;
import loja.checkout.web.ResumoResponse;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {

    private final Catalogo<Entrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> pagamentos;
    private final Catalogo<Regiao> regioes = new Catalogo<>(List.of(Regiao.values()));

    public ResumoService(List<Entrega> entregas, List<Cupom> cupons,
                         List<NivelClube> niveis, List<FormaPagamento> pagamentos) {
        this.entregas = new Catalogo<>(entregas);
        this.cupons = new Catalogo<>(cupons);
        this.niveis = new Catalogo<>(niveis);
        this.pagamentos = new Catalogo<>(pagamentos);
    }

    public ResumoResponse resumir(ResumoRequest pedido) {
        Carrinho carrinho = montarCarrinho(pedido.itens());
        NivelClube nivel = niveis.exigir(pedido.nivelClube(), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = regioes.exigir(pedido.regiao(), CodigoErro.REGIAO_INVALIDA);
        Entrega entrega = entregas.exigir(pedido.modalidadeEntrega(), CodigoErro.MODALIDADE_INVALIDA);
        if (!entrega.atende(carrinho)) {
            throw new PedidoRecusadoException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.frete(entrega.frete(carrinho));
        BigDecimal desconto = descontoDoCupom(pedido.cupom(), carrinho, frete);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        FormaPagamento pagamento = pagamentos.exigir(pedido.formaPagamento(), CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new PedidoRecusadoException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!pagamento.disponivel(totalPedido)) {
            throw new PedidoRecusadoException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Cobranca cobranca = pagamento.cobrar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                entrega.prazoDias(),
                seguro,
                cobranca.totalFinal().subtract(totalPedido),
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private BigDecimal descontoDoCupom(String codigo, Carrinho carrinho, BigDecimal frete) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        return cupons.exigir(codigo, CodigoErro.CUPOM_INVALIDO).desconto(carrinho, frete);
    }

    private Carrinho montarCarrinho(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Carrinho(itens.stream().map(this::paraItem).toList());
    }

    private Item paraItem(ItemRequest item) {
        if (item == null
                || !positivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || !positivo(item.pesoKg())) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }
}
