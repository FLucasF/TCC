package com.loja.checkout.model;

import com.loja.checkout.util.Arredondador;
import java.util.Arrays;
import java.util.List;

public interface FormaPagamento {

    double calcularAjuste(double total, int parcelas);

    boolean isDisponivel(double total, int parcelas);

    List<Integer> getParcelasPermitidas();

    static FormaPagamento obter(String forma) {
        return switch (forma) {
            case "PIX" -> new FormaPagamentoPix();
            case "CARTAO" -> new FormaPagamentoCartao();
            case "BOLETO" -> new FormaPagamentoBoleto();
            default -> null;
        };
    }

}

class FormaPagamentoPix implements FormaPagamento {
    @Override
    public double calcularAjuste(double total, int parcelas) {
        double desconto = Arredondador.arredondar(total * 0.05);
        return -desconto;
    }

    @Override
    public boolean isDisponivel(double total, int parcelas) {
        return true;
    }

    @Override
    public List<Integer> getParcelasPermitidas() {
        return Arrays.asList(1);
    }
}

class FormaPagamentoCartao implements FormaPagamento {
    private static final double TAXA_MENSAL = 0.0199;

    @Override
    public double calcularAjuste(double total, int parcelas) {
        if (parcelas <= 3) {
            return 0.0;
        }

        double taxa = TAXA_MENSAL;
        double numerador = total * taxa;
        double denominador = 1.0 - Math.pow(1.0 + taxa, -parcelas);
        double valorParcela = Arredondador.arredondar(numerador / denominador);
        double totalComJuros = Arredondador.arredondar(valorParcela * parcelas);
        double juros = totalComJuros - total;

        return Arredondador.arredondar(juros);
    }

    @Override
    public boolean isDisponivel(double total, int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public List<Integer> getParcelasPermitidas() {
        return Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    }
}

class FormaPagamentoBoleto implements FormaPagamento {
    private static final double TARIFA = 3.49;

    @Override
    public double calcularAjuste(double total, int parcelas) {
        return Arredondador.arredondar(TARIFA);
    }

    @Override
    public boolean isDisponivel(double total, int parcelas) {
        return total <= 1000.00;
    }

    @Override
    public List<Integer> getParcelasPermitidas() {
        return Arrays.asList(1);
    }
}
