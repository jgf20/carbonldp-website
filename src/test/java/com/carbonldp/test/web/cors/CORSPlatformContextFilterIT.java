package com.carbonldp.test.web.cors;

import com.carbonldp.apps.AppRepository;
import com.carbonldp.apps.context.AppContext;
import com.carbonldp.apps.context.AppContextHolder;
import com.carbonldp.test.AbstractIT;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.testng.annotations.Test;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CORSPlatformContextFilterIT extends AbstractIT {
	@Autowired
	@Qualifier( "corsPlatformContextFilter" )
	private Filter corsPlatformContextFilter;

	@Autowired
	private AppRepository appRepository;

	AppContext context = AppContextHolder.createEmptyContext();

	@Test
	public void filterTest() {
		HttpServletRequest request = Mockito.mock( HttpServletRequest.class );
		HttpServletResponse response = Mockito.mock( HttpServletResponse.class );
		FilterChain chain = Mockito.mock( FilterChain.class );
		Mockito.when( request.getHeader( "Origin" ) ).thenReturn( "http://www.test.com/" );
		Mockito.when( request.getHeader( "Access-Control-Request-Method" ) ).thenReturn( "OPTIONS" );

		applicationContextTemplate.runInPlatformContext( () -> {
			try {
				corsPlatformContextFilter.doFilter( request, response, chain );
			} catch ( Exception e ) {
				throw new RuntimeException( "Filter not executed correctly", e );
			}
		} );
		Mockito.verify( response ).addHeader( "Access-Control-Allow-Origin", "http://www.test.com/" );
		Mockito.verify( response ).addHeader( "Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS" );
	}
}
