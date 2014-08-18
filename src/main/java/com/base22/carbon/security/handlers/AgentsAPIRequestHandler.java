package com.base22.carbon.security.handlers;

import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.base22.carbon.exceptions.CarbonException;
import com.base22.carbon.models.ErrorResponse;
import com.base22.carbon.security.models.Agent;
import com.base22.carbon.security.models.Agent.Properties;
import com.base22.carbon.security.models.PlatformRole;
import com.base22.carbon.security.models.RDFAgent;
import com.base22.carbon.security.models.RDFAgentFactory;
import com.base22.carbon.security.utils.AuthenticationUtil;
import com.base22.carbon.utils.HttpUtil;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

@Component
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS, value = "request")
public class AgentsAPIRequestHandler extends AbstractApplicationAPIRequestHandler {

	public ResponseEntity<Object> handleAgentRegistration(Model requestModel, HttpServletRequest request, HttpServletResponse response) throws CarbonException {

		Resource requestResource = getRequestModelMainResource(requestModel);
		RDFAgent requestRDFAgent = getRequestRDFAgent(requestResource);

		validateRequestRDFAgent(requestRDFAgent);

		if ( emailIsAlreadyInUse(requestRDFAgent) ) {
			return handleEmailAlreadyRegistred(requestRDFAgent, request, response);
		}

		Agent requestAgent = getRequestAgent(requestRDFAgent);

		if ( requestAgent.getKey() == null && configurationService.createAPIKeyForNewAgents() ) {
			generateAPIKeyForAgent(requestAgent);
		}

		requestAgent.setEnabled(true);

		requestAgent = createAgentLoginDetails(requestAgent);

		addAgentToDefaultPlatformRole(requestAgent);

		RDFAgent targetAgent = requestAgent.createRDFRepresentation();
		targetAgent.setAPIKey(requestAgent.getKey());

		return new ResponseEntity<Object>(targetAgent, HttpStatus.CREATED);
	}

	private ResponseEntity<Object> handleEmailAlreadyRegistred(RDFAgent requestRDFAgent, HttpServletRequest request, HttpServletResponse response) {
		String friendlyMessage = "The email supplied is already in use.";
		String debugMessage = MessageFormat.format("The email supplied is already in use.", requestRDFAgent.getMainEmail());

		if ( LOG.isDebugEnabled() ) {
			LOG.debug("<< handleEmailAlreadyRegistred() > {}", debugMessage);
		}

		ErrorResponse errorObject = new ErrorResponse();
		errorObject.setHttpStatus(HttpStatus.CONFLICT);
		errorObject.setFriendlyMessage(friendlyMessage);
		errorObject.setDebugMessage(debugMessage);

		return HttpUtil.createErrorResponseEntity(errorObject);
	}

	protected RDFAgent getRequestRDFAgent(Resource requestResource) throws CarbonException {
		RDFAgent requestRDFAgent = null;
		RDFAgentFactory factory = new RDFAgentFactory();
		try {
			requestRDFAgent = factory.create(requestResource);
		} catch (CarbonException e) {
			e.getErrorObject().setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			throw e;
		}
		return requestRDFAgent;
	}

	protected void validateRequestRDFAgent(RDFAgent requestRDFAgent) throws CarbonException {
		StringBuilder bodyIssueBuilder = null;

		if ( requestRDFAgent.getUUID() != null ) {
			bodyIssueBuilder = new StringBuilder();

			bodyIssueBuilder.append(Properties.UUID.getPrefixedURI().getSlug());
			bodyIssueBuilder.append(" > This property is set by the server. It cannot be specified upon creation.");
		}

		if ( requestRDFAgent.getFullName() == null || requestRDFAgent.getFullName().trim().length() == 0 ) {
			if ( bodyIssueBuilder == null ) {
				bodyIssueBuilder = new StringBuilder();
			} else {
				bodyIssueBuilder.append("\n");
			}

			bodyIssueBuilder.append(Properties.FULL_NAME.getPrefixedURI().getSlug());
			bodyIssueBuilder.append(" > Required.");
		}

		if ( requestRDFAgent.getMainEmail() == null ) {
			if ( bodyIssueBuilder == null ) {
				bodyIssueBuilder = new StringBuilder();
			} else {
				bodyIssueBuilder.append("\n");
			}

			bodyIssueBuilder.append(Properties.EMAIL.getPrefixedURI().getSlug());
			bodyIssueBuilder.append(" > Required.");
		} else {
			// TODO: Check that it is a valid email
			EmailValidator emailValidator = EmailValidator.getInstance(false);
			if ( ! emailValidator.isValid(requestRDFAgent.getMainEmail()) ) {
				if ( bodyIssueBuilder == null ) {
					bodyIssueBuilder = new StringBuilder();
				} else {
					bodyIssueBuilder.append("\n");
				}

				bodyIssueBuilder.append(Properties.EMAIL.getPrefixedURI().getSlug());
				bodyIssueBuilder.append(" > Not a valid email address.");
			}
		}

		if ( requestRDFAgent.getPassword() == null ) {
			if ( bodyIssueBuilder == null ) {
				bodyIssueBuilder = new StringBuilder();
			} else {
				bodyIssueBuilder.append("\n");
			}

			bodyIssueBuilder.append(Properties.PASSWORD.getPrefixedURI().getSlug());
			bodyIssueBuilder.append(" > Required.");
		}

		if ( bodyIssueBuilder != null ) {
			String friendlyMessage = "The body of the request is not valid.";
			String debugMessage = "The properties of the Agent sent are not valid.";

			if ( LOG.isDebugEnabled() ) {
				LOG.debug("<< validateRequestRDFAgent() > {}", debugMessage);
			}

			ErrorResponse errorObject = new ErrorResponse();
			errorObject.setHttpStatus(HttpStatus.BAD_REQUEST);
			errorObject.setFriendlyMessage(friendlyMessage);
			errorObject.setDebugMessage(debugMessage);
			errorObject.setEntityBodyIssue(null, bodyIssueBuilder.toString());

			throw new CarbonException(errorObject);
		}
	}

	protected boolean emailIsAlreadyInUse(RDFAgent requestRDFAgent) throws CarbonException {
		boolean exists = false;
		try {
			exists = unsecuredAgentDetailsDAO.agentEmailExists(requestRDFAgent.getMainEmail());
		} catch (CarbonException e) {
			e.getErrorObject().setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			throw e;
		}

		return exists;
	}

	protected Agent getRequestAgent(RDFAgent requestRDFAgent) throws CarbonException {
		Agent requestAgent = new Agent();
		try {
			requestAgent.recoverFromLDPR(requestRDFAgent);
		} catch (CarbonException e) {
			e.getErrorObject().setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			throw e;
		}
		return requestAgent;
	}

	protected Agent createAgentLoginDetails(Agent requestAgent) throws CarbonException {
		try {
			requestAgent = unsecuredAgentDetailsDAO.registerAgentLoginDetails(requestAgent);
		} catch (CarbonException e) {
			e.getErrorObject().setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			throw e;
		}

		return requestAgent;
	}

	private void generateAPIKeyForAgent(Agent requestAgent) {
		requestAgent.setKey(AuthenticationUtil.generateRandomSalt());
	}

	private void addAgentToDefaultPlatformRole(Agent requestAgent) throws CarbonException {
		PlatformRole defaultRole = null;
		try {
			defaultRole = unsecuredPlatformRoleDAO.findByName(configurationService.getDefaultPlatformRole());
		} catch (CarbonException e) {
			e.getErrorObject().setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			throw e;
		}

		try {
			unsecuredPlatformRoleDAO.addAgentToRole(requestAgent, defaultRole);
		} catch (CarbonException e) {
			e.getErrorObject().setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			throw e;
		}
	}
}
