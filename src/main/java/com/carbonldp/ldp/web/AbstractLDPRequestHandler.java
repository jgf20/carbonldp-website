package com.carbonldp.ldp.web;

import com.carbonldp.Consts;
import com.carbonldp.HTTPHeaders;
import com.carbonldp.config.ConfigurationRepository;
import com.carbonldp.descriptions.APIPreferences.InteractionModel;
import com.carbonldp.http.Link;
import com.carbonldp.ldp.containers.ContainerDescription;
import com.carbonldp.ldp.containers.ContainerService;
import com.carbonldp.ldp.nonrdf.NonRDFSourceService;
import com.carbonldp.ldp.sources.RDFSourceDescription;
import com.carbonldp.ldp.sources.RDFSourceService;
import com.carbonldp.models.HTTPHeader;
import com.carbonldp.models.HTTPHeaderValue;
import com.carbonldp.rdf.IRIObject;
import com.carbonldp.rdf.RDFNodeEnum;
import com.carbonldp.rdf.RDFResource;
import com.carbonldp.sparql.SPARQLService;
import com.carbonldp.utils.*;
import com.carbonldp.web.AbstractRequestHandler;
import com.carbonldp.web.exceptions.BadRequestException;
import com.carbonldp.web.exceptions.PreconditionFailedException;
import com.carbonldp.web.exceptions.PreconditionRequiredException;
import org.joda.time.DateTime;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.impl.AbstractModel;
import org.openrdf.model.impl.URIImpl;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class AbstractLDPRequestHandler extends AbstractRequestHandler {
	public static final HTTPHeaderValue interactionModelApplied;
	public static final HTTPHeaderValue returnRepresentationApplied;

	static {
		interactionModelApplied = new HTTPHeaderValue();
		interactionModelApplied.setMainKey( "rel" );
		interactionModelApplied.setMainValue( "interaction-model" );
	}

	static {
		returnRepresentationApplied = new HTTPHeaderValue();
		returnRepresentationApplied.setMainKey( "return" );
		returnRepresentationApplied.setMainValue( "representation" );
	}

	protected ConfigurationRepository configurationRepository;

	protected RDFSourceService sourceService;
	protected ContainerService containerService;
	protected NonRDFSourceService nonRdfSourceService;
	protected SPARQLService sparqlService;

	protected HttpServletRequest request;
	protected HttpServletResponse response;
	protected List<HTTPHeaderValue> appliedPreferences;

	private Set<InteractionModel> supportedInteractionModels;
	private InteractionModel defaultInteractionModel;

	protected void setUp( HttpServletRequest request, HttpServletResponse response ) {
		this.request = request;
		this.response = response;
		this.appliedPreferences = new ArrayList<>();
	}

	protected boolean targetResourceExists( URI targetURI ) {
		return sourceService.exists( targetURI );
	}

	protected URI getTargetURI( HttpServletRequest request ) {
		String url = RequestUtil.getRequestURL( request );
		return new URIImpl( url );
	}

	protected InteractionModel getInteractionModel( URI targetURI ) {
		InteractionModel requestInteractionModel = getRequestInteractionModel( request );
		if ( requestInteractionModel != null ) {
			checkInteractionModelSupport( requestInteractionModel );
			appliedPreferences.add( interactionModelApplied );
			return requestInteractionModel;
		}
		return getDefaultInteractionModel( targetURI );
	}

	private InteractionModel getRequestInteractionModel( HttpServletRequest request ) {
		HTTPHeader preferHeader = new HTTPHeader( request.getHeaders( HTTPHeaders.PREFER ) );

		// TODO: Move this to a constants file
		List<HTTPHeaderValue> filteredValues = HTTPHeader.filterHeaderValues( preferHeader, null, null, "rel", "interaction-model" );
		int size = filteredValues.size();
		if ( size == 0 ) return null;
		if ( size > 1 ) throw new BadRequestException( 0x5002 );

		String interactionModelURI = filteredValues.get( 0 ).getMainValue();
		InteractionModel interactionModel = RDFNodeUtil.findByIRI( interactionModelURI, InteractionModel.class );
		if ( interactionModel == null ) throw new BadRequestException( 0x5003 );
		return interactionModel;
	}

	private void checkInteractionModelSupport( InteractionModel requestInteractionModel ) {
		if ( ! getSupportedInteractionModels().contains( requestInteractionModel ) ) {
			throw new BadRequestException( 0x5004 );
		}
	}

	private InteractionModel getDefaultInteractionModel( URI targetURI ) {
		URI dimURI = sourceService.getDefaultInteractionModel( targetURI );
		if ( dimURI == null ) return getDefaultInteractionModel();

		InteractionModel sourceDIM = RDFNodeUtil.findByIRI( dimURI, InteractionModel.class );
		if ( sourceDIM == null ) return getDefaultInteractionModel();

		if ( ! getSupportedInteractionModels().contains( sourceDIM ) ) return getDefaultInteractionModel();
		return sourceDIM;
	}

	protected void addReturnRepresentationHeader() {
		appliedPreferences.add( returnRepresentationApplied );
	}

	protected void setAppliedPreferenceHeaders() {
		for ( HTTPHeaderValue appliedPreference : appliedPreferences ) {
			response.addHeader( HTTPHeaders.PREFERENCE_APPLIED, appliedPreference.toString() );
		}
	}

	protected void setWeakETagHeader( DateTime modifiedTime ) {
		response.setHeader( HTTPHeaders.ETAG, HTTPUtil.formatWeakETag( modifiedTime.toString() ) );
	}

	protected void setStrongETagHeader( String eTag ) {
		response.setHeader( HTTPHeaders.ETAG, eTag );
	}

	protected void setLocationHeader( IRIObject IRIObject ) {
		response.setHeader( HTTPHeaders.LOCATION, IRIObject.getIRI().stringValue() );
	}

	protected void addTypeLinkHeader( RDFNodeEnum interactionModel ) {
		Link link = new Link( interactionModel.getIRI().stringValue() );
		link.addRelationshipType( Consts.TYPE );

		response.addHeader( HTTPHeaders.LINK, link.toString() );
	}

	protected void addInteractionModelLinkHeader( RDFNodeEnum interactionModel ) {
		Link link = new Link( interactionModel.getIRI().stringValue() );
		link.addRelationshipType( Consts.INTERACTION_MODEL );

		response.addHeader( HTTPHeaders.LINK, link.toString() );
	}

	protected void addContainerTypeLinkHeader( ContainerDescription.Type containerType ) {

		addTypeLinkHeader( ContainerDescription.Resource.CLASS );
		addTypeLinkHeader( containerType );
		addTypeLinkHeader( RDFSourceDescription.Resource.CLASS );

	}

	protected String getRequestETag() {
		return request.getHeader( HTTPHeaders.IF_MATCH );
	}

	protected void checkPrecondition( URI targetURI, String requestETag ) {
		if ( requestETag == null ) throw new PreconditionRequiredException();
		requestETag = requestETag.trim();
		if ( ! requestETag.startsWith( "\"" ) && ! requestETag.endsWith( "\"" ) ) {
			requestETag = "\"" + requestETag + "\"";
		}
		try {
			Integer.parseInt( requestETag.substring( 1, requestETag.length() - 1 ) );
		} catch ( NumberFormatException e ) {
			throw new PreconditionFailedException( 0x5005 );
		}
		String eTag = sourceService.getETag( targetURI );

		if ( ! eTag.equals( requestETag ) ) throw new PreconditionFailedException( 0x5006 );

	}

	protected void seekForOrphanFragments( AbstractModel requestModel, RDFResource requestDocumentResource ) {
		for ( Resource subject : requestModel.subjects() ) {
			if ( ! ValueUtil.isIRI( subject ) ) continue;
			URI subjectURI = ValueUtil.getIRI( subject );
			if ( ! IRIUtil.hasFragment( subjectURI ) ) continue;
			URI documentURI = new URIImpl( IRIUtil.getDocumentIRI( subjectURI.stringValue() ) );
			if ( ! requestDocumentResource.getIRI().equals( documentURI ) ) {
				throw new BadRequestException( "The request contains orphan fragments." );
			}
		}
	}

	protected Set<InteractionModel> getSupportedInteractionModels() {
		return supportedInteractionModels;
	}

	protected void setSupportedInteractionModels( Set<InteractionModel> supportedInteractionModels ) {
		this.supportedInteractionModels = supportedInteractionModels;
	}

	protected InteractionModel getDefaultInteractionModel() {
		return defaultInteractionModel;
	}

	protected void setDefaultInteractionModel( InteractionModel defaultInteractionModel ) {
		this.defaultInteractionModel = defaultInteractionModel;
	}

	@Autowired
	public void setConfigurationRepository( ConfigurationRepository configurationRepository ) {
		this.configurationRepository = configurationRepository;
	}

	@Autowired
	public void setRDFSourceService( RDFSourceService sourceService ) {
		this.sourceService = sourceService;
	}

	@Autowired
	public void setContainerService( ContainerService containerService ) {
		this.containerService = containerService;
	}

	@Autowired
	public void setNonRDFResourceService( NonRDFSourceService nonRdfSourceService ) {
		this.nonRdfSourceService = nonRdfSourceService;
	}

	@Autowired
	public void setSparqlService( SPARQLService sparqlService ) {
		this.sparqlService = sparqlService;
	}

}
