package com.base22.carbon.security.handlers;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.base22.carbon.constants.HttpHeaders;
import com.base22.carbon.exceptions.CarbonException;
import com.base22.carbon.models.ErrorResponse;
import com.base22.carbon.models.HttpHeader;
import com.base22.carbon.models.HttpHeaderValue;
import com.base22.carbon.security.constants.AclSR;
import com.base22.carbon.security.models.Application;
import com.base22.carbon.security.models.ApplicationRole;
import com.base22.carbon.security.models.RDFApplicationRole;
import com.base22.carbon.security.utils.AuthenticationUtil;
import com.base22.carbon.utils.HttpUtil;

@Component
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS, value = "request")
public class ApplicationRoleGETRequestHandler extends AbstractApplicationAPIRequestHandler {

	public ResponseEntity<Object> handleRequest(String appIdentifier, String appRoleUUID, HttpServletRequest request, HttpServletResponse response)
			throws CarbonException {

		UUID targetAppRoleUUID = getTargetAppRoleUUID(appRoleUUID);

		ApplicationRole targetAppRole = getTargetAppRole(appIdentifier, targetAppRoleUUID);
		if ( ! targetAppRoleExists(targetAppRole) ) {
			return handleNonExistentAppRole(appIdentifier, targetAppRoleUUID, request, response);
		}

		RDFApplicationRole targetRDFAppRole = targetAppRole.createRDFRepresentation();

		Enumeration<String> preferHeaders = request.getHeaders(HttpHeaders.PREFER);
		HttpHeader preferHeader = new HttpHeader(preferHeaders);
		if ( includeACL(preferHeader) ) {
			injectACLToTargetAppRole(targetAppRole, targetRDFAppRole, response);
		}

		return new ResponseEntity<Object>(targetRDFAppRole, HttpStatus.OK);
	}

	private ResponseEntity<Object> handleNonExistentAppRole(String appIdentifier, UUID targetAppRoleUUID, HttpServletRequest request,
			HttpServletResponse response) {
		String friendlyMessage = "The applicationRole specified wasn't found.";
		String debugMessage = MessageFormat.format("The applicationRole with the UUID: ''{0}'', wasn''t found.", targetAppRoleUUID.toString());

		if ( LOG.isDebugEnabled() ) {
			LOG.debug("xx handleNonExistentAppRole() > {}", debugMessage);
		}

		ErrorResponse errorObject = new ErrorResponse();
		errorObject.setHttpStatus(HttpStatus.NOT_FOUND);
		errorObject.setFriendlyMessage(friendlyMessage);
		errorObject.setDebugMessage(debugMessage);

		return HttpUtil.createErrorResponseEntity(errorObject);
	}

	private UUID getTargetAppRoleUUID(String appRoleUUID) throws CarbonException {
		if ( ! AuthenticationUtil.isUUIDString(appRoleUUID) ) {
			String friendlyMessage = "The applicationRole identifier isn't a UUID.";

			if ( LOG.isDebugEnabled() ) {
				LOG.debug("xx handleNonExistentAppRole() > {}", friendlyMessage);
			}

			ErrorResponse errorObject = new ErrorResponse();
			errorObject.setHttpStatus(HttpStatus.BAD_REQUEST);
			errorObject.setFriendlyMessage(friendlyMessage);
			errorObject.setDebugMessage(friendlyMessage);

			throw new CarbonException(errorObject);
		}

		return AuthenticationUtil.restoreUUID(appRoleUUID);
	}

	private ApplicationRole getTargetAppRole(String appIdentifier, UUID targetAppRoleUUID) throws CarbonException {
		Application targetApplication = null;
		try {
			targetApplication = securedApplicationDAO.findByIdentifier(appIdentifier);
		} catch (CarbonException e) {
			e.getErrorObject().setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			throw e;
		}

		if ( targetApplication == null ) {
			String friendlyMessage = "The application specified wasn't found.";
			String debugMessage = MessageFormat.format("The application with the Identifier: ''{0}'', wasn''t found.", appIdentifier);

			if ( LOG.isDebugEnabled() ) {
				LOG.debug("xx handleNonExistentAppRole() > {}", debugMessage);
			}

			ErrorResponse errorObject = new ErrorResponse();
			errorObject.setHttpStatus(HttpStatus.NOT_FOUND);
			errorObject.setFriendlyMessage(friendlyMessage);
			errorObject.setDebugMessage(debugMessage);

			throw new CarbonException(errorObject);
		}

		ApplicationRole targetAppRole = null;
		try {
			targetAppRole = securedApplicationRoleDAO.findByUUID(targetAppRoleUUID);
		} catch (CarbonException e) {
			e.getErrorObject().setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			throw e;
		}
		return targetAppRole;
	}

	private boolean targetAppRoleExists(ApplicationRole targetAppRole) {
		return targetAppRole != null;
	}

	private boolean includeACL(HttpHeader preferHeader) {
		boolean include = false;
		List<HttpHeaderValue> includePreferences = HttpHeader.filterHeaderValues(preferHeader, "return", "representation", "include", null);

		for (HttpHeaderValue includePreference : includePreferences) {
			String includeValue = includePreference.getExtendingValue();
			if ( includeValue != null ) {
				if ( AclSR.Resources.findByURI(includeValue) == AclSR.Resources.CLASS ) {
					include = true;
				}
			}
		}
		return include;
	}

	private void injectACLToTargetAppRole(ApplicationRole targetAppRole, RDFApplicationRole targetRDFAppRole, HttpServletResponse response)
			throws CarbonException {
		try {
			ldpPermissionService.injectACLToLDPResource(targetAppRole, targetRDFAppRole);
		} catch (CarbonException e) {
			e.getErrorObject().setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			throw e;
		}

		HttpHeaderValue aclPreference = new HttpHeaderValue();
		aclPreference.setMainKey("return");
		aclPreference.setMainValue("representation");
		aclPreference.setExtendingKey("include");
		aclPreference.setExtendingValue(AclSR.Resources.CLASS.getPrefixedURI().getURI());

		response.addHeader("Preference-Applied", aclPreference.toString());
	}
}
