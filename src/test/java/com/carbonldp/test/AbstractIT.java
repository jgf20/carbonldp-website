package com.carbonldp.test;

import com.carbonldp.Vars;
import com.carbonldp.apps.App;
import com.carbonldp.apps.AppFactory;
import com.carbonldp.apps.AppService;
import com.carbonldp.ldp.containers.BasicContainer;
import com.carbonldp.ldp.containers.Container;
import com.carbonldp.utils.PropertiesUtil;
import org.mockito.Mockito;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.AbstractModel;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.sail.nativerdf.NativeStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Test( groups = "integration-tests" )
@ContextConfiguration( classes = TestConfig.class )
public abstract class AbstractIT extends AbstractTestNGSpringContextTests {

	@Autowired
	protected AppService appService;
	@Autowired
	protected PlatformContextActionTemplate platformContextTemplate;
	@Autowired
	protected ApplicationContextActionTemplate applicationContextTemplate;
	@Autowired
	protected AuthenticationProvider sesameUsernamePasswordAuthenticationProvider;

	protected final String testRepositoryID = "test-blog";
	protected final String testResourceURI = "http://local.carbonldp.com/apps/test-blog/";

	protected final Logger LOG = LoggerFactory.getLogger( this.getClass() );

	private final String propertiesFile = "config.properties";
	private final String platformRepositoryLocationProperty = "repositories.platform.sesame.directory";
	private final String appsRepositoryLocationProperty = "repositories.apps.sesame.directory";
	private final String appsRepositoryFilesLocationProperty = "repositories.apps.files.directory";
	private final String platformRepositoryFilesLocationProperty = "repositories.platform.files.directory";

	private final String testDataLocation = "test-model.trig";
	private final String platformDefaultDataLocation = "platform-default.trig";

	protected Properties properties;

	protected AbstractIT() {
		this.properties = loadProperties( propertiesFile );
		PropertiesUtil.resolveProperties( properties );
		if ( ! Vars.isInitialized() ) Vars.init( properties );
		erasePlatformRepositoryDirectory( properties );
		eraseAppsRepositoryDirectory( properties );
		loadPlatformRepositoryDefaultData( properties );
		createRepositoryFilesDirectory( appsRepositoryFilesLocationProperty );
		createRepositoryFilesDirectory( platformRepositoryFilesLocationProperty );

	}

	private void createRepositoryFilesDirectory( String appsRepositoryFilesLocationProperty ) {
		File dir = new File( properties.getProperty( appsRepositoryFilesLocationProperty ) );
		boolean done = dir.mkdir();

	}

	@BeforeSuite
	public void hello() {
		LOG.debug( "something something" );

	}

	private Properties loadProperties( String fileName ) {
		Properties properties = new Properties();
		InputStream inputTest = getClass().getClassLoader().getResourceAsStream( fileName );
		InputStream input = getClass().getClassLoader().getResourceAsStream( "local-config.properties" );

		if ( input == null ) throw new RuntimeException( "Properties file not found" );
		if ( inputTest == null ) throw new RuntimeException( "Properties file not found" );

		try {
			properties.load( input );
			properties.load( inputTest );
		} catch ( IOException e ) {
			throw new RuntimeException( e );
		} finally {
			try {
				input.close();
			} catch ( IOException e ) {
				throw new RuntimeException( e );
			}
		}

		return properties;
	}

	private void erasePlatformRepositoryDirectory( Properties properties ) {
		String platformRepositoryLocation = properties.getProperty( platformRepositoryLocationProperty );
		try {
			deleteDirectory( platformRepositoryLocation );
		} catch ( IOException e ) {
			throw new RuntimeException( "The platform repository directory coulnd't be deleted.", e );
		}
	}

	private void eraseAppsRepositoryDirectory( Properties properties ) {
		String appsRepositoryLocation = properties.getProperty( appsRepositoryLocationProperty );
		try {
			deleteDirectory( appsRepositoryLocation );
		} catch ( IOException e ) {
			throw new RuntimeException( "The apps repository directory coulnd't be deleted.", e );
		}
	}

	private void loadPlatformRepositoryDefaultData( Properties properties ) {
		Repository platformRepository = getRepository( properties.getProperty( platformRepositoryLocationProperty ) );
		loadDefaultResourcesfile( platformRepository, platformDefaultDataLocation, properties.getProperty( "platform.url" ) );
		shutdownRepository( platformRepository );
	}

	private void deleteDirectory( String directoryLocation ) throws IOException {
		File directory = new File( directoryLocation );
		deleteDirectory( directory );
	}

	private void deleteDirectory( File directory ) throws IOException {
		if ( ! directory.isDirectory() ) {
			return;
		}

		if ( directory.list().length != 0 ) {
			String files[] = directory.list();

			for ( String temp : files ) {
				File fileToDelete = new File( directory, temp );

				if ( fileToDelete.isDirectory() ) deleteDirectory( fileToDelete );
				else deleteFile( fileToDelete );
			}
		}

		directory.delete();
	}

	private void deleteFile( File file ) throws IOException {
		file.delete();
	}

	private Repository getRepository( String repositoryFile ) {
		File repositoryDir = new File( repositoryFile );
		Repository repository = new SailRepository( new NativeStore( repositoryDir ) );
		try {
			repository.initialize();
		} catch ( RepositoryException e ) {
			LOG.debug( e.getMessage() );
			throw new RuntimeException( "The repository in the directory: '" + repositoryFile + "', couldn't be initialized.", e );
		}
		return repository;
	}

	private void shutdownRepository( Repository repository ) {
		try {
			repository.shutDown();
		} catch ( RepositoryException e ) {
			throw new RuntimeException( e );
		}
	}

	private void loadDefaultResourcesfile( Repository repository, String resourcesFile, String baseURI ) {
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream( resourcesFile );

		RepositoryConnection connection;
		try {
			connection = repository.getConnection();
		} catch ( RepositoryException e ) {
			throw new RuntimeException( "A connection couldn't be retrieved.", e );
		}

		try {
			connection.add( inputStream, baseURI, RDFFormat.TRIG );
		} catch ( RDFParseException e ) {
			throw new RuntimeException( "The file couldn't be parsed.", e );
		} catch ( RepositoryException | IOException e ) {
			throw new RuntimeException( "The resources couldn't be loaded.", e );
		} finally {
			try {
				connection.close();
			} catch ( RepositoryException e ) {
				throw new RuntimeException( "The connection couldn't be closed.", e );
			}
		}
	}

	@BeforeClass( alwaysRun = true, dependsOnMethods = "springTestContextPrepareTestInstance" )
	public void setRepository() {

		InputStream inputStream = null;
		ValueFactory factory = ValueFactoryImpl.getInstance();
		RDFParser rdfParser = Rio.createParser( RDFFormat.TRIG );
		AbstractModel model = new LinkedHashModel();
		rdfParser.setRDFHandler( new StatementCollector( model ) );
		inputStream = getClass().getClassLoader().getResourceAsStream( testDataLocation );
		try {
			rdfParser.parse( inputStream, testDataLocation );
		} catch ( Exception e ) {
			throw new RuntimeException( e );
		}
		URI appURI = factory.createURI( testResourceURI );
		Container container = new BasicContainer( model, appURI );
		App app = AppFactory.getInstance().create( container, appURI.stringValue(), testRepositoryID );

		Authentication authentication = Mockito.mock( Authentication.class );

		Mockito.when( authentication.getPrincipal() ).thenReturn( "admin@carbonldp.com" );
		Mockito.when( authentication.getCredentials() ).thenReturn( "hello" );
		Authentication authToken;
		try {
			authToken = sesameUsernamePasswordAuthenticationProvider.authenticate( authentication );
		} catch ( BadCredentialsException e ) {
			throw new RuntimeException( e );
		}
		SecurityContextHolder.getContext().setAuthentication( authToken );

		// This if is needed here, lines above this are neccesary to run every time before each class.
		if ( ! appService.exists( new URIImpl( testResourceURI ) ) )
			appService.create( app );

	}
}
