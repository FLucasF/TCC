package br.com.loja.checkout;

import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.entrega.Entrega;
import br.com.loja.checkout.pagamento.Cobranca;
import br.com.loja.checkout.pagamento.FormaPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ResumoService {

    private final Catalogo<NivelClube> niveis;
    private final Catalogo<Entrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> formasPagamento;
    private final Catalogo<Regiao> regioes = new Catalogo<>(List.of(Regiao.values()), Regiao::name);

    public ResumoService(List<NivelClube> niveis, List<Entrega> entregas, List<Cupom> cupons,
                         List<FormaPagamento> formasPagamento) {
        this.niveis = new Catalogo<>(niveis, NivelClube::codigo);
        this.entregas = new Catalogo<>(entregas, Entrega::codigo);
        this.cupons = new Catalogo<>(cupons, Cupom::codigo);
        this.formasPagamento = new Catalogo<>(formasPagamento, FormaPagamento::codigo);
    }

    public ResumoResponse calcular(ResumoRequest request) {
        Pedido pedido = request.paraPedido();
        NivelClube nivel = niveis.buscar(request.nivelClube()).orElseThrow(() -> erro(CodigoErro.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = regioes.buscar(request.regiao()).orElseThrow(() -> erro(CodigoErro.REGIAO_INVALIDA));
        Entrega entrega = entregas.buscar(request.modalidadeEntrega()).orElseThrow(() -> erro(CodigoErro.MODALIDADE_INVALIDA));
        if (!entrega.atende(pedido)) {
            throw erro(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        Cupom cupom = request.cupom() == null
                ? Cupom.NENHUM
                : cupons.buscar(request.cupom()).orElseThrow(() -> erro(CodigoErro.CUPOM_INVALIDO));
        if (!cupom.aplicavel(pedido)) {
            throw erro(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        FormaPagamento pagamento = formasPagamento.buscar(request.formaPagamento())
                .orElseThrow(() -> erro(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = request.parcelasOuPadrao();
        if (!pagamento.parcelasPermitidas(parcelas)) {
            throw erro(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal subtotal = pedido.subtotal();
        BigDecimal frete = nivel.frete(entrega.frete(pedido));
        BigDecimal desconto = cupom.desconto(pedido, frete);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);

        if (!pagamento.atende(total)) {
            throw erro(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Cobranca cobranca = pagamento.cobrar(total, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                entrega.prazoDias(),
                seguro,
                cobranca.totalFinal().subtract(total),
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private static ErroNegocio erro(CodigoErro codigo) {
        return new ErroNegocio(codigo);
    }
}
