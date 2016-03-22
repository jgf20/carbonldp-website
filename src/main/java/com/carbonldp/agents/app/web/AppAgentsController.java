package com.carbonldp.agents.app.web;

import com.carbonldp.Consts;
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
 * @since 0.14.0-ALPHA
 */

@Controller
@RequestMapping( value = "/apps/*/agents/" )
public class AppAgentsController extends AbstractLDPController {

	private AppAgentsPOSTHandler postRequestHandler;
	private AppAgentsDELETEHandler deleteRequestHandler;

	@RequestMapping( method = RequestMethod.POST, consumes = {
		Consts.TURTLE,
		Consts.JSONLD,
		Consts.RDFJSON,
		Consts.RDFXML,
		Consts.TRIG,
		Consts.NTRIPLES,
		Consts.N3,
		Consts.TRIX,
		Consts.BINARY,
		Consts.NQUADS,
		Consts.RDFA
	} )
	public ResponseEntity<Object> registerAgent( @RequestBody RDFDocument requestDocument, HttpServletRequest request, HttpServletResponse response ) {
		return postRequestHandler.handleRequest( requestDocument, request, response );
	}

	@RequestMapping( method = RequestMethod.DELETE )
	public ResponseEntity<Object> deleteAgent( @RequestBody( required = false ) RDFDocument requestDocument, HttpServletRequest request, HttpServletResponse response ) {
		return deleteRequestHandler.handleRequest( requestDocument, request, response );
	}

	@Autowired
	public void setPOSTRequestHandler( AppAgentsPOSTHandler postRequestHandler ) {
		this.postRequestHandler = postRequestHandler;
	}

	@Autowired
	public void setDeleteRequestHandler( AppAgentsDELETEHandler deleteRequestHandler ) {
		this.deleteRequestHandler = deleteRequestHandler;
	}

}
