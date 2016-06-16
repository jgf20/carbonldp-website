package com.carbonldp.jobs;

import com.carbonldp.authorization.acl.ACEDescription;
import com.carbonldp.exceptions.InvalidResourceException;
import com.carbonldp.exceptions.ResourceDoesntExistException;
import com.carbonldp.ldp.AbstractSesameLDPService;
import com.carbonldp.ldp.containers.ContainerService;
import com.carbonldp.ldp.sources.RDFSourceService;
import com.carbonldp.models.Infraction;
import com.carbonldp.rdf.RDFResourceRepository;
import com.carbonldp.web.exceptions.ForbiddenException;
import org.openrdf.model.IRI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JorgeEspinosa
 * @since 0.33.0
 */
public class SesameJobService extends AbstractSesameLDPService implements JobService {
	private ContainerService containerService;
	private RDFSourceService sourceService;
	private ExecutionService executionService;
	private RDFResourceRepository resourceRepository;
	protected PermissionEvaluator permissionEvaluator;

	@Override
	public void create( IRI targetIRI, Job job ) {
		validate( job );
		containerService.createChild( targetIRI, job );
	}

	@Override
	public void createExecution( IRI jobIRI, Execution execution ) {
		containerService.createChild( jobIRI, execution );
		IRI executionQueueLocation = resourceRepository.getIRI( jobIRI, JobDescription.Property.EXECUTION_QUEUE_LOCATION );
		executionService.enqueue( execution.getIRI(), executionQueueLocation );
	}

	private void validate( Job job ) {
		List<Infraction> infractions = new ArrayList<>();
		JobDescription.Type jobType = JobFactory.getInstance().getJobType( job );
		if ( jobType == null )
			throw new InvalidResourceException( new Infraction( 0x2001, "rdf.type", "job type" ) );
		switch ( jobType ) {
			case EXPORT_BACKUP_JOB:
				infractions = ExportBackupJobFactory.getInstance().validate( job );
				break;
			case IMPORT_BACKUP_JOB:
				infractions = ImportBackupJobFactory.getInstance().validate( job );
				checkPermissionsOverTheBackup( job );
				break;
			default:
				infractions.add( new Infraction( 0x2001, "rdf.type", "job type" ) );
		}
		if ( ! infractions.isEmpty() ) throw new InvalidResourceException( infractions );
	}

	private void checkPermissionsOverTheBackup( Job job ) {
		ImportBackupJob importBackupJob = new ImportBackupJob( job );
		IRI backupIRI = importBackupJob.getBackup();
		if ( ! sourceService.exists( backupIRI ) ) throw new InvalidResourceException( new Infraction( 0x2011, "iri", backupIRI.stringValue() ) );
		sourceService.get( backupIRI );
	}

	@Override
	public Job get( IRI targetIRI ) {
		return new Job( sourceService.get( targetIRI ) );
	}

	@Autowired
	public void setContainerService( ContainerService containerService ) { this.containerService = containerService; }

	@Autowired
	public void setSourceService( RDFSourceService sourceService ) { this.sourceService = sourceService; }

	@Autowired
	public void setExecutionService( ExecutionService executionService ) {
		this.executionService = executionService;
	}

	@Autowired
	public void setResourceRepository( RDFResourceRepository resourceRepository ) {
		this.resourceRepository = resourceRepository;
	}

	@Autowired
	public void setPermissionEvaluator( PermissionEvaluator permissionEvaluator ) {
		this.permissionEvaluator = permissionEvaluator;
	}
}
