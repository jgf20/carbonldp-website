package com.carbonldp.jobs;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @author NestorVenegas
 * @since 0.33.0
 */
public interface ExecutionService {

	@PreAuthorize( "hasPermission(#executionIRI, 'UPDATE')" )
	public void changeExecutionStatus( IRI executionIRI, ExecutionDescription.Status status );

	@PreAuthorize( "hasPermission(#executionQueueLocationIRI, 'UPDATE')" )
	public void enqueue( IRI executionIRI, IRI executionQueueLocationIRI );

	@PreAuthorize( "hasPermission(#executionQueueLocationIRI, 'UPDATE')" )
	public void dequeue( IRI executionQueueLocationIRI );

	@PreAuthorize( "hasPermission(#executionQueueLocationIRI, 'READ')" )
	public Execution peek( IRI executionQueueLocationIRI );

	@PreAuthorize( "hasPermission(#executionIRI, 'UPDATE')" )
	public void addResult( IRI executionIRI, Value status );

	@PreAuthorize( "hasPermission(#executionIRI, 'UPDATE')" )
	public void addErrorDescription( IRI executionIRI, String error );
}
