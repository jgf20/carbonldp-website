package com.carbonldp.ldp;

import com.carbonldp.rdf.RDFBlankNodeRepository;
import com.carbonldp.rdf.RDFDocumentRepository;
import com.carbonldp.rdf.RDFResourceRepository;
import com.carbonldp.repository.AbstractSesameRepository;
import org.eclipse.rdf4j.spring.SesameConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public abstract class AbstractSesameLDPRepository extends AbstractSesameRepository {

	protected RDFResourceRepository resourceRepository;
	protected RDFDocumentRepository documentRepository;
	protected RDFBlankNodeRepository blankNodeRepository;

	public AbstractSesameLDPRepository( SesameConnectionFactory connectionFactory, RDFResourceRepository resourceRepository,
		RDFDocumentRepository documentRepository ) {
		super( connectionFactory );
		this.resourceRepository = resourceRepository;
		this.documentRepository = documentRepository;
	}

	@Autowired
	public void setBlankNodeRepository( RDFBlankNodeRepository blankNodeRepository ) {
		this.blankNodeRepository = blankNodeRepository;
	}

}
