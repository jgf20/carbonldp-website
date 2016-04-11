package com.carbonldp.test.apps.context;

import com.carbonldp.apps.App;
import com.carbonldp.apps.AppRepository;
import com.carbonldp.apps.context.AppContext;
import com.carbonldp.apps.context.AppContextHolder;
import com.carbonldp.apps.context.AppContextPersistenceFilter;
import com.carbonldp.test.AbstractIT;
import com.carbonldp.test.ActionCallback;
import org.mockito.Mockito;
import org.openrdf.model.impl.URIImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class AppContextIT extends AbstractIT {

	@Autowired
	private AppContextPersistenceFilter appContextPersistenceFilter;

	@Autowired
	private AppRepository appRepository;

	static final String FILTER_APPLIED = "__carbon_acpf_applied";

	AppContext context = AppContextHolder.createEmptyContext();

	@Test
	public void avoidFilterSecondTimeTest() {
		HttpServletRequest request = Mockito.mock( HttpServletRequest.class );
		HttpServletResponse response = Mockito.mock( HttpServletResponse.class );
		FilterChain chain = Mockito.mock( FilterChain.class );

		Mockito.when( request.getAttribute( FILTER_APPLIED ) ).thenReturn( true );
		try {
			appContextPersistenceFilter.doFilter( request, response, chain );
		} catch ( IOException | ServletException e ) {
			throw new RuntimeException( e );
		}
		Mockito.verify( request, Mockito.never() ).getRequestURI();
		context.setApplication( null );
	}

	@Test
	public void wrongRequestURITest() {
		HttpServletRequest request = Mockito.mock( HttpServletRequest.class );
		HttpServletResponse response = Mockito.mock( HttpServletResponse.class );
		FilterChain chain = Mockito.mock( FilterChain.class );

		Mockito.when( request.getAttribute( FILTER_APPLIED ) ).thenReturn( null );
		Mockito.when( request.getRequestURI() ).thenReturn( "something" );
		try {
			appContextPersistenceFilter.doFilter( request, response, chain );
		} catch ( IOException | ServletException e ) {
			throw new RuntimeException( e );
		}
		Mockito.verify( response ).setStatus( 404 );
		context.setApplication( null );
	}

	@Test
	public void appNotFoundTest() {
		HttpServletRequest request = Mockito.mock( HttpServletRequest.class );
		HttpServletResponse response = Mockito.mock( HttpServletResponse.class );
		FilterChain chain = Mockito.mock( FilterChain.class );

		Mockito.when( request.getAttribute( FILTER_APPLIED ) ).thenReturn( null );
		Mockito.when( request.getRequestURI() ).thenReturn( "apps/some-blog/" );
		try {
			appContextPersistenceFilter.doFilter( request, response, chain );
		} catch ( IOException | ServletException e ) {
			throw new RuntimeException( e );
		}
		Mockito.verify( response ).setStatus( 404 );
		context.setApplication( null );
	}

	@Test
	public void plattformToAppContextExchangerTest() {
		App app = appRepository.findByRootContainer( new URIImpl( testResourceURI ) );
		context.setApplication( null );
		assertTrue( context.isEmpty() );
		applicationContextTemplate.runInAppContext( app, new ActionCallback() {
			@Override
			public void run() {
				assertEquals( AppContextHolder.getContext().getApplication().getIRI().stringValue(), testResourceURI );

			}

		} );
		context.setApplication( null );
	}

	@Test
	public void appToPlatformContextExchangerTest() {
		App app = appRepository.findByRootContainer( new URIImpl( testResourceURI ) );
		context.setApplication( app );
		AppContextHolder.setContext( context );
		app = AppContextHolder.getContext().getApplication();
		assertEquals( app.getIRI().stringValue(), testResourceURI );

		platformContextTemplate.runInPlatformContext( new ActionCallback() {
			@Override
			public void run() {
				assertTrue( AppContextHolder.getContext().isEmpty() );
			}
		} );
		context.setApplication( null );
	}

	@Test
	public void successfullAppContextEnableTest() {
		HttpServletRequest request = Mockito.mock( HttpServletRequest.class );
		HttpServletResponse response = Mockito.mock( HttpServletResponse.class );
		FilterChain chain = new ChainMock( testResourceURI );

		Mockito.when( request.getAttribute( FILTER_APPLIED ) ).thenReturn( null );
		Mockito.when( request.getRequestURI() ).thenReturn( "apps/test-blog/" );
		try {
			appContextPersistenceFilter.doFilter( request, response, chain );
		} catch ( IOException | ServletException e ) {
			throw new RuntimeException( e );
		}
		Mockito.verify( request ).removeAttribute( FILTER_APPLIED );
		context.setApplication( null );
	}

}

class ChainMock implements FilterChain {
	String testResourceURI;

	public ChainMock( String testResourceURI ) {
		this.testResourceURI = testResourceURI;
	}

	@Override
	public void doFilter( ServletRequest request, ServletResponse response ) throws IOException, ServletException {
		// TODO Auto-generated method stub
		App app = AppContextHolder.getContext().getApplication();
		assertEquals( app.getIRI().stringValue(), testResourceURI );
	}
}
