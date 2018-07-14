/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.edu.ifpe.gem.realm;

/**
 *
 * @author MASC,FAL,JVAL
 */
public class GemRealmException extends RuntimeException {
   
	private static final long serialVersionUID = 7692245171182388127L;

	public GemRealmException(Exception root) {
        super(root);
    }
}
