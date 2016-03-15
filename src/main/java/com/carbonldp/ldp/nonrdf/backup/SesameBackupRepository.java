package com.carbonldp.ldp.nonrdf.backup;

import com.carbonldp.Consts;
import com.carbonldp.Vars;
import com.carbonldp.ldp.AbstractSesameLDPRepository;
import com.carbonldp.ldp.containers.ContainerDescription;
import com.carbonldp.ldp.containers.ContainerRepository;
import com.carbonldp.ldp.nonrdf.RDFRepresentationRepository;
import com.carbonldp.ldp.sources.RDFSourceDescription;
import com.carbonldp.ldp.sources.RDFSourceRepository;
import com.carbonldp.rdf.RDFDocumentRepository;
import com.carbonldp.rdf.RDFResourceRepository;
import org.joda.time.DateTime;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.spring.SesameConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

/**
 * @author NestorVenegas
 * @since _version_
 */
public class SesameBackupRepository extends AbstractSesameLDPRepository implements BackupRepository {

	private RDFRepresentationRepository rdfRepresentationRepository;
	private ContainerRepository containerRepository;
	private RDFSourceRepository sourceRepository;

	public SesameBackupRepository( SesameConnectionFactory connectionFactory, RDFResourceRepository resourceRepository, RDFDocumentRepository documentRepository ) {
		super( connectionFactory, resourceRepository, documentRepository );
	}

	@Override
	public void createAppBackup( URI appURI, URI backupURI, File zipFile ) {
		DateTime creationTime = DateTime.now();
		URI containerURI = new URIImpl( appURI.stringValue() + Vars.getInstance().getBackupsContainer() );

		Backup backup = BackupFactory.getInstance().create( backupURI );
		backup.add( RDFSourceDescription.Property.DEFAULT_INTERACTION_MODEL.getURI(), RDFSourceDescription.Resource.CLASS.getURI() );
		backup.setTimestamps( creationTime );
		rdfRepresentationRepository.create( backup, zipFile, Consts.ZIP );

		addContainedResource( containerURI, backupURI );

		containerRepository.addMember( containerURI, backupURI );
		sourceRepository.touch( containerURI, creationTime );
	}

	private void addContainedResource( URI containerURI, URI resourceURI ) {
		connectionTemplate.write( ( connection ) -> connection.add( containerURI, ContainerDescription.Property.CONTAINS.getURI(), resourceURI, containerURI ) );
	}

	@Autowired
	public void setRdfRepresentationRepository( RDFRepresentationRepository rdfRepresentationRepository ) {
		this.rdfRepresentationRepository = rdfRepresentationRepository;
	}

	@Autowired
	public void setContainerRepository( ContainerRepository containerRepository ) {
		this.containerRepository = containerRepository;
	}

	@Autowired
	public void setSourceRepository( RDFSourceRepository sourceRepository ) {
		this.sourceRepository = sourceRepository;
	}
}
