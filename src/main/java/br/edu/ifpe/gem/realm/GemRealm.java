package br.edu.ifpe.gem.realm;

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.appserv.security.AppservRealm;
import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.common.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
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

	public static final String GROUPS_SQL_QUERY = "groups-sql-query";
	public static final String JTA_DATA_SOURCE = "jta-data-source";
	public static final String DOMINIOSINSTITUNCIONAIS = "dominiosInstituncionais";
	public static final String DOMINIOSALUNOS = "dominiosAluno";
	private static List<String> dominios; 

	private static DataSource dataSource;
	
	@SuppressWarnings("unchecked")
	private Connection getConnection() {
		try {
			synchronized (this) {
				if (dataSource == null) {
					ActiveDescriptor<ConnectorRuntime> cr = (ActiveDescriptor<ConnectorRuntime>) Util.getDefaultHabitat().getBestDescriptor(BuilderHelper.createContractFilter(ConnectorRuntime.class.getName()));
					ConnectorRuntime connectorRuntime = Util.getDefaultHabitat().getServiceHandle(cr).getService();
					dataSource = (DataSource) connectorRuntime.lookupNonTxResource(getJtaDataSource(), false);
				}
			}

			return dataSource.getConnection();
		} catch (NamingException | SQLException ex) {
			throw new GemRealmException(ex);
		} catch (Exception ex) {
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

	public String getJtaDataSource() {
		return super.getProperty(JTA_DATA_SOURCE);
	}

	public String getGroupsQuery() {
		return super.getProperty(GROUPS_SQL_QUERY);
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
		setProperty(GROUPS_SQL_QUERY, properties.getProperty(GROUPS_SQL_QUERY));
		setProperty(JTA_DATA_SOURCE, properties.getProperty(JTA_DATA_SOURCE));
		setProperty(DOMINIOSINSTITUNCIONAIS, properties.getProperty(DOMINIOSINSTITUNCIONAIS));
		setProperty(DOMINIOSALUNOS, properties.getProperty(DOMINIOSALUNOS));

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
		boolean result = false;

		if(!result) {
			for (String dominio : dominios) {
				if(username.substring(username.indexOf("@")).equals("@" + dominio)){
					result = true;
					break;
				}
			}
		}

		return result;
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

		try {
			conn = getConnection();
			stmt = conn.prepareStatement(getGroupsQuery());
			stmt.setString(1, username);
			rs = stmt.executeQuery();

			while (rs.next()) {
				String group = rs.getString(1);
				groups.add(group);
			}
		} catch (SQLException ex) {
			throw new GemRealmException(ex);
		} finally {
			close(rs, stmt, conn);
		}

		return groups;
	}

}
