package loja.checkout;

import loja.checkout.clube.NivelClube;
import loja.checkout.cupom.Cupom;
import loja.checkout.entrega.ModalidadeEntrega;
import loja.checkout.pagamento.Cobranca;
import loja.checkout.pagamento.FormaPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import static loja.checkout.ErroCheckout.*;

@Service
public class ResumoService {

    private final Catalogo<ModalidadeEntrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> pagamentos;

    public ResumoService(List<ModalidadeEntrega> entregas, List<Cupom> cupons,
                         List<NivelClube> niveis, List<FormaPagamento> pagamentos) {
        this.entregas = new Catalogo<>(entregas);
        this.cupons = new Catalogo<>(cupons);
        this.niveis = new Catalogo<>(niveis);
        this.pagamentos = new Catalogo<>(pagamentos);
    }

    public ResumoResponse calcular(ResumoRequest req) {
        Pedido pedido = pedidoValido(req.itens());
        NivelClube nivel = niveis.buscar(req.nivelClube())
                .orElseThrow(() -> new PedidoRecusadoException(NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.de(req.regiao())
                .orElseThrow(() -> new PedidoRecusadoException(REGIAO_INVALIDA));
        ModalidadeEntrega entrega = entregas.buscar(req.modalidadeEntrega())
                .orElseThrow(() -> new PedidoRecusadoException(MODALIDADE_INVALIDA));
        if (!entrega.atende(pedido)) {
            throw new PedidoRecusadoException(MODALIDADE_INDISPONIVEL);
        }

        BigDecimal frete = Dinheiro.arredondar(nivel.frete(entrega.frete(pedido)));
        BigDecimal desconto = descontoCupom(req.cupom(), pedido, frete);
        BigDecimal seguro = regiao.seguro(pedido.subtotal());
        BigDecimal total = pedido.subtotal().subtract(desconto).add(frete).add(seguro);

        FormaPagamento pagamento = pagamentos.buscar(req.formaPagamento())
                .orElseThrow(() -> new PedidoRecusadoException(FORMA_PAGAMENTO_INVALIDA));
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new PedidoRecusadoException(PARCELAMENTO_INVALIDO);
        }
        if (!pagamento.disponivel(total)) {
            throw new PedidoRecusadoException(FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Cobranca cobranca = pagamento.cobrar(total, parcelas);

        return new ResumoResponse(
                pedido.subtotal(), desconto, frete, entrega.prazoDias(), seguro,
                Dinheiro.arredondar(cobranca.totalFinal().subtract(total)),
                Dinheiro.arredondar(cobranca.totalFinal()), parcelas,
                Dinheiro.arredondar(cobranca.valorParcela()),
                Dinheiro.arredondar(nivel.credito(pedido)), nivel.brinde(pedido));
    }

    private Pedido pedidoValido(List<Item> itens) {
        if (itens == null || itens.isEmpty()
                || itens.stream().anyMatch(i -> i == null || !i.valido())) {
            throw new PedidoRecusadoException(PEDIDO_INVALIDO);
        }
        return Pedido.de(itens);
    }

    private BigDecimal descontoCupom(String codigo, Pedido pedido, BigDecimal frete) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo)
                .orElseThrow(() -> new PedidoRecusadoException(CUPOM_INVALIDO));
        if (!cupom.aplicavel(pedido, frete)) {
            throw new PedidoRecusadoException(CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.arredondar(cupom.desconto(pedido, frete));
    }
}
