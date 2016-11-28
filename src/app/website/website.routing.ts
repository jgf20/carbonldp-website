import { ModuleWithProviders } from "@angular/core";
import { Routes, RouterModule } from "@angular/router";

// Components
import { WebsiteView } from "./website.view";
import { HomeView } from "./home/home.view";
import { RegisterView } from "./register/register.view";
import { SignupThanksView } from "./signup-thanks/signup-thanks.view";
import { UIExamplesView } from "./ui-examples/ui-examples.view";
import { LicenseView } from './license/license.view'
import { DocumentationView } from "app/website/documentation/documentation.view";
import { AboutCarbonLDPView } from "./about/about-carbon-ldp.view";
import { CommunityAndSupportView } from "./community-and-support/community-and-support.view";
import { GetStartedView } from "./get-started/get-started.view";

// Documentation
import { HomeView as DocumentationHomeView } from "./documentation/home/home.view";
import { QuickStartGuideView } from "./documentation/quick-start-guide/quick-start-guide.view";

// Documentation -> Essential Concepts
import { EssentialConceptsView } from "./documentation/essential-concepts/essential-concepts.view";
import { LinkedDataConceptsView } from "./documentation/essential-concepts/linked-data-concepts.view";
// Documentation -> RESTApi
import { RESTApiView } from "./documentation/rest-api/rest-api.view";
import { GettingStartedView } from "./documentation/rest-api/getting-started.view";
import { InteractionModelsView } from "./documentation/rest-api/interaction-models.view";
import { ObjectModelView } from "./documentation/rest-api/object-model.view";
import { RDFSourceView } from "./documentation/rest-api/rdf-source.view";
import { ContainersView } from "./documentation/rest-api/containers.view";
// Documentation -> JavaScriptSDK
import { JavaScriptSDKView } from "./documentation/javascript-sdk.view";
import { JavaScriptSDKHomeView } from "./documentation/javascript-sdk/javascript-sdk-home.view";
import { GettingStartedView as JavaScriptSDKGettingStartedView } from "./documentation/javascript-sdk/getting-started.view";
import { ContextsView } from "./documentation/javascript-sdk/contexts.view";
import { ObjectModelView as JavaScriptSDKObjectModelView } from "./documentation/javascript-sdk/object-model.view";
import { ObjectSchemaView } from "./documentation/javascript-sdk/object-schema.view";
import { UploadingFilesView } from "./documentation/javascript-sdk/files.view";
import { QueryingView } from "./documentation/javascript-sdk/querying.view";
import { AuthenticationView } from "./documentation/javascript-sdk/authentication.view";
import { AuthorizationView } from "./documentation/javascript-sdk/authorization.view";
import { AccessPointsView } from "./documentation/javascript-sdk/access-points.view";
import websiteMetadata from  "./website.routing.json!";

// Object that contains ONLY the components tree of website routes.
const websiteRoutesWithOutMetadata = {
		"": {
			"component": WebsiteView,
			"children": {
				"": {
					"component": HomeView,
				},
				"community-and-support": {
					"component": CommunityAndSupportView,
				},
				"get-started": {
					"component": GetStartedView,
				}, "about": {
					"component": AboutCarbonLDPView,
				},
				"register": {
					"component": RegisterView,
				},
				"signup-thanks": {
					"component": SignupThanksView,
				},
				"ui-examples": {
					"component": UIExamplesView,
				},
				"license": {
					"component": LicenseView,
				},
				"documentation": {
					"component": DocumentationView,
					"children": {
						"": {
							"component": DocumentationHomeView,
						},
						"quick-start-guide": {
							"component": QuickStartGuideView,
						},
						"essential-concepts": {
							"component": EssentialConceptsView,
						},
						"linked-data-concepts": {
							"component": LinkedDataConceptsView,
						},
						"rest-api": {
							"component": RESTApiView,
						},
						"rest-api-getting-started": {
							"component": GettingStartedView,
						},
						"rest-api-interaction-models": {
							"component": InteractionModelsView,
						},
						"rest-api-object-model": {
							"component": ObjectModelView,
						},
						"rest-api-containers": {
							"component": ContainersView,
						},
						"rest-api-rdf-source": {
							"component": RDFSourceView,
						},
						"javascript-sdk": {
							"component": JavaScriptSDKView,
							"children": {
								"": {
									"component": JavaScriptSDKHomeView,
								},
								"getting-started": {
									"component": JavaScriptSDKGettingStartedView,
								},
								"contexts": {
									"component": ContextsView,
								},
								"object-model": {
									"component": JavaScriptSDKObjectModelView,
								},
								"object-schema": {
									"component": ObjectSchemaView,
								},
								"files": {
									"component": UploadingFilesView,
								}
								,
								"querying": {
									"component": QueryingView,
								},
								"authentication": {
									"component": AuthenticationView,
								},
								"authorization": {
									"component": AuthorizationView,
								},
								"access-points": {
									"component": AccessPointsView,
								}
							}
						}
					}
				}
			}
		}
	};

let routes = ( ()=>{
	// Process and refactor websiteMetadata and websiteRoutesWithOutMetadata, to automatically generate complete website routes.
	function refactorJSON(components,metadata){

		// merge both JSON objects
	let refactoredJSON = metadata;// Copying metadata to a new Object

	for (let attributeName of Object.keys( components )) {
		if(refactoredJSON.hasOwnProperty(attributeName)) {
			if ( components[attributeName] !== null && typeof components[attributeName] === "object" ) {

				// Add path attribute in the appropriate place
				if( attributeName!=="component" && attributeName!=="children") refactoredJSON[attributeName]["path"]= attributeName;

				// Recursive call if nested object is found
				refactoredJSON[attributeName] = refactorJSON(components[attributeName], refactoredJSON[attributeName]);

				// Convert children object into an array
				// TODO: Could use Object.values( refactoredJSON[attributeName] ) once the method is not in experimental phase.
				if( attributeName ==="children" ) refactoredJSON[attributeName] = Object.keys(refactoredJSON[attributeName]).map(function(k){return refactoredJSON[attributeName][k];});
			}

		} else {//else copy the property from components
			refactoredJSON[attributeName] = components[attributeName];
		}
	}

	return refactoredJSON;
	}

	let JSONroutes = refactorJSON(websiteRoutesWithOutMetadata,websiteMetadata);
	// Convert routes first level Object into an array
	let routes = Object.keys(JSONroutes).map(function(k){return JSONroutes[k];});

	return routes;

})();

const websiteRoutes:Routes = routes;

export const routing:ModuleWithProviders = RouterModule.forChild( websiteRoutes );