/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.edu.ifpe.gem.realm;

import com.sun.appserv.security.AppservPasswordLoginModule;
import java.util.List;
import java.util.logging.Level;

import javax.security.auth.login.LoginException;

/**
 *
 * @author MASC,FAL,JVAL
 */
public class GemLoginModule extends AppservPasswordLoginModule {

    @SuppressWarnings("deprecation")
	@Override
    protected void authenticateUser() throws LoginException {
        GemRealm<?> realm = (GemRealm<?>) _currentRealm;
        _logger.setLevel(Level.INFO);
        _logger.info("_username:" + _username);
        _logger.info("_password:" + _password);
        _logger.info("tentando autenticar...");
        if (realm.authenticateUser(_username, _password)) {
        	_logger.info("recuperando grupos do usuário...");
            List<String> groupsList = realm.getGroupList(_username);
            String[] groups = new String[groupsList.size()];
            int i = 0;
            for (String group : groupsList) {
                groups[i++] = group;
            }
            
            _logger.info("lista construída! fazendo o commit...");
            commitUserAuthentication(groups);
            _logger.info("commit executado...");
        } else {
            throw new LoginException("Invalid login!");
        }
    }

}
