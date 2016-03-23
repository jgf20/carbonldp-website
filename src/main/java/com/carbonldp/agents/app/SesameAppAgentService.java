package com.carbonldp.agents.app;

import com.carbonldp.agents.Agent;
import com.carbonldp.agents.AgentValidator;
import com.carbonldp.agents.SesameAgentsService;
import com.carbonldp.apps.roles.AppRoleRepository;
import com.carbonldp.authorization.acl.ACL;
import com.carbonldp.exceptions.ResourceAlreadyExistsException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author NestorVenegas
 * @since 0.14.0-ALPHA
 */

public class SesameAppAgentService extends SesameAgentsService {

	protected AppAgentRepository appAgentRepository;
	protected AppRoleRepository appRoleRepository;

	@Override
	public void register( Agent agent ) {
		validate( agent );

		String email = agent.getEmails().iterator().next();
		if ( appAgentRepository.existsWithEmail( email ) ) throw new ResourceAlreadyExistsException();
		setAgentPasswordFields( agent );

		boolean requireValidation = configurationRepository.requireAgentEmailValidation();
		if ( requireValidation ) agent.setEnabled( false );
		else agent.setEnabled( true );

		appAgentRepository.create( agent );
		aclRepository.createACL( agent.getURI() );

		if ( requireValidation ) {
			AgentValidator validator = createAgentValidator( agent );
			ACL validatorACL = aclRepository.createACL( validator.getURI() );
			addValidatorDefaultPermissions( validatorACL );

			sendValidationEmail( agent, validator );
			// TODO: Create "resend validation" resource
		}

	}

	@Autowired
	public void setAppAgentRepository( AppAgentRepository appAgentRepository ) { this.appAgentRepository = appAgentRepository; }

	@Autowired
	public void setAppRoleRepository( AppRoleRepository appRoleRepository ) { this.appRoleRepository = appRoleRepository; }
}