package com.carbonldp.config;

import com.carbonldp.agents.platform.PlatformAgentRepository;
import com.carbonldp.agents.platform.PlatformAgentService;
import com.carbonldp.agents.platform.SesamePlatformAgentService;
import com.carbonldp.apps.AppRepository;
import com.carbonldp.apps.AppService;
import com.carbonldp.apps.SesameAppService;
import com.carbonldp.apps.roles.AppRoleRepository;
import com.carbonldp.authorization.acl.ACLRepository;
import com.carbonldp.ldp.containers.ContainerRepository;
import com.carbonldp.ldp.containers.ContainerService;
import com.carbonldp.ldp.containers.SesameContainerService;
import com.carbonldp.ldp.sources.RDFSourceRepository;
import com.carbonldp.ldp.sources.RDFSourceService;
import com.carbonldp.ldp.sources.SesameRDFSourceService;
import com.carbonldp.spring.TransactionWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServicesConfig {

	@Autowired
	private RDFSourceRepository sourceRepository;
	@Autowired
	private ContainerRepository containerRepository;

	@Autowired
	private ACLRepository aclRepository;

	@Bean
	protected TransactionWrapper transactionWrapper() {
		return new TransactionWrapper();
	}

	@Bean
	public RDFSourceService sourceService() {
		return new SesameRDFSourceService( transactionWrapper(), sourceRepository, containerRepository, aclRepository );
	}

	@Bean
	public ContainerService containerService() {
		return new SesameContainerService( transactionWrapper(), sourceRepository, containerRepository, aclRepository );
	}

	@Bean
	public AppService appService( AppRepository appRepository, AppRoleRepository appRoleRepository ) {
		return new SesameAppService( transactionWrapper(), sourceRepository, containerRepository, aclRepository, appRepository, appRoleRepository );
	}

	@Bean
	public PlatformAgentService platformAgentService( PlatformAgentRepository platformAgentRepository ) {
		return new SesamePlatformAgentService( transactionWrapper(), sourceRepository, containerRepository, aclRepository, platformAgentRepository );
	}
}
