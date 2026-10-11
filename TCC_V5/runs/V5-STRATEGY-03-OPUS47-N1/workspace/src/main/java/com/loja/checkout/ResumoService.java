package com.loja.checkout;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {

    public ResumoResponse calcular(ResumoRequest req) {
        validarItens(req);
        Clube clube = decodificar(Clube.class, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = decodificar(Regiao.class, req.regiao(), "REGIAO_INVALIDA");
        Modalidade modalidade = decodificar(Modalidade.class, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = pesoTotal(req);
        if (!modalidade.atende(pesoTotal)) throw new ErroPedido("MODALIDADE_INDISPONIVEL");

        Cupom cupom = null;
        if (req.cupom() != null) cupom = decodificar(Cupom.class, req.cupom(), "CUPOM_INVALIDO");

        BigDecimal subtotalProdutos = Dinheiro.centavos(subtotalProdutos(req));
        BigDecimal frete = clube.freteGratis()
            ? Dinheiro.ZERO
            : Dinheiro.centavos(modalidade.frete(pesoTotal));

        BigDecimal descontoCupom = Dinheiro.ZERO;
        if (cupom != null) {
            Cupom.Contexto ctx = new Cupom.Contexto(req.itens(), subtotalProdutos, frete);
            if (!cupom.aplicavel(ctx)) throw new ErroPedido("CUPOM_NAO_APLICAVEL");
            descontoCupom = cupom.desconto(ctx);
        }

        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = Dinheiro.centavos(
            subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        Pagamento pagamento = decodificar(Pagamento.class, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!pagamento.parcelasValidas(parcelas)) throw new ErroPedido("PARCELAMENTO_INVALIDO");
        if (!pagamento.atende(totalPedido)) throw new ErroPedido("FORMA_PAGAMENTO_INDISPONIVEL");

        Pagamento.Resultado r = pagamento.calcular(totalPedido, parcelas);

        return new ResumoResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            modalidade.prazoDias(),
            seguro,
            r.ajustePagamento(),
            r.totalFinal(),
            parcelas,
            r.valorParcela(),
            clube.credito(subtotalProdutos),
            clube.brinde(subtotalProdutos)
        );
    }

    private static void validarItens(ResumoRequest req) {
        if (req.itens() == null || req.itens().isEmpty()) throw new ErroPedido("PEDIDO_INVALIDO");
        for (Item i : req.itens()) {
            if (i == null
                || i.precoUnitario() == null || i.precoUnitario().signum() <= 0
                || i.quantidade() == null || i.quantidade() <= 0
                || i.pesoKg() == null || i.pesoKg().signum() <= 0) {
                throw new ErroPedido("PEDIDO_INVALIDO");
            }
        }
    }

    private static BigDecimal subtotalProdutos(ResumoRequest req) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item i : req.itens()) {
            total = total.add(i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade())));
        }
        return total;
    }

    private static BigDecimal pesoTotal(ResumoRequest req) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item i : req.itens()) {
            total = total.add(i.pesoKg().multiply(BigDecimal.valueOf(i.quantidade())));
        }
        return total;
    }

    private static <E extends Enum<E>> E decodificar(Class<E> cls, String valor, String codigoErro) {
        if (valor == null) throw new ErroPedido(codigoErro);
        try {
            return Enum.valueOf(cls, valor);
        } catch (IllegalArgumentException e) {
            throw new ErroPedido(codigoErro);
        }
    }
}
