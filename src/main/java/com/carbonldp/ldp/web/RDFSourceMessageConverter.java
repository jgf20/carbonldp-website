package com.carbonldp.ldp.web;

import com.carbonldp.Consts;
import com.carbonldp.HTTPHeaders;
import com.carbonldp.http.Link;
import com.carbonldp.ldp.nonrdf.RDFRepresentationDescription;
import com.carbonldp.ldp.sources.RDFSource;
import com.carbonldp.rdf.RDFResourceDescription;
import com.carbonldp.web.converters.ModelMessageConverter;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.springframework.http.HttpHeaders;

import java.util.Set;

public class RDFSourceMessageConverter extends ModelMessageConverter<RDFSource> {

	public RDFSourceMessageConverter() {
		super( false, true );
	}

	@Override
	protected boolean supports( Class<?> clazz ) {
		return RDFSource.class.isAssignableFrom( clazz );
	}

	@Override
	protected Model getModelToWrite( RDFSource model ) {
		return model.getBaseModel();
	}

	@Override
	protected void setAdditionalHeaders( RDFSource model, HttpHeaders headers ) {
		addRDFResourceLinkHeader( headers );
		setETagHeader( model.getBaseModel(), headers );
		if ( isRDFRepresentation( model ) ) addDescribedByHeader( model, headers );
	}

	private boolean isRDFRepresentation( RDFSource model ) {
		Model types = model.filter( null, RDFResourceDescription.Property.TYPE.getIRI(), null );
		Set<Value> setTypes = types.objects();
		Value rdfRepresentationType = RDFRepresentationDescription.Resource.CLASS.getIRI();
		return setTypes.contains( rdfRepresentationType );
	}

	private void addDescribedByHeader( RDFSource model, HttpHeaders headers ) {
		Link link = new Link( model.getIRI().stringValue() );
		link.addRelationshipType( Consts.DESCRIBED_BY );
		link.setAnchor( model.getIRI().stringValue() );

		headers.add( HTTPHeaders.LINK, link.toString() );
	}

	private void addRDFResourceLinkHeader( HttpHeaders headers ) {
		Link link = new Link( RDFResourceDescription.Resource.CLASS.getIRI().stringValue() );
		link.addRelationshipType( Consts.TYPE );

		headers.add( HTTPHeaders.LINK, link.toString() );
	}
}
