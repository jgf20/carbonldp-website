package com.carbonldp.ldp.sources;

import com.carbonldp.descriptions.APIPreferences;
import com.carbonldp.ldp.web.AbstractRequestWithBodyHandler;
import com.carbonldp.models.EmptyResponse;
import com.carbonldp.rdf.RDFDocument;
import com.carbonldp.rdf.RDFResource;
import com.carbonldp.web.exceptions.BadRequestException;
import com.carbonldp.web.exceptions.NotFoundException;
import org.openrdf.model.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Transactional
public abstract class AbstractPUTRequestHandler<E extends RDFResource> extends AbstractRequestWithBodyHandler<E> {
	public ResponseEntity<Object> handleRequest( RDFDocument requestDocument, HttpServletRequest request, HttpServletResponse response ) {
		setUp( request, response );

		URI targetURI = getTargetURI( request );
		if ( ! targetResourceExists( targetURI ) ) {
			throw new NotFoundException();
		}

		String requestETag = getRequestETag();
		checkPrecondition( targetURI, requestETag );

		RDFResource requestDocumentResource = requestDocument.getDocumentResource();

		validateDocumentResource( targetURI, requestDocumentResource );
		E documentResourceView = getDocumentResourceView( requestDocumentResource );

		replaceResource( targetURI, documentResourceView );

		addTypeLinkHeader( APIPreferences.InteractionModel.RDF_SOURCE );
		return createSuccessfulResponse( targetURI );
	}

	@Override
	protected void validateDocumentResource( URI targetURI, RDFResource requestDocumentResource ) {
		super.validateDocumentResource( targetURI, requestDocumentResource );
		if ( ! targetURI.equals( requestDocumentResource.getIRI() ) ) throw new BadRequestException( 0x2203 );

	}

	protected abstract E getDocumentResourceView( RDFResource requestDocumentResource );

	protected ResponseEntity<Object> createSuccessfulResponse( URI affectedResourceURI ) {
		String eTag = sourceService.getETag( affectedResourceURI );

		setStrongETagHeader( eTag );
		return new ResponseEntity<>( new EmptyResponse(), HttpStatus.OK );
	}

	protected abstract void replaceResource( URI targetURI, E documentResourceView );
}