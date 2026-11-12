package loja;

public class Planos {

    public double preco(String plano, double base) {
        if (plano.equals("BASICO")) return base;
        if (plano.equals("PLUS"))   return base * 0.9;
        throw new IllegalArgumentException("plano desconhecido: " + plano);
    }

    public int limite(String plano) {
        if (plano.equals("BASICO")) return 1;
        if (plano.equals("PLUS"))   return 3;
        throw new IllegalArgumentException("plano desconhecido: " + plano);
    }
}
