package br.edu.ifpe.gem.realm;

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.appserv.security.AppservRealm;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.common.Util;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;

import javax.naming.NamingException;
import javax.sql.DataSource;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.utilities.BuilderHelper;

/**
 *
 * @author MASC, FAL, JVAL
 * @param <E>
 */
public class GemRealm<E> extends AppservRealm {

    public static final String PASSWORD_SQL_QUERY = "password-sql-query";
    public static final String GROUPS_SQL_QUERY = "groups-sql-query";
    public static final String LOGIN_GOOGLE_SQL_QUERY = "login-google-sql-query";
    public static final String JTA_DATA_SOURCE = "jta-data-source";
    public static final String HASH_ALGORITHM = "hash-algorithm";
    public static final String CAMPIGEM = "campigem";
    public static final String DOMINIOSINSTITUNCIONAIS = "dominiosInstituncionais";
    public static final String DOMINIOSALUNOS = "dominiosAluno";
    public static final String CHARSET = "charset";
    private static Enumeration<?> all_properties;
    private static List<String> dominios; 

    private static DataSource dataSource;

    private Connection getConnection() {
        try {
            synchronized (this) {
                if (dataSource == null) {
                	_logger.setLevel(Level.INFO);
                	_logger.info("*** Lendo dataSource ***");
                    @SuppressWarnings("unchecked")
					ActiveDescriptor<ConnectorRuntime> cr = (ActiveDescriptor<ConnectorRuntime>) Util.getDefaultHabitat().getBestDescriptor(BuilderHelper.createContractFilter(ConnectorRuntime.class.getName()));
                    ConnectorRuntime connectorRuntime = Util.getDefaultHabitat().getServiceHandle(cr).getService();
                    dataSource = (DataSource) connectorRuntime.lookupNonTxResource(getJtaDataSource(), false);
                    if (dataSource != null) {
                    	_logger.info("*** Datasource lido com sucesso ***");
                    } else {
                    	_logger.info("*** Problema na leitura do datasource ***");
                    }
                }
            }

            _logger.info("*** recuperando conexão ***");
            return dataSource.getConnection();
        } catch (NamingException | SQLException ex) {
        	_logger.info("*** erro na recuperação da conexão ***");
            throw new GemRealmException(ex);
        } catch (Exception ex) {
        	_logger.info("*** erro inesperado ***");
        	throw new GemRealmException(ex);
        }
    }

    private void close(ResultSet resultSet, Statement statement, Connection connection) {
        try {
            if (resultSet != null) {
                resultSet.close();
            }

            if (statement != null) {
                statement.close();
            }

            if (connection != null) {
                connection.close();
            }
        } catch (SQLException ex) {
            throw new GemRealmException(ex);
        }
    }

    public String getCharset() {
        return super.getProperty(CHARSET);
    }

    public String getHashAlgorithm() {
        return super.getProperty(HASH_ALGORITHM);
    }

    public String getJtaDataSource() {
        return super.getProperty(JTA_DATA_SOURCE);
    }

    public String getPasswordQuery() {
        return super.getProperty(PASSWORD_SQL_QUERY);
    }

    public String getGroupsQuery() {
        return super.getProperty(GROUPS_SQL_QUERY);
    }
    
    public String getLoginGoogleQuery() {
    	return super.getProperty(LOGIN_GOOGLE_SQL_QUERY);
    }
    
    public String getCampiGem() {
    	return super.getProperty(CAMPIGEM);
    }
    
    public String getDominiosInstituncionais() {
    	return super.getProperty(DOMINIOSINSTITUNCIONAIS);
    }
    
    public String getDominiosAluno() {
    	return super.getProperty(DOMINIOSALUNOS);
    }

    @Override
    public synchronized void init(Properties properties) throws BadRealmException, NoSuchRealmException {
        setProperty(JAAS_CONTEXT_PARAM, properties.getProperty(JAAS_CONTEXT_PARAM));
        setProperty(PASSWORD_SQL_QUERY, properties.getProperty(PASSWORD_SQL_QUERY));
        setProperty(GROUPS_SQL_QUERY, properties.getProperty(GROUPS_SQL_QUERY));
        setProperty(JTA_DATA_SOURCE, properties.getProperty(JTA_DATA_SOURCE));
        setProperty(HASH_ALGORITHM, properties.getProperty(HASH_ALGORITHM));
        setProperty(CHARSET, properties.getProperty(CHARSET));
        setProperty(LOGIN_GOOGLE_SQL_QUERY, properties.getProperty(LOGIN_GOOGLE_SQL_QUERY));
        setProperty(CAMPIGEM, properties.getProperty(CAMPIGEM));
        setProperty(DOMINIOSINSTITUNCIONAIS, properties.getProperty(DOMINIOSINSTITUNCIONAIS));
        setProperty(DOMINIOSALUNOS, properties.getProperty(DOMINIOSALUNOS));
        
//        all_properties = properties.keys();
//        dominios = new ArrayList<String>();
//        while(all_properties.hasMoreElements()) {
//        	String key = (String)all_properties.nextElement();
//        	if(key.substring(0, 7).equals("dominio")) {
//        		dominios.add((String)properties.getProperty(key));
//        	}
//        }
        dominios = new ArrayList<String>();
        
        int inicio = 0;
		int fim = 0;
		String dominiosInstituncionais = getDominiosInstituncionais();
		for (int i=0; i<dominiosInstituncionais.length(); i++) {
			char c = dominiosInstituncionais.charAt(i);
			if(c == ';') {
				fim = i;
				dominios.add(dominiosInstituncionais.substring(inicio, fim));
				inicio = i+1;
			}
		}
		inicio = 0;
		fim = 0;
		String dominiosAluno = getDominiosAluno();
		for (int i=0; i<dominiosAluno.length(); i++) {
			char c = dominiosAluno.charAt(i);
			if(c == ';') {
				fim = i;
				dominios.add(dominiosAluno.substring(inicio, fim));
				inicio = i+1;
			}
		}
		_logger.info("Dominios Registrados....:");
		for (String string : dominios) {
			_logger.info(string);
		}
    }

    @Override
    public String getAuthType() {
        return "jdbc";
    }

    @Override
    public String getJAASContext() {
        return "gemRealm";
    }

    public boolean authenticateUser(String username, String password) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean result = true;

        try {
    		_logger.info("recuperando conexao SQL...");
        	conn = getConnection();
        	_logger.info("conexão recuperada...");
        
        	if (conn != null)
        		_logger.info("conexão não nula");
        	stmt = conn.prepareStatement(getPasswordQuery());
        	stmt.setString(1, username);
        	_logger.info("tentando executar a query...");
        	rs = stmt.executeQuery();
        	_logger.info("query executada...");

        	if (rs.next()) {
        		_logger.info("informações recuperadas:");
            	String passwd = rs.getString(1);
            	String salt = rs.getString(2);
            	_logger.info("password: " + password);
            	_logger.info("passwd:" + passwd);
            	_logger.info("salt:" + salt);
            	_logger.info("senha calculada: " + getHash(salt, password));
            
            	if (passwd == null || !passwd.equals(getHash(salt, password))) {
                	result = false;
            	}
        	} else {
        		result = false;
        	}
        
        	_logger.info("result: " + result);
    	} catch (SQLException ex) {
    		throw new GemRealmException(ex);
    	} finally {
        	close(rs, stmt, conn);
    	}
        
        _logger.info("******************************** asfdsafasdfasdf **************************************");
        if(!result) {
        	for (String dominio : dominios) {
        		_logger.info(dominio);
        		if(username.substring(username.indexOf("@")).equals("@" + dominio))
            	{
            		try {
            			_logger.info("recuperando conexao SQL...");
            			conn = getConnection();
            			_logger.info("conexão recuperada...");

            			if (conn != null)
            				_logger.info("conexão não nula");
            			stmt = conn.prepareStatement(getLoginGoogleQuery());
            			stmt.setString(1, username);
            			_logger.info("tentando executar a query...");
            			rs = stmt.executeQuery();
            			_logger.info("query executada...");

            			if (rs.next()) {
            				_logger.info("informações recuperadas:");
            				String subject = rs.getString(1);
            				_logger.info("password: " + password);
            				_logger.info("subject:" + subject);

            				if (subject == null || !subject.equals(password)) {
            					result = false;
            				} else {
            					result = true;
            				}
            			} else {
            				result = false;
            			}

            			_logger.info("result: " + result);
            		} catch (SQLException ex) {

            		} finally {
            			close(rs, stmt, conn);
            		}
            	}
        	}
        }
        _logger.info("retornando resultado");
        return result;
    }

    public String getHash(String salt, String password) {
        try {
            String passwd = salt + password;
            MessageDigest digest = MessageDigest.getInstance(getHashAlgorithm());
            digest.update(passwd.getBytes(Charset.forName(getCharset())));
            return Base64.getEncoder().encodeToString(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new GemRealmException(ex);
        }
    }
    
    @Override
    public Enumeration<?> getGroupNames(String username) {
        Vector<String> vector = new Vector<String>();
        List<String> groups = getGroupList(username);

        for (String group : groups) {
            vector.add(group);
        }

        return vector.elements();
    }

    public List<String> getGroupList(String username) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<String> groups = new ArrayList<>();
        _logger.info("recuperando grupos de " + username);

        try {
        	_logger.info("grupos: recuperando conexão...");
            conn = getConnection();
            _logger.info("conexão recuperada!");
            stmt = conn.prepareStatement(getGroupsQuery());
            stmt.setString(1, username);
            _logger.info("executando query: " + getGroupsQuery());
            rs = stmt.executeQuery();

            while (rs.next()) {
                String group = rs.getString(1);
                _logger.info("grupo: " + group);
                groups.add(group);
            }
        } catch (SQLException ex) {
            throw new GemRealmException(ex);
        } finally {
            close(rs, stmt, conn);
        }

        _logger.info("retornando lista de grupos");
        return groups;
    }

}
