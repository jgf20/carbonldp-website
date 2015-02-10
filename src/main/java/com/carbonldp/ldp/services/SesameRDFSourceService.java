package com.carbonldp.ldp.services;

import static com.carbonldp.commons.Consts.NEW_LINE;
import static com.carbonldp.commons.Consts.TAB;

import java.util.HashSet;
import java.util.Set;

import org.joda.time.DateTime;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.AbstractModel;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.spring.SesameConnectionFactory;
import org.springframework.transaction.annotation.Transactional;

import com.carbonldp.commons.models.Container;
import com.carbonldp.commons.models.RDFSource;
import com.carbonldp.exceptions.StupidityException;
import com.carbonldp.repository.RDFDocumentRepository;
import com.carbonldp.repository.RDFResourceRepository;
import com.carbonldp.repository.txn.RepositoryRuntimeException;

@Transactional
public class SesameRDFSourceService extends AbstractSesameLDPService implements RDFSourceService {

	public SesameRDFSourceService(SesameConnectionFactory connectionFactory, RDFResourceRepository resourceRepository, RDFDocumentRepository documentRepository) {
		super(connectionFactory, resourceRepository, documentRepository);
	}

	@Override
	public RDFSource exists(URI sourceURI) {
		// TODO Auto-generated method stub
		return null;
	}

	private static final String get_query;
	static {
		StringBuilder queryBuilder = new StringBuilder();
		//@formatter:off
		queryBuilder
			.append("CONSTRUCT {").append(NEW_LINE)
			.append(TAB).append("?s ?p ?o").append(NEW_LINE)
			.append("} WHERE {").append(NEW_LINE)
			.append(TAB).append("GRAPH ?sourceURI {").append(NEW_LINE)
			.append(TAB).append(TAB).append("?s ?p ?o").append(NEW_LINE)
			.append(TAB).append("}").append(NEW_LINE)
			.append("}")
		;
		//@formatter:on
		get_query = queryBuilder.toString();
	}

	@Override
	public RDFSource get(URI sourceURI) {
		RepositoryConnection connection = connectionFactory.getConnection();

		GraphQuery query;
		try {
			query = connection.prepareGraphQuery(QueryLanguage.SPARQL, get_query);
		} catch (RepositoryException e) {
			// TODO: Add error code
			throw new RepositoryRuntimeException(e);
		} catch (MalformedQueryException e) {
			throw new StupidityException(e);
		}

		query.setBinding("sourceURI", sourceURI);

		AbstractModel model = new LinkedHashModel();
		ValueFactory factory = ValueFactoryImpl.getInstance();
		try {
			GraphQueryResult result = query.evaluate();
			while (result.hasNext()) {
				Statement statement = result.next();
				model.add(factory.createStatement(statement.getSubject(), statement.getPredicate(), statement.getObject(), sourceURI));
			}
		} catch (QueryEvaluationException e) {
			// TODO: Add error code
			throw new RepositoryRuntimeException(e);
		}

		return new RDFSource(model, sourceURI);
	}

	@Override
	public Set<RDFSource> get(Set<URI> sourceURIs) {
		RepositoryConnection connection = connectionFactory.getConnection();
		Resource[] contexts = sourceURIs.toArray(new Resource[sourceURIs.size()]);

		AbstractModel model = new LinkedHashModel();
		try {
			RepositoryResult<Statement> statements = connection.getStatements(null, null, null, false, contexts);
			while (statements.hasNext()) {
				model.add(statements.next());
			}
		} catch (RepositoryException e) {
			// TODO: Add error code
			throw new RepositoryRuntimeException(e);
		}

		Set<RDFSource> sources = new HashSet<RDFSource>();
		for (URI sourceURI : sourceURIs) {
			sources.add(new RDFSource(model, sourceURI));
		}

		return sources;
	}

	@Override
	public DateTime touch(URI sourceURI) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DateTime touch(URI sourceURI, DateTime modified) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addAccessPoint(URI sourceURI, Container accessPoint) {
		// TODO Auto-generated method stub

	}

	@Override
	public void update(RDFSource source) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(URI sourceURI) {
		// TODO Auto-generated method stub

	}

}