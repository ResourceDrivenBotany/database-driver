/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PlantGameServer;

/**
 *
 * @author mishakanai
 */
public class NegativeResourceException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public NegativeResourceException() {
		super();
	}

	public NegativeResourceException(String string) {
		super(string);
	}
    }
