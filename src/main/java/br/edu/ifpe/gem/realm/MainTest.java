package br.edu.ifpe.gem.realm;

import java.util.ArrayList;

public class MainTest {

	public static void main(String[] args) {
		
		String dominiosInstituncionais = "a.recife.ifpe.edu.br;cabo.ifpe.edu.br;";
		ArrayList <String> diSeparados = new ArrayList<String>();
		
		int inicio = 0;
		int fim = 0;
		for (int i=0; i<dominiosInstituncionais.length(); i++) {
			char c = dominiosInstituncionais.charAt(i);
			if(c == ';') {
				fim = i;
				diSeparados.add(dominiosInstituncionais.substring(inicio, fim));
				inicio = i+1;
			}
		}
		
		for (String string : diSeparados) {
			System.out.println(string);
		}
	}

}
