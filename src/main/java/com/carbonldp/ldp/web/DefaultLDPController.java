package com.carbonldp.ldp.web;

import com.carbonldp.rdf.RDFDocument;
import com.carbonldp.web.exceptions.NotImplementedException;
import org.openrdf.model.impl.AbstractModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

@Controller
@RequestMapping( "/**" )
public class DefaultLDPController extends AbstractLDPController {
	private BaseOPTIONSRequestHandler optionsHandler;
	private BaseGETRequestHandler getHandler;
	private BaseRDFPostRequestHandler rdfPOSTHandler;
	private BaseNonRDFPostRequestHandler nonRDFPostHandler;
	private BaseRDFPutRequestHandler putRDFHandler;
	private BaseNonRDFPutRequestHandler putNonRDFHandler;
	private BasePATCHRequestHandler patchHandler;
	private BaseDELETERequestHandler deleteHandler;

	private BaseSPARQLQueryPOSTRequestHandler sparqlQueryHandler;

	@RequestMapping( method = RequestMethod.OPTIONS )
	public ResponseEntity<Object> handleOPTIONS( HttpServletRequest request, HttpServletResponse response ) {
		return optionsHandler.handleRequest( request, response );
	}

	@RequestMapping( method = {RequestMethod.GET, RequestMethod.HEAD} )
	public ResponseEntity<Object> handleGET( HttpServletRequest request, HttpServletResponse response ) {
		return getHandler.handleRequest( request, response );
	}

	@RequestMapping( method = RequestMethod.POST, consumes = {
		"application/ld+json",
		"text/turtle"
	} )
	public ResponseEntity<Object> handleRDFPost( @RequestBody RDFDocument requestDocument, HttpServletRequest request, HttpServletResponse response ) {
		return rdfPOSTHandler.handleRequest( requestDocument, request, response );
	}

	@RequestMapping( method = RequestMethod.POST)
	public ResponseEntity<Object> handleNonRDFPost( InputStream bodyInputStream, HttpServletRequest request, HttpServletResponse response ) {
		return nonRDFPostHandler.handleRequest( bodyInputStream, request, response );
	}

	@RequestMapping( method = RequestMethod.POST, consumes = "application/sparql-query" )
	public ResponseEntity<Object> handleSPARQLPost( @RequestBody String query, HttpServletRequest request, HttpServletResponse response ) {
		return sparqlQueryHandler.handleRequest( query, request, response );
	}

	@RequestMapping( method = RequestMethod.POST, consumes = "multipart/*" )
	public ResponseEntity<Object> handleMultipartPost( MultipartFile requestBody, HttpServletRequest request, HttpServletResponse response ) {
		throw new NotImplementedException();
	}

	@RequestMapping( method = RequestMethod.PUT, consumes = {
		"application/ld+json",
		"text/turtle"
	} )
	public ResponseEntity<Object> handleRDFPUT( @RequestBody RDFDocument requestDocument, HttpServletRequest request, HttpServletResponse response ) {
		return putRDFHandler.handleRequest( requestDocument, request, response );
	}

	@RequestMapping( method = RequestMethod.PUT )
	public ResponseEntity<Object> handleNonRDFPUT( InputStream bodyInputStream, HttpServletRequest request, HttpServletResponse response ) {
		return putNonRDFHandler.handleRequest( bodyInputStream, request, response );
	}

	@RequestMapping( method = RequestMethod.PATCH )
	public ResponseEntity<Object> handlePATCH( @RequestBody AbstractModel requestModel, HttpServletRequest request, HttpServletResponse response ) {
		return patchHandler.handleRequest( requestModel, request, response );
	}

	@RequestMapping( method = RequestMethod.DELETE )
	public ResponseEntity<Object> handleDELETE( HttpServletRequest request, HttpServletResponse response ) {
		return deleteHandler.handleRequest( request, response );
	}

	@Autowired
	public void setOptionsHandler( BaseOPTIONSRequestHandler optionsHandler ) { this.optionsHandler = optionsHandler; }

	@Autowired
	public void setGetHandler( BaseGETRequestHandler getHandler ) { this.getHandler = getHandler; }

	@Autowired
	public void setRDFPOSTHandler( BaseRDFPostRequestHandler rdfPOSTHandler ) { this.rdfPOSTHandler = rdfPOSTHandler; }

	@Autowired
	public void setNonRDFPostHandler( BaseNonRDFPostRequestHandler baseNonRDFPostRequestHandler ) {this.nonRDFPostHandler = baseNonRDFPostRequestHandler;}

	@Autowired
	public void setSPARQLQueryHandler( BaseSPARQLQueryPOSTRequestHandler sparqlQueryHandler ) { this.sparqlQueryHandler = sparqlQueryHandler; }

	@Autowired
	public void setPutRDFHandler( BaseRDFPutRequestHandler putRDFHandler ) { this.putRDFHandler = putRDFHandler; }

	@Autowired
	public void setPutNonRDFHandler( BaseNonRDFPutRequestHandler putNonRDFHandler ) {this.putNonRDFHandler = putNonRDFHandler;}

	@Autowired
	public void setPatchHandler( BasePATCHRequestHandler patchHandler ) { this.patchHandler = patchHandler; }

	@Autowired
	public void setDeleteHandler( BaseDELETERequestHandler deleteHandler ) { this.deleteHandler = deleteHandler; }
}
