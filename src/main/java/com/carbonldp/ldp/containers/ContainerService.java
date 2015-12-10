package com.carbonldp.ldp.containers;

import com.carbonldp.descriptions.APIPreferences;
import org.joda.time.DateTime;
import org.openrdf.model.URI;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.File;
import java.util.Set;

public interface ContainerService {
	@PreAuthorize( "hasPermission(#containerURI, 'READ')" )
	public Container get( URI containerURI, Set<APIPreferences.ContainerRetrievalPreference> containerRetrievalPreferences );

	@PreAuthorize( "hasPermission(#containerURI, 'READ')" )
	public Set<APIPreferences.ContainerRetrievalPreference> getRetrievalPreferences( URI containerURI );

	@PreAuthorize( "hasPermission(#containerURI, 'READ')" )
	public ContainerDescription.Type getContainerType( URI containerURI );

	@PreAuthorize( "hasPermission(#containerURI, 'CREATE_CHILD')" )
	public DateTime createChild( URI containerURI, BasicContainer basicContainer );

	@PreAuthorize( "hasPermission(#containerURI, 'ADD_MEMBER')" )
	public void addMembers( URI containerURI, Set<URI> members );

	@PreAuthorize( "hasPermission(#containerURI, 'ADD_MEMBER')" )
	public void addMember( URI containerURI, URI member );

	// TODO: Add permission validation
	public void removeMembers( URI targetURI );

	// TODO: Add permision validation
	public void removeMembers( URI targetUri, Set<URI> members );

	// TODO: Add permision validation
	public void removeMember( URI containerURI, URI member );

	// TODO: Add permission validation
	public void deleteContainedResources( URI targetURI );

	// TODO: Add permission validation
	public void delete( URI targetURI );

	//TODO: Add permission validation
	public void createNonRDFResource( URI targetURI, URI resourceURI, File resourceFile, String mimeType );
}