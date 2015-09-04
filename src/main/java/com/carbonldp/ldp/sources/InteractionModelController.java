package com.carbonldp.ldp.sources;

import com.carbonldp.descriptions.APIPreferences;
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

@Controller( "rdfSource:interactionModelController" )
@InteractionModel( APIPreferences.InteractionModel.RDF_SOURCE )
@RequestMapping( "/**" )
public class InteractionModelController {

	private BasePUTRequestHandler putRequestHandler;

	@RequestMapping( method = RequestMethod.PUT )
	public ResponseEntity<Object> handleRDFPUTToRDFSource( @RequestBody RDFDocument requestDocument, HttpServletRequest request, HttpServletResponse response ) {
		return putRequestHandler.handleRequest( requestDocument, request, response );
	}

	@Autowired
	public void setPutRequestHandler( BasePUTRequestHandler putRequestHandler ) {
		this.putRequestHandler = putRequestHandler;
	}
}