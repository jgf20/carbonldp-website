package com.carbonldp.ldp.containers;

import com.carbonldp.rdf.RDFResource;
import com.carbonldp.web.RequestHandler;
import org.openrdf.model.URI;

import java.util.Set;

/**
 * @author MiguelAraCo
 * @since 0.10.0-ALPHA
 */
@RequestHandler( "container:basePUTRequestHandler" )
public class BasePUTRequestHandler extends AbstractPUTRequestHandler<RDFResource> {

	@Override
	protected void addMembers( URI targetUri, Set<URI> members ) {
		containerService.addMembers( targetUri, members );
	}
}
