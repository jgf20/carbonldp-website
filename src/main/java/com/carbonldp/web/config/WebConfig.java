package com.carbonldp.web.config;

import com.carbonldp.config.ConfigurationRepository;
import com.carbonldp.ldp.nonrdf.NonRDFSourceMessageConverter;
import com.carbonldp.ldp.web.RDFSourceMessageConverter;
import com.carbonldp.log.ControllerCallsLogger;
import com.carbonldp.rdf.RDFResourceMessageConverter;
import com.carbonldp.sparql.SPARQLBooleanMessageConverter;
import com.carbonldp.sparql.SPARQLGraphMessageConverter;
import com.carbonldp.sparql.SPARQLTupleMessageConverter;
import com.carbonldp.web.*;
import com.carbonldp.web.converters.AbstractModelMessageConverter;
import com.carbonldp.web.converters.EmptyResponseMessageConverter;
import com.carbonldp.rdf.RDFDocumentMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

@Configuration
// This import replaces the need of using @EnableWebMvc
@Import( value = CustomWebMVCConfigurationSupport.class )
@EnableTransactionManagement
@EnableAspectJAutoProxy
@ComponentScan(
	useDefaultFilters = false,
	includeFilters = {
		@ComponentScan.Filter( type = FilterType.ANNOTATION, value = ControllerAdvice.class ),
		@ComponentScan.Filter( type = FilterType.ANNOTATION, value = Controller.class ),
		@ComponentScan.Filter( type = FilterType.ANNOTATION, value = RequestHandler.class )
	},
	basePackages = {"com.carbonldp"}
)
public class WebConfig extends WebMvcConfigurerAdapter {

	@Autowired
	private ConfigurationRepository configurationRepository;

	@Override
	public void configureMessageConverters( List<HttpMessageConverter<?>> converters ) {
		converters.add( sparqlTupleMessageConverter() );
		converters.add( nonRDFSourceMessageConverter() );
		converters.add( rdfSourceMessageConverter() );
		converters.add( rdfResourceMessageConverter() );
		converters.add( rdfDocumentMessageConverter() );
		converters.add( modelMessageConverter() );
		converters.add( emptyResponseMessageConverter() );
		converters.add( sparqlBooleanMessageConverter() );
		converters.add( sparqlGraphMessageConverter() );
		converters.add( new ByteArrayHttpMessageConverter() );
		converters.add( new StringHttpMessageConverter() );
		converters.add( new FormHttpMessageConverter() );
	}

	@Override
	public void configureContentNegotiation( ContentNegotiationConfigurer configurer ) {
		super.configureContentNegotiation( configurer );
		configurer.favorPathExtension( false );
	}

	@Bean
	public MultipartResolver multipartResolver() {
		return new StandardServletMultipartResolver();
	}

	@Bean
	public RDFDocumentMessageConverter rdfDocumentMessageConverter() {
		return new RDFDocumentMessageConverter( configurationRepository );
	}

	@Bean
	public AbstractModelMessageConverter modelMessageConverter() {
		return new AbstractModelMessageConverter( configurationRepository );
	}

	@Bean
	public RDFResourceMessageConverter rdfResourceMessageConverter() {
		return new RDFResourceMessageConverter();
	}

	@Bean
	public RDFSourceMessageConverter rdfSourceMessageConverter() {
		return new RDFSourceMessageConverter();
	}

	@Bean
	public NonRDFSourceMessageConverter nonRDFSourceMessageConverter() {
		return new NonRDFSourceMessageConverter();
	}

	@Bean
	public EmptyResponseMessageConverter emptyResponseMessageConverter() {
		return new EmptyResponseMessageConverter();
	}

	@Bean
	public ControllerCallsLogger controllerCallsLogger() {
		return new ControllerCallsLogger();
	}

	@Bean
	public SPARQLTupleMessageConverter sparqlTupleMessageConverter() {
		return new SPARQLTupleMessageConverter();
	}

	@Bean
	public SPARQLGraphMessageConverter sparqlGraphMessageConverter() {
		return new SPARQLGraphMessageConverter();
	}

	@Bean
	public SPARQLBooleanMessageConverter sparqlBooleanMessageConverter() {
		return new SPARQLBooleanMessageConverter();
	}
}