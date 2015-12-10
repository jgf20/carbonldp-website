package com.carbonldp.authorization;

import com.carbonldp.apps.context.AppContextPersistenceFilter;
import com.carbonldp.authentication.token.JWTAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;

@Configuration
@EnableWebSecurity
@Order( Ordered.LOWEST_PRECEDENCE )
public class AuthorizationConfig extends AbstractWebSecurityConfigurerAdapter {

	private interface EntryPointOrder {
		int APPS = 1;
		int PLATFORM = 2;
	}

	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

	@Override
	protected void configure( HttpSecurity http ) throws Exception {
		super.configure( http );
		//@formatter:off
		http
			.antMatcher( "/**" )
				.authorizeRequests()
					.anyRequest()
						.permitAll()
		;
		//@formatter:on
	}

	@Configuration
	@Order( EntryPointOrder.APPS )
	public static class AppsEntryPointConfig extends AbstractWebSecurityConfigurerAdapter {
		@Override
		protected void configure( HttpSecurity http ) throws Exception {
			super.configure( http );
			//@formatter:off
			http
				.antMatcher( "/apps/?*/**" )
					.addFilterBefore( appContextPersistenceFilter, SecurityContextPersistenceFilter.class )
					.addFilterAfter( corsAppContextFilter, AppContextPersistenceFilter.class )
					.addFilterAfter( appRolePersistenceFilter, JWTAuthenticationFilter.class )
					.authorizeRequests()
						.anyRequest()
							.permitAll()
			;
			//@formatter:on
		}
	}

	@Configuration
	@Order( EntryPointOrder.PLATFORM )
	public static class PlatformEntryPointConfig extends AbstractWebSecurityConfigurerAdapter {
		@Override
		protected void configure( HttpSecurity http ) throws Exception {
			super.configure( http );
			//@formatter:off
			http
				.antMatcher( "/platform/**" )
					.addFilterBefore( corsPlatformContextFilter, SecurityContextPersistenceFilter.class )
					.authorizeRequests()
						.antMatchers( "/platform/apps/" )
							.permitAll()
						.antMatchers( "/platform/apps/?*/" )
							.permitAll()
						.antMatchers( "/platform/roles/" )
							.permitAll()
						.antMatchers( "/platform/roles/?*/" )
							.permitAll()
						.antMatchers( "/platform/permissions/" )
							.permitAll()
						.antMatchers( "/platform/permissions/?*/" )
							.permitAll()
			;
			//@formatter:on
		}
	}
}