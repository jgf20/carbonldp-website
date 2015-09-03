package com.carbonldp.web.config;

import com.carbonldp.HTTPHeaders;
import com.carbonldp.descriptions.APIPreferences;
import com.carbonldp.models.HTTPHeader;
import com.carbonldp.models.HTTPHeaderValue;
import com.carbonldp.utils.RDFNodeUtil;
import com.google.common.collect.ImmutableSet;
import org.springframework.web.servlet.mvc.condition.AbstractRequestCondition;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author MiguelAraCo
 * @see CustomRequestMappingHandlerMapping
 * @see org.springframework.web.servlet.mvc.condition.HeadersRequestCondition
 * @see org.springframework.web.servlet.mvc.method.RequestMappingInfo
 * @since _version_
 */
public class InteractionModelRequestCondition extends AbstractRequestCondition<InteractionModelRequestCondition> {
	private final Set<APIPreferences.InteractionModel> interactionModels;

	public InteractionModelRequestCondition( APIPreferences.InteractionModel... interactionModels ) {
		this.interactionModels = new HashSet<>( Arrays.asList( interactionModels ) );
	}

	@Override
	protected Set<APIPreferences.InteractionModel> getContent() {
		return this.getInteractionModels();
	}

	@Override
	protected String getToStringInfix() {
		return " && ";
	}

	@Override
	public InteractionModelRequestCondition combine( InteractionModelRequestCondition other ) {
		return ! other.getInteractionModels().isEmpty() ? other : this;
	}

	@Override
	public InteractionModelRequestCondition getMatchingCondition( HttpServletRequest request ) {
		if ( this.getInteractionModels().size() == 0 ) return this;

		APIPreferences.InteractionModel requestInteractionModel = getRequestInteractionModel( request );
		if ( requestInteractionModel == null ) return null;

		for ( APIPreferences.InteractionModel interactionModel : this.getInteractionModels() ) {
			if ( interactionModel.equals( requestInteractionModel ) ) return this;
		}

		return null;
	}

	private APIPreferences.InteractionModel getRequestInteractionModel( HttpServletRequest request ) {
		HTTPHeader preferHeader = new HTTPHeader( request.getHeaders( HTTPHeaders.PREFER ) );
		List<HTTPHeaderValue> filteredValues = HTTPHeader.filterHeaderValues( preferHeader, null, null, "rel", "interaction-model" );
		if ( filteredValues.size() == 0 ) return null;

		String interactionModelURI = filteredValues.get( 0 ).getMainValue();
		return RDFNodeUtil.findByURI( interactionModelURI, APIPreferences.InteractionModel.class );
	}

	@Override
	public int compareTo( InteractionModelRequestCondition other, HttpServletRequest httpServletRequest ) {
		// TODO: Corroborate that this is the correct implementation
		return other.getInteractionModels().size() - this.getInteractionModels().size();
	}

	public Set<APIPreferences.InteractionModel> getInteractionModels() {
		return ImmutableSet.copyOf( this.interactionModels );
	}
}
