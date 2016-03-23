package com.carbonldp.rdf;

import org.openrdf.model.URI;

import java.util.Collection;
import java.util.Set;

public interface RDFDocumentRepository {
	public boolean documentExists( URI documentURI );

	public RDFDocument getDocument( URI documentURI );

	public Set<RDFDocument> getDocuments( Collection<? extends URI> documentURIs );

	public void addDocument( RDFDocument document );

	public void addDocuments( Collection<RDFDocument> documents );

	public void update( RDFDocument document );

	public void deleteDocument( URI documentURI );

	public void deleteDocuments( Collection<URI> documentURIs );

	public void add( URI sourceURI, RDFDocument document );

	public void subtract( URI sourceURI, RDFDocument document );

	public void set( URI sourceURI, RDFDocument document );

}