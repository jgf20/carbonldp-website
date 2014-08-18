package com.base22.carbon.security.dao.impl;

import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.base22.carbon.exceptions.CarbonException;
import com.base22.carbon.security.dao.AgentLoginDetailsDAO;
import com.base22.carbon.security.exceptions.DAOException;
import com.base22.carbon.security.models.Agent;
import com.base22.carbon.security.models.JDBCTransactionException;
import com.base22.carbon.security.utils.AuthenticationUtil;

@Service("agentDetailsDAO")
public class JDBCAgentLoginDetailsDAO extends JdbcDAO implements AgentLoginDetailsDAO {

	public static final String TABLE = "agents";
	public static final String UUID_FIELD = "uuid";
	public static final String HEX_UUID_FIELD = "hex_uuid";
	public static final String NAME_FIELD = "name";
	public static final String PASSWORD_FIELD = "password";
	public static final String KEY_FIELD = "api_key";
	public static final String SALT_FIELD = "salt";
	public static final String ENABLED_FIELD = "enabled";

	public static final String EMAILS_TABLE = "agent_emails";
	public static final String EMAIL_INDEX_FIELD = "index";
	public static final String EMAIL_FIELD = "email";

	public static final String EXTERNAL_ID_FIELD = "agent_uuid";

	public static final String AGENTS_ROLES_TABLE = "agents_platform_roles";

	public Agent registerAgentLoginDetails(final Agent agent) throws CarbonException {

		UUID agentUUID = null;
		if ( agent.getUuid() == null ) {
			// The UUID hasn't been set, create one
			agentUUID = UUID.randomUUID();
		} else {
			agentUUID = agent.getUuid();
		}

		final String uuidString = AuthenticationUtil.minimizeUUID(agentUUID);

		// Prepare the password
		String rawPassword = agent.getPassword();
		String hashedPassword = null;
		final String salt = AuthenticationUtil.generateRandomSalt();
		String saltedPassword = AuthenticationUtil.saltPassword(rawPassword, salt);
		try {
			hashedPassword = AuthenticationUtil.hashPassword(saltedPassword);
		} catch (NoSuchAlgorithmException e) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debug("xx registerAgentLoginDetails() > Exception Stacktrace:", e);
			}
			if ( LOG.isErrorEnabled() ) {
				LOG.error("<< registerAgentLoginDetails() > The raw password couldn't be hashed.");
			}
			throw new DAOException("The raw password couldn't be hashed.");
		}

		final String hashedPasswordToInsert = hashedPassword;

		// TODO: Support statement batches
		// Save the agent's login details
		int insertionResult;
		JDBCUpdateTransactionTemplate template = new JDBCUpdateTransactionTemplate();
		try {
			//@formatter:off
			insertionResult = template.execute(securityJDBCDataSource, new JDBCUpdateTransactionCallback() {
				//@formatter:on
				@Override
				public StringBuilder prepareSQLQuery(StringBuilder queryBuilder) {
					//@formatter:off
					queryBuilder
						.append("INSERT INTO ")
						.append(TABLE)
						.append(" (")
							.append(UUID_FIELD)
							.append(", ")
							.append(NAME_FIELD)
							.append(", ")
							.append(PASSWORD_FIELD)
							.append(", ")
							.append(KEY_FIELD)
							.append(", ")
							.append(SALT_FIELD)
							.append(", ")
							.append(ENABLED_FIELD)
						.append(") VALUES (UNHEX(?), ?, ?, ?, ?, ?)")
					;
					//@formatter:on
					return queryBuilder;
				}

				@Override
				public PreparedStatement prepareStatement(PreparedStatement statement) throws SQLException {
					statement.setString(1, uuidString);
					setStringOrNull(statement, 2, agent.getFullName());
					setStringOrNull(statement, 3, hashedPasswordToInsert);
					setStringOrNull(statement, 4, agent.getKey());
					statement.setString(5, salt);
					statement.setBoolean(6, agent.isEnabled());

					return statement;
				}
			});
		} catch (JDBCTransactionException e) {
			if ( LOG.isErrorEnabled() ) {
				LOG.error("<< registerAgentLoginDetails() > The agentLoginDetails couldn't be registered.");
			}
			throw e;
		}

		if ( insertionResult != 1 ) {
			// TODO: Can we get the reason in this case?
			if ( LOG.isErrorEnabled() ) {
				LOG.error("<< registerAgentLoginDetails() > The agentLoginDetails couldn't be registered.");
			}
			throw new DAOException("The agentLoginDetails couldn't be registered.");
		}

		addEmailToAgent(agentUUID, 1, agent.getMainEmail());

		agent.setUuid(agentUUID);
		agent.setPassword(hashedPassword);
		agent.setSalt(salt);

		return agent;
	}

	public void addEmailToAgent(UUID agentUUID, final int index, final String email) throws CarbonException {
		final String uuidString = AuthenticationUtil.minimizeUUID(agentUUID);

		int insertionResult;
		JDBCUpdateTransactionTemplate template = new JDBCUpdateTransactionTemplate();
		try {
			//@formatter:off
			insertionResult = template.execute(securityJDBCDataSource, new JDBCUpdateTransactionCallback() {
				//@formatter:on
				@Override
				public StringBuilder prepareSQLQuery(StringBuilder queryBuilder) {
					//@formatter:off
					queryBuilder
						.append("INSERT INTO ")
							.append(EMAILS_TABLE)
						.append(" (")
							.append(EXTERNAL_ID_FIELD)
							.append(", `")
							.append(EMAIL_INDEX_FIELD)
							.append("`, ")
							.append(EMAIL_FIELD)
						.append(") VALUES (UNHEX(?), ?, ?)")
					;
					//@formatter:on
					return queryBuilder;
				}

				@Override
				public PreparedStatement prepareStatement(PreparedStatement statement) throws SQLException {
					statement.setString(1, uuidString);
					statement.setInt(2, 1);
					statement.setString(3, email);
					return statement;
				}
			});
		} catch (JDBCTransactionException e) {
			if ( LOG.isErrorEnabled() ) {
				LOG.error("<< addEmailToAgent() > The agentLoginDetails couldn't be registered.");
			}
			throw e;
		}

		if ( insertionResult != 1 ) {
			// TODO: Can we get the reason in this case?
			if ( LOG.isErrorEnabled() ) {
				LOG.error("<< addEmailToAgent() > The agentLoginDetails couldn't be registered.");
			}
			throw new DAOException("The email couldn't be added to the agent.");
		}

	}

	public Agent findByUUID(UUID uuid) throws CarbonException {
		Agent agent = null;

		final String uuidString = AuthenticationUtil.minimizeUUID(uuid);

		JDBCQueryTransactionTemplate template = new JDBCQueryTransactionTemplate();
		try {
			//@formatter:off
			agent = template.execute(securityJDBCDataSource, new JDBCQueryTransactionCallback<Agent>() {
				//@formatter:off
				@Override
				public StringBuilder prepareSQLQuery(StringBuilder sqlBuilder) {
					//@formatter:off
					sqlBuilder
						.append("SELECT ").append(TABLE).append(".*, HEX(")
							.append(TABLE).append(".").append(UUID_FIELD)
						.append(") AS ")
							.append(HEX_UUID_FIELD)
						.append(", ").append(EMAILS_TABLE).append(".").append(EMAIL_INDEX_FIELD)
						.append(", ").append(EMAILS_TABLE).append(".").append(EMAIL_FIELD)
						.append(" FROM ")
							.append(TABLE)
						.append(" LEFT JOIN ").append(EMAILS_TABLE)
						.append(" ON ").append(TABLE).append(".").append(UUID_FIELD).append(" = ").append(EMAILS_TABLE).append(".").append(EXTERNAL_ID_FIELD)
						.append(" WHERE ")
							.append(TABLE).append(".").append(UUID_FIELD).append(" = UNHEX(?)")
						.append(" ORDER BY ")
							.append(EMAILS_TABLE).append(".").append(EMAIL_INDEX_FIELD).append(" ASC")
					;
					/*
					
					SELECT agents.*, agent_emails.index, agent_emails.email
					FROM `agents`
					LEFT JOIN agent_emails
					ON agents.uuid = agent_emails.agent_uuid
					WHERE agents.uuid = UNHEX("2d5f530f7a024e4fefbfbd547aefbfbd")
					ORDER BY agent_emails.index

					*/
					//@formatter:on
					return sqlBuilder;
				}

				@Override
				public PreparedStatement prepareStatement(PreparedStatement statement) throws SQLException {
					statement.setString(1, uuidString);
					return statement;
				}

				@Override
				public Agent interpretResultSet(ResultSet resultSet) throws SQLException {
					Agent agent = null;

					agent = populateAgentWithLoginDetails(resultSet);

					return agent;
				}

			});
		} catch (JDBCTransactionException e) {
			if ( LOG.isErrorEnabled() ) {
				LOG.error("<< findByUUID() > There was a problem while trying to find the agentLoginDetails with UUID: '{}'.", uuid.toString());
			}
			throw e;
		}

		return agent;
	}

	public Agent findByEmail(final String email) throws CarbonException {
		Agent agent = null;

		JDBCQueryTransactionTemplate template = new JDBCQueryTransactionTemplate();
		try {
			//@formatter:off
			agent = template.execute(securityJDBCDataSource, new JDBCQueryTransactionCallback<Agent>() {
				//@formatter:off
				@Override
				public StringBuilder prepareSQLQuery(StringBuilder sqlBuilder) {
					//@formatter:off
					sqlBuilder
						.append("SELECT ").append(TABLE).append(".*, HEX(")
							.append(TABLE).append(".").append(UUID_FIELD)
						.append(") AS ")
							.append(HEX_UUID_FIELD)
						.append(", ").append(EMAILS_TABLE).append(".").append(EMAIL_INDEX_FIELD)
						.append(", ").append(EMAILS_TABLE).append(".").append(EMAIL_FIELD)
						.append(" FROM ")
							.append(TABLE)
						.append(" LEFT JOIN ").append(EMAILS_TABLE)
						.append(" ON ").append(TABLE).append(".").append(UUID_FIELD).append(" = ").append(EMAILS_TABLE).append(".").append(EXTERNAL_ID_FIELD)
						
						.append(" WHERE EXISTS (")
							.append(" SELECT 1 FROM ").append(EMAILS_TABLE)
							.append(" WHERE ").append(TABLE).append(".").append(UUID_FIELD).append(" = ").append(EXTERNAL_ID_FIELD).append(" AND ").append(EMAIL_FIELD).append(" = ?")
						.append(" )")
						
						.append(" ORDER BY ")
							.append(EMAILS_TABLE).append(".").append(EMAIL_INDEX_FIELD).append(" ASC")
					;
					/*
					
					SELECT agents.*, agent_emails.index, agent_emails.email
					FROM agents
					LEFT JOIN agent_emails
					ON agents.uuid = agent_emails.agent_uuid
					WHERE EXISTS ( 
					    SELECT 1 FROM agent_emails 
					    WHERE agents.uuid = agent_uuid AND email = "some@example.com"
					)
					ORDER BY agent_emails.index
					
					 */
					//@formatter:on
					return sqlBuilder;
				}

				@Override
				public PreparedStatement prepareStatement(PreparedStatement statement) throws SQLException {
					statement.setString(1, email);
					return statement;
				}

				@Override
				public Agent interpretResultSet(ResultSet resultSet) throws SQLException {
					Agent agent = null;

					agent = populateAgentWithLoginDetails(resultSet);

					return agent;
				}

			});
		} catch (JDBCTransactionException e) {
			if ( LOG.isErrorEnabled() ) {
				LOG.error("<< findByUsername() > There was a problem while trying to find the agent with the email: '{}'.", email);
			}
			throw e;
		}

		return agent;
	}

	public Agent findByKey(final String key) throws CarbonException {
		Agent agent = null;

		JDBCQueryTransactionTemplate template = new JDBCQueryTransactionTemplate();
		try {
			//@formatter:off
			agent = template.execute(securityJDBCDataSource, new JDBCQueryTransactionCallback<Agent>() {
				//@formatter:off
				@Override
				public StringBuilder prepareSQLQuery(StringBuilder sqlBuilder) {
					//@formatter:off
					sqlBuilder
						.append("SELECT ").append(TABLE).append(".*, HEX(")
							.append(TABLE).append(".").append(UUID_FIELD)
						.append(") AS ")
							.append(HEX_UUID_FIELD)
						.append(", ").append(EMAILS_TABLE).append(".").append(EMAIL_INDEX_FIELD)
						.append(", ").append(EMAILS_TABLE).append(".").append(EMAIL_FIELD)
						.append(" FROM ")
							.append(TABLE)
						.append(" LEFT JOIN ").append(EMAILS_TABLE)
						.append(" ON ").append(TABLE).append(".").append(UUID_FIELD).append(" = ").append(EMAILS_TABLE).append(".").append(EXTERNAL_ID_FIELD)
						.append(" WHERE ")
							.append(TABLE).append(".").append(KEY_FIELD).append(" = ?")
						.append(" ORDER BY ")
							.append(EMAILS_TABLE).append(".").append(EMAIL_INDEX_FIELD).append(" ASC")
					;
					//@formatter:on
					return sqlBuilder;
				}

				@Override
				public PreparedStatement prepareStatement(PreparedStatement statement) throws SQLException {
					statement.setString(1, key);
					return statement;
				}

				@Override
				public Agent interpretResultSet(ResultSet resultSet) throws SQLException {
					Agent agent = null;

					agent = populateAgentWithLoginDetails(resultSet);

					return agent;
				}

			});
		} catch (JDBCTransactionException e) {
			if ( LOG.isErrorEnabled() ) {
				LOG.error("<< findByUsername() > There was a problem while trying to find the agentLoginDetails with key: '{}'.", key);
			}
			throw e;
		}

		return agent;
	}

	public boolean agentEmailExists(final String email) throws CarbonException {
		Boolean exists = false;
		JDBCQueryTransactionTemplate template = new JDBCQueryTransactionTemplate();
		try {
			//@formatter:off
			exists = template.execute(securityJDBCDataSource, new JDBCQueryTransactionCallback<Boolean>() {
				//@formatter:off
				@Override
				public StringBuilder prepareSQLQuery(StringBuilder sqlBuilder) {
					//@formatter:off
					sqlBuilder
						.append("SELECT 1 FROM ").append(EMAILS_TABLE)
						.append(" WHERE ").append(EMAIL_FIELD).append(" = ?")
					;
					//@formatter:on
					return sqlBuilder;
				}

				@Override
				public PreparedStatement prepareStatement(PreparedStatement statement) throws SQLException {
					statement.setString(1, email);
					return statement;
				}

				@Override
				public Boolean interpretResultSet(ResultSet resultSet) throws SQLException {
					return resultSet.next();
				}

			});
		} catch (JDBCTransactionException e) {
			if ( LOG.isErrorEnabled() ) {
				LOG.error("<< agentEmailExists() > There was a problem while trying to find if an agent exists with the email: '{}'.", email);
			}
			throw e;
		}

		return exists;
	}

	private Agent populateAgentWithLoginDetails(ResultSet resultSet) throws SQLException {
		Agent agent = null;
		List<String> emails = new ArrayList<String>();

		if ( resultSet.next() ) {
			String uuidString = resultSet.getString(HEX_UUID_FIELD);
			String fullName = resultSet.getString(NAME_FIELD);
			String password = resultSet.getString(PASSWORD_FIELD);
			String key = resultSet.getString(KEY_FIELD);
			String salt = resultSet.getString(SALT_FIELD);
			boolean enabled = resultSet.getBoolean(ENABLED_FIELD);

			String email = resultSet.getString(EMAIL_FIELD);

			agent = new Agent();
			agent.setUuid(uuidString);
			agent.setFullName(fullName);
			agent.setPassword(password);
			agent.setKey(key);
			agent.setSalt(salt);
			agent.setEnabled(enabled);

			emails.add(email);

			// Add the other emails (if any)
			while (resultSet.next()) {
				email = resultSet.getString(EMAIL_FIELD);
				emails.add(email);
			}

			agent.setEmails(emails);
		}

		return agent;
	}

}
