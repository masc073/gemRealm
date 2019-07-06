/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.edu.ifpe.gem.realm;

import com.sun.appserv.security.AppservPasswordLoginModule;
import java.util.List;
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
        if (realm.authenticateUser(_username, _password)) {
            List<String> groupsList = realm.getGroupList(_username);
            String[] groups = new String[groupsList.size()];
            int i = 0;
            for (String group : groupsList) {
                groups[i++] = group;
            }
            
            commitUserAuthentication(groups);
        } else {
            throw new LoginException("Invalid login!");
        }
    }

}
