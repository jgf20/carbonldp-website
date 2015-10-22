package com.carbonldp.ldp.containers;

import com.carbonldp.authorization.acl.ACLRepository;
import com.carbonldp.descriptions.APIPreferences;
import com.carbonldp.exceptions.InvalidResourceException;
import com.carbonldp.exceptions.ResourceAlreadyExistsException;
import com.carbonldp.exceptions.ResourceDoesntExistException;
import com.carbonldp.ldp.AbstractSesameLDPService;
import com.carbonldp.ldp.sources.RDFSourceRepository;
import com.carbonldp.models.Infraction;
import com.carbonldp.namespaces.C;
import com.carbonldp.rdf.RDFResource;
import com.carbonldp.spring.TransactionWrapper;
import org.joda.time.DateTime;
import org.openrdf.model.URI;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;
import java.util.Set;

@Transactional
public class SesameContainerService extends AbstractSesameLDPService implements ContainerService {

	public SesameContainerService( TransactionWrapper transactionWrapper, RDFSourceRepository sourceRepository, ContainerRepository containerRepository, ACLRepository aclRepository ) {
		super( transactionWrapper, sourceRepository, containerRepository, aclRepository );
	}

	@Override
	public Container get( URI containerURI, Set<APIPreferences.ContainerRetrievalPreference> containerRetrievalPreferences ) {
		return containerRepository.get( containerURI, containerRetrievalPreferences );
	}

	@Override
	public Set<APIPreferences.ContainerRetrievalPreference> getRetrievalPreferences( URI containerURI ) {
		return containerRepository.getRetrievalPreferences( containerURI );
	}

	@Override
	public ContainerDescription.Type getContainerType( URI containerURI ) {
		return containerRepository.getContainerType( containerURI );
	}

	@Override
	public DateTime createChild( URI containerURI, BasicContainer basicContainer ) {
		DateTime creationTime = DateTime.now();
		URI membershipResource = containerRepository.getTypedRepository( this.getContainerType( containerURI ) ).getMembershipResource( containerURI );
		basicContainer.setTimestamps( creationTime );
		validate( basicContainer );

		containerRepository.createChild( containerURI, basicContainer );
		aclRepository.createACL( basicContainer.getDocument() );

		sourceRepository.touch( containerURI, creationTime );

		if ( ! membershipResource.equals( containerURI ) ) {
			sourceRepository.touch( membershipResource, creationTime );
		}

		return creationTime;
	}

	protected void validate( RDFResource toValidate ) {
		if ( ! BasicContainerFactory.getInstance().isValid( toValidate ) ) throw new IllegalArgumentException( "invalid resource" );
	}

	protected void validate( MembersAction membersAction ) {
		List<Infraction> infractions = MembersActionFactory.getInstance().validate( membersAction );
		if ( ! infractions.isEmpty() ) throw new InvalidResourceException( infractions );
	}

	@Override
	public void createNonRDFResource( URI targetURI, URI resourceURI, File resourceFile, String mediaType ) {
		if ( ! sourceRepository.exists( targetURI ) ) throw new ResourceDoesntExistException();
		if ( sourceRepository.exists( resourceURI ) ) throw new ResourceAlreadyExistsException();

		containerRepository.createNonRDFResource( targetURI, resourceURI, resourceFile, mediaType );
	}

	private void isAddMembersAction( AddMembersAction toValidate ) {
		if ( ! AddMembersActionFactory.getInstance().is( toValidate ) ) throw new InvalidResourceException( new Infraction( 0x2001, "rdf.type", C.Classes.ADD_MEMBER ) );
	}

	private void isRemoveMemberAction( RemoveMembersAction toValidate ) {
		if ( ! RemoveMembersActionFactory.getInstance().is( toValidate ) ) throw new InvalidResourceException( new Infraction( 0x2001, "rdf.type", C.Classes.REMOVE_MEMBER ) );
	}

	@Override
	public void addMembers( URI containerURI, AddMembersAction members ) {
		isAddMembersAction( members );
		validate( members );
		DateTime modifiedTime = DateTime.now();
		URI membershipResource = containerRepository.getTypedRepository( this.getContainerType( containerURI ) ).getMembershipResource( containerURI );
		for ( URI member : members.getMembers() ) {
			addMember( containerURI, member );
		}

		sourceRepository.touch( membershipResource, modifiedTime );
	}

	@Override
	public void addMember( URI containerURI, URI member ) {
		if ( ! sourceRepository.exists( containerURI ) ) throw new ResourceDoesntExistException();
		containerRepository.addMember( containerURI, member );

	}

	@Override
	public void removeMembers( URI containerURI, RemoveMembersAction members ) {
		validate( members );
		isRemoveMemberAction( members );
		DateTime modifiedTime = DateTime.now();
		URI membershipResource = containerRepository.getTypedRepository( this.getContainerType( containerURI ) ).getMembershipResource( containerURI );
		for ( URI member : members.getMembers() ) {
			removeMember( containerURI, member );
		}
		sourceRepository.touch( membershipResource, modifiedTime );

	}

	@Override
	public void removeMember( URI containerURI, URI member ) {
		containerRepository.removeMember( containerURI, member );
	}

	@Override
	public void removeMembers( URI targetURI ) {
		// TODO: Should the resource be touched here?
		containerRepository.removeMembers( targetURI );
	}

	@Override
	public void deleteContainedResources( URI targetURI ) {
		containerRepository.deleteContainedResources( targetURI );
	}

	@Override
	public void delete( URI targetURI ) {
		sourceRepository.delete( targetURI );
	}
}
