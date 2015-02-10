package com.carbonldp.repository;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.spring.SesameConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.carbonldp.ConfigurationRepository;
import com.carbonldp.agents.AgentService;
import com.carbonldp.agents.SesameAgentService;
import com.carbonldp.apps.AppService;
import com.carbonldp.apps.SesameAppService;
import com.carbonldp.authorization.PlatformRoleService;
import com.carbonldp.authorization.SesamePlatformRoleService;
import com.carbonldp.ldp.services.ContainerService;
import com.carbonldp.ldp.services.RDFSourceService;
import com.carbonldp.ldp.services.SesameBasicContainerService;
import com.carbonldp.ldp.services.SesameContainerService;
import com.carbonldp.ldp.services.SesameDirectContainerService;
import com.carbonldp.ldp.services.SesameIndirectContainerService;
import com.carbonldp.ldp.services.SesameRDFSourceService;
import com.carbonldp.ldp.services.TypedContainerService;
import com.carbonldp.repository.txn.TxnConfig;

@Configuration
//@formatter:off
@Import(
	value = {
		TxnConfig.class
	}
)
//@formatter:on
public class RepositoryConfig {
	@Autowired
	private ConfigurationRepository configurationRepository;
	@Autowired
	private SesameConnectionFactory connectionFactory;

	@Bean
	protected RDFResourceRepository rdfResourceRepository() {
		return new SesameRDFResourceRepository(connectionFactory);
	}

	@Bean
	protected RDFDocumentRepository rdfDocumentRepository() {
		return new SesameRDFDocumentRepository(connectionFactory);
	}

	@Bean
	public RDFSourceService rdfSourceService() {
		return new SesameRDFSourceService(connectionFactory, rdfResourceRepository(), rdfDocumentRepository());
	}

	@Bean
	public ContainerService containerService() {
		List<TypedContainerService> typedServices = new ArrayList<TypedContainerService>();
		typedServices.add(new SesameBasicContainerService(connectionFactory, rdfResourceRepository(), rdfDocumentRepository()));
		typedServices.add(new SesameDirectContainerService(connectionFactory, rdfResourceRepository(), rdfDocumentRepository()));
		typedServices.add(new SesameIndirectContainerService(connectionFactory, rdfResourceRepository(), rdfDocumentRepository()));

		return new SesameContainerService(connectionFactory, rdfResourceRepository(), rdfDocumentRepository(), typedServices);
	}

	@Bean
	public AgentService agentService() {
		URI agentsContainerURI = new URIImpl(configurationRepository.getPlatformAppsContainerURL());
		return new SesameAgentService(connectionFactory, rdfSourceService(), containerService(), agentsContainerURI);
	}

	@Bean
	public PlatformRoleService platformRoleService() {
		URI agentsContainerURI = new URIImpl(configurationRepository.getPlatformRolesContainerURL());
		return new SesamePlatformRoleService(connectionFactory, rdfSourceService(), containerService(), agentsContainerURI);
	}

	@Bean
	public AppService appService() {
		return new SesameAppService(connectionFactory, rdfSourceService(), containerService());
	}
}