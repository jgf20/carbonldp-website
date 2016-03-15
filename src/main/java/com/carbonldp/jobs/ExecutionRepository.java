package com.carbonldp.jobs;

import org.openrdf.model.URI;
import org.openrdf.model.Value;

/**
 * @author NestorVenegas
 * @since _version_
 */
public interface ExecutionRepository {
	public ExecutionDescription.Status getExecutionStatus( URI executionURI );

	public void changeExecutionStatus( URI executionURI, ExecutionDescription.Status status );

	public void enqueue( URI executionURI, URI appURI );

	public URI getAppRelatedURI( URI executionsContainerURI );

	public void addResult(URI executionURI, Value status);

	public void addErrorDescription(URI executionURI, String error);
}
