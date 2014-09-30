package com.base22.carbon.ldp;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import com.base22.carbon.APIPreferences.RetrieveContainerPreference;
import com.base22.carbon.CarbonException;
import com.base22.carbon.ldp.models.Container;
import com.base22.carbon.ldp.models.ContainerClass;
import com.base22.carbon.ldp.models.ContainerFactory;
import com.base22.carbon.ldp.models.RDFSource;
import com.base22.carbon.ldp.models.URIObject;
import com.base22.carbon.repository.WriteTransactionTemplate;
import com.hp.hpl.jena.rdf.model.Model;

@Service("s_BasicContainerService")
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS, value = "request")
public class BasicContainerService extends ContainerService {
	@Override
	public Container get(URIObject documentURIObject, List<RetrieveContainerPreference> preferences, String dataset) throws CarbonException {
		Container container = null;

		String documentURI = documentURIObject.getURI();

		String query = null;
		query = prepareBasicContainerQuery(documentURI, preferences);

		Model containerModel;
		containerModel = sparqlService.construct(query, dataset);

		ContainerFactory factory = new ContainerFactory();
		try {
			container = factory.create(documentURI, containerModel);
		} catch (CarbonException e) {
			if ( LOG.isErrorEnabled() ) {
				LOG.error("<< get() > The BasicContainer couldn't be fetched.");
			}
			throw e;
		}

		return container;
	}

	@Override
	protected void addMembers(URIObject containerURIObject, Container container, List<? extends RDFSource> members, WriteTransactionTemplate template)
			throws CarbonException {
		StringBuffer query = new StringBuffer();
		//@formatter:off
		query.append("INSERT DATA {");
		
		for(RDFSource member : members) {
			query
				.append("\n\tGRAPH <")
					.append(container.getURI())
				.append("> {")
					.append("\n\t\t<")
						.append(container.getURI())
					.append("> <")
						.append(ContainerClass.DEFAULT_HAS_MEMBER_RELATION)
						.append("> <")
						.append(member.getURI())
						.append(">.")
				.append("\n\t}.")
			;
		}
		
		query.append("}");
		//@formatter:on

		sparqlService.update(query.toString(), template);

		if ( container.getMemberOfRelation() != null ) {
			addInverseMembershipTriples(containerURIObject, container, members, template);
		}
	}

	protected Model getContainerProperties(URIObject containerURIObject, String datasetName) throws CarbonException {
		String containerURI = containerURIObject.getURI();

		StringBuffer query = new StringBuffer();
		//@formatter:off
		query
	        .append("CONSTRUCT {")
	            .append("\n\t<")
	                .append(containerURI)
	            .append("> ?containerPredicate ?containerObject")
	        .append("\n} WHERE {")
	            .append("\n\t GRAPH <")
	                .append(containerURI)
	            .append("> {")
	                .append("\n\t\t <")
	                    .append(containerURI)
	                .append("> ?containerPredicate ?containerObject.")
	                .append("\n\t\tFILTER( ?containerPredicate != <")
	                .append(ContainerClass.CONTAINS)
	                .append("> && ?containerPredicate != <")
	                .append(ContainerClass.DEFAULT_HAS_MEMBER_RELATION)
	                .append("> )")
	            .append("\n\t}")
	        .append("\n}")
	    ;
		//@formatter:on

		return sparqlService.construct(query.toString(), datasetName);
	}

	protected Model getContainmentTriples(URIObject containerURIObject, String datasetName) throws CarbonException {
		String containerURI = containerURIObject.getURI();

		StringBuffer query = new StringBuffer();
		//@formatter:off
		query
	        .append("CONSTRUCT {")
	            .append("\n\t<")
	                .append(containerURI)
	            .append("> <")
	                .append(ContainerClass.CONTAINS)
	            .append("> ?containedObjects.")
	        .append("\n} WHERE {")
	            .append("\n\t\t GRAPH <")
	                .append(containerURI)
	            .append("> {")
	                .append("\n\t\t\t <")
	                    .append(containerURI)
	                .append("> <")
	                    .append(ContainerClass.CONTAINS)
	                .append("> ?containedObjects.")
	            .append("\n\t\t}.")
	        .append("\n}")
	    ;
		//@formatter:on

		return sparqlService.construct(query.toString(), datasetName);
	}

	protected Model getMembershipTriples(URIObject containerURIObject, String datasetName) throws CarbonException {
		String containerURI = containerURIObject.getURI();

		StringBuffer query = new StringBuffer();
		//@formatter:off
		query
	        .append("CONSTRUCT {")
	            .append("\n\t<")
	                .append(containerURI)
	            .append("> <")
	                .append(ContainerClass.DEFAULT_HAS_MEMBER_RELATION)
	            .append("> ?membershipObjects.")
	        .append("\n} WHERE {")
	            .append("\n\t\t GRAPH <")
	                .append(containerURI)
	            .append("> {")
	                .append("\n\t\t\t <")
	                    .append(containerURI)
	                .append("> <")
	                    .append(ContainerClass.DEFAULT_HAS_MEMBER_RELATION)
	                .append("> ?membershipObjects.")
	            .append("\n\t\t}.")
	        .append("\n}")
	    ;
		//@formatter:on

		return sparqlService.construct(query.toString(), datasetName);
	}

	//@formatter:off
    protected String prepareBasicContainerQuery(String documentURI, List<RetrieveContainerPreference> preferences) {
        StringBuffer query = new StringBuffer();
        
        boolean containerProperties = preferences.contains(RetrieveContainerPreference.CONTAINER_PROPERTIES);
        boolean containmentTriples = preferences.contains(RetrieveContainerPreference.CONTAINMENT_TRIPLES);
        boolean membershipTriples = preferences.contains(RetrieveContainerPreference.MEMBERSHIP_TRIPLES);
        
        if( containerProperties && ! containmentTriples && ! membershipTriples ) {
            // TRUE && FALSE && FALSE
            // Just container properties
            
            query
                .append("CONSTRUCT {")
                    .append("\n\t<")
                        .append(documentURI)
                    .append("> ?containerPredicate ?containerObject")
                .append("\n} WHERE {")
                    .append("\n\t GRAPH <")
                        .append(documentURI)
                    .append("> {")
                        .append("\n\t\t <")
                            .append(documentURI)
                        .append("> ?containerPredicate ?containerObject.")
                        .append("\n\t\tFILTER( ?containerPredicate != <")
                        .append(ContainerClass.CONTAINS)
                        .append("> && ?containerPredicate != <")
                        .append(ContainerClass.DEFAULT_HAS_MEMBER_RELATION)
                        .append("> )")
                    .append("\n\t}")
                .append("\n}")
            ;
        
        } else if( containerProperties && containmentTriples && !membershipTriples ) {
            // TRUE && TRUE && FALSE
            // Container properties and containment triples
            
            query
                .append("CONSTRUCT {")
                    .append("\n\t<")
                        .append(documentURI)
                    .append("> ?containerPredicate ?containerObject")
                .append("\n} WHERE {")
                    .append("\n\t GRAPH <")
                        .append(documentURI)
                    .append("> {")
                        .append("\n\t\t <")
                            .append(documentURI)
                        .append("> ?containerPredicate ?containerObject.")
                        .append("\n\t\tFILTER( ?containerPredicate != <")
                        .append(ContainerClass.DEFAULT_HAS_MEMBER_RELATION)
                        .append("> )")
                    .append("\n\t}")
                .append("\n}")
            ;
        
        } else if( containerProperties && !containmentTriples && membershipTriples ) {
            // TRUE && FALSE && TRUE
            // Container properties and membership triples
            
            query
                .append("CONSTRUCT {")
                    .append("\n\t<")
                        .append(documentURI)
                    .append("> ?containerPredicate ?containerObject")
                .append("\n} WHERE {")
                    .append("\n\t GRAPH <")
                        .append(documentURI)
                    .append("> {")
                        .append("\n\t\t <")
                            .append(documentURI)
                        .append("> ?containerPredicate ?containerObject.")
                        .append("\n\t\tFILTER( ?containerPredicate != <")
                        .append(ContainerClass.CONTAINS)
                        .append("> )")
                    .append("\n\t}")
                .append("\n}")
            ;   
        
        } else if( containerProperties && containmentTriples && membershipTriples ) {
            // TRUE && TRUE && TRUE
            // Container properties, containment triples and membership triples
            
            query
                .append("CONSTRUCT {")
                    .append("\n\t<")
                        .append(documentURI)
                    .append("> ?containerPredicate ?containerObject.")
                .append("\n} WHERE {")
                    .append("\n\t\t GRAPH <")
                        .append(documentURI)
                    .append("> {")
                        .append("\n\t\t\t <")
                            .append(documentURI)
                        .append("> ?containerPredicate ?containerObject.")
                    .append("\n\t\t}.")
                .append("\n}")
            ;
        
        } else if( !containerProperties && !containmentTriples && !membershipTriples ) {
            // FALSE && FALSE && FALSE
            
            // Bad combination, you are asking for something empty
            return null;
        
        } else if( !containerProperties && containmentTriples && !membershipTriples ) {
            // FALSE && TRUE && FALSE
            // Containment triples
            
            query
                .append("CONSTRUCT {")
                    .append("\n\t<")
                        .append(documentURI)
                    .append("> <")
                        .append(ContainerClass.CONTAINS)
                    .append("> ?containedObjects.")
                .append("\n} WHERE {")
                    .append("\n\t\t GRAPH <")
                        .append(documentURI)
                    .append("> {")
                        .append("\n\t\t\t <")
                            .append(documentURI)
                        .append("> <")
                            .append(ContainerClass.CONTAINS)
                        .append("> ?containedObjects.")
                    .append("\n\t\t}.")
                .append("\n}")
            ;
        
        } else if( !containerProperties && !containmentTriples && membershipTriples ) {
            // FALSE && FALSE && TRUE
            // Membership triples
            
            query
                .append("CONSTRUCT {")
                    .append("\n\t<")
                        .append(documentURI)
                    .append("> <")
                        .append(ContainerClass.DEFAULT_HAS_MEMBER_RELATION)
                    .append("> ?membershipObjects.")
                .append("\n} WHERE {")
                    .append("\n\t\t GRAPH <")
                        .append(documentURI)
                    .append("> {")
                        .append("\n\t\t\t <")
                            .append(documentURI)
                        .append("> <")
                            .append(ContainerClass.DEFAULT_HAS_MEMBER_RELATION)
                        .append("> ?membershipObjects.")
                    .append("\n\t\t}.")
                .append("\n}")
            ;
        
        } else if( !containerProperties && containmentTriples && membershipTriples ) {
            // FALSE && TRUE && TRUE
            // Containment triples and Membership triples
            
            query
                .append("CONSTRUCT {")
                    .append("\n\t<")
                        .append(documentURI)
                    .append(">")
                        .append("\n\t\t<")
                            .append(ContainerClass.CONTAINS)
                        .append("> ?containedObjects;")
                        .append("\n\t\t<")
                            .append(ContainerClass.DEFAULT_HAS_MEMBER_RELATION)
                        .append("> ?membershipObjects.")
                .append("\n} WHERE {")
                    .append("\n\t\t GRAPH <")
                        .append(documentURI)
                    .append("> {")
                        .append("\n\t\t\t <")
                            .append(documentURI)
                        .append(">")
                            .append("\n\t\t\t\t<")
                                .append(ContainerClass.CONTAINS)
                            .append("> ?containedObjects;")
                            .append("\n\t\t\t\t<")
                                .append(ContainerClass.DEFAULT_HAS_MEMBER_RELATION)
                            .append("> ?membershipObjects.")
                    .append("\n\t\t}.")
                .append("\n}")
            ;
        }
        
        return query.toString();
    }
    //@formatter:on

}
