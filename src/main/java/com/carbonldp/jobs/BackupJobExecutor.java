package com.carbonldp.jobs;

import com.carbonldp.Consts;
import com.carbonldp.Vars;
import com.carbonldp.apps.App;
import com.carbonldp.apps.AppRepository;
import com.carbonldp.ldp.nonrdf.backup.BackupService;
import com.carbonldp.spring.TransactionWrapper;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.trig.TriGWriter;
import org.openrdf.spring.SesameConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;

import java.io.*;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author NestorVenegas
 * @since _version_
 */
public class BackupJobExecutor implements TypedJobExecutor {
	protected final Logger LOG = LoggerFactory.getLogger( this.getClass() );
	private AppRepository appRepository;
	private SesameConnectionFactory connectionFactory;
	private BackupService backupService;
	private TransactionWrapper transactionWrapper;
	private ExecutionRepository executionRepository;

	public boolean supports( JobDescription.Type jobType ) {
		return jobType == JobDescription.Type.BACKUP;
	}

	public void execute( Job job, Execution execution ) {
		transactionWrapper.runInPlatformContext( () -> executionRepository.changeExecutionStatus( execution.getURI(), ExecutionDescription.Status.RUNNING ) );
		URI appURI = job.getAppRelated();
		App app = appRepository.get( appURI );
		String appRepositoryID = app.getRepositoryID();
		String appRepositoryPath = Vars.getInstance().getAppsFilesDirectory().concat( Consts.SLASH ).concat( appRepositoryID );
		File nonRDFSourceDirectory = new File( appRepositoryPath );
		File rdfRepositoryFile = transactionWrapper.runInAppContext( app, () -> createTemporaryRDFBackupFile() );
		File zipFile = nonRDFSourceDirectory.exists() ?
			transactionWrapper.runWithSystemPermissionsInPlatformContext( () -> createZipFile( nonRDFSourceDirectory, rdfRepositoryFile ) ) :
			transactionWrapper.runWithSystemPermissionsInPlatformContext( () -> createZipFile( rdfRepositoryFile ) );

		backupService.createAppBackup( appURI, zipFile );

		deleteTemporaryFile( zipFile );
		deleteTemporaryFile( rdfRepositoryFile );

		transactionWrapper.runInPlatformContext( () -> executionRepository.changeExecutionStatus( execution.getURI(), ExecutionDescription.Status.FINISHED ) );
	}

	private File createZipFile( File... files ) {
		File temporaryFile;
		FileOutputStream fileOutputStream;
		try {
			temporaryFile = File.createTempFile( createRandomSlug(), RDFFormat.TRIG.getDefaultFileExtension() );
			temporaryFile.deleteOnExit();
			fileOutputStream = new FileOutputStream( temporaryFile );
		} catch ( FileNotFoundException e ) {
			throw new RuntimeException( "there's no such file", e );
		} catch ( IOException e ) {
			throw new RuntimeException( e );
		}
		ZipOutputStream zipOutputStream = new ZipOutputStream( fileOutputStream );

		for ( File file : files ) {
			if ( file.isDirectory() ) {
				File[] listFiles = file.listFiles();
				for ( File listFile : listFiles ) {
					addFileTozip( zipOutputStream, listFile, file );
				}
			} else {
				addFileTozip( zipOutputStream, file, null );
			}
		}
		try {
			zipOutputStream.close();
			fileOutputStream.close();
		} catch ( IOException e ) {
			LOG.warn( "zip stream could no be closed" );
		}
		return temporaryFile;
	}

	private void addFileTozip( ZipOutputStream zipOutputStream, File file, File directoryFile ) {
		FileSystemResource resource = new FileSystemResource( file.getPath() );
		FileInputStream fileInputStream;
		try {
			fileInputStream = (FileInputStream) resource.getInputStream();
		} catch ( IOException e ) {
			throw new RuntimeException( "there's no such file", e );
		}
		ZipEntry zipEntry;
		if ( directoryFile != null ) {
			zipEntry = new ZipEntry( directoryFile.getName().concat( Consts.SLASH ).concat( file.getName() ) );
		} else {
			zipEntry = new ZipEntry( file.getName() );
		}
		try {
			zipOutputStream.putNextEntry( zipEntry );
		} catch ( IOException e ) {
			throw new RuntimeException( "unable to add file ", e );
		}

		byte[] bytes = new byte[1024];
		int length;
		try {
			while ( ( length = fileInputStream.read( bytes ) ) >= 0 ) {
				zipOutputStream.write( bytes, 0, length );
			}
		} catch ( IOException e ) {
			throw new RuntimeException( "unable to add file ", e );
		}
		try {
			zipOutputStream.closeEntry();
			fileInputStream.close();
		} catch ( IOException e ) {
			LOG.warn( "stream could no be closed" );
		}
	}

	protected File createTemporaryRDFBackupFile() {
		File temporaryFile;
		FileOutputStream outputStream;
		final RDFWriter trigWriter;
		try {
			temporaryFile = File.createTempFile( createRandomSlug(), RDFFormat.TRIG.getDefaultFileExtension() );
			temporaryFile.deleteOnExit();

			outputStream = new FileOutputStream( temporaryFile );
			trigWriter = new TriGWriter( outputStream );
			try {
				connectionFactory.getConnection().export( trigWriter );
			} catch ( RepositoryException e ) {
				throw new RuntimeException( e );
			} catch ( RDFHandlerException e ) {
				throw new RuntimeException( e );
			} finally {
				try {
					outputStream.close();
				} catch ( IOException e ) {
					LOG.warn( "The outputStream couldn't be closed. Exception: ", e );
				}
			}
		} catch ( IOException | SecurityException e ) {
			throw new RuntimeException( "The temporary file couldn't be created. Exception:", e );
		}
		return temporaryFile;
	}

	protected void deleteTemporaryFile( File file ) {
		boolean wasDeleted = false;
		try {
			wasDeleted = file.delete();
		} catch ( SecurityException e ) {
			LOG.warn( "A temporary file couldn't be deleted. Exception:", e );
		}
		if ( ! wasDeleted ) LOG.warn( "The temporary file: '{}', couldn't be deleted.", file.toString() );
	}

	protected String createRandomSlug() {
		Random random = new Random();
		return String.valueOf( Math.abs( random.nextLong() ) );
	}

	@Autowired
	public void setAppRepository( AppRepository appRepository ) {this.appRepository = appRepository; }

	@Autowired
	public void setConnectionFactory( SesameConnectionFactory connectionFactory ) {
		this.connectionFactory = connectionFactory;
	}

	@Autowired
	public void setBackupService( BackupService backupService ) {
		this.backupService = backupService;
	}

	@Autowired
	public void setTransactionWrapper( TransactionWrapper transactionWrapper ) {this.transactionWrapper = transactionWrapper; }

	@Autowired
	public void setExecutionRepository( ExecutionRepository executionRepository ) { this.executionRepository = executionRepository; }
}
