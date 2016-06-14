package com.carbonldp.authentication.web;

import com.carbonldp.ldp.web.AbstractLDPController;
import com.carbonldp.rdf.RDFDocument;
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
 * @since 0.36.0
 */

@Controller
@RequestMapping( "/apps/*/auth-tickets/" )
public class AppTicketAuthenticationController extends AbstractLDPController {
	TicketAuthenticationRequestHandler authenticationHandler;

	@RequestMapping( method = RequestMethod.POST )
	public ResponseEntity<Object> handleRDFPost( @RequestBody RDFDocument requestDocument, HttpServletRequest request, HttpServletResponse response ) {
		return authenticationHandler.handleRequest( requestDocument, request, response );
	}

	@Autowired
	public void setAuthenticationHandler( TicketAuthenticationRequestHandler authenticationHandler ) {
		this.authenticationHandler = authenticationHandler;
	}
}
