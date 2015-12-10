package com.carbonldp.apps.roles.web;

import com.carbonldp.descriptions.APIPreferences;
import com.carbonldp.ldp.web.AbstractLDPController;
import com.carbonldp.rdf.RDFDocument;
import com.carbonldp.web.config.InteractionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author NestorVenegas
 * @since _version_
 */

@Controller

@InteractionModel( APIPreferences.InteractionModel.CONTAINER )
@RequestMapping( value = "apps/*/roles/*/" )
public class AppRoleController extends AbstractLDPController {

	private AppRolePUTHandler putHandler;

	@RequestMapping( method = RequestMethod.PUT)
	public ResponseEntity<Object> defineParentChildRelation( @RequestBody RDFDocument requestDocument, HttpServletRequest request, HttpServletResponse response ) {
		return putHandler.handleRequest( requestDocument, request, response );
	}

	@Autowired
	public void setAppRolesPutHandler( AppRolePUTHandler putHandler ) {
		this.putHandler = putHandler;
	}
}
