import { Component, CORE_DIRECTIVES, DynamicComponentLoader, ElementRef, View} from 'angular2/angular2';
import { ROUTER_DIRECTIVES, ROUTER_PROVIDERS, Router, Instruction, RouteParams } from 'angular2/router';
import ContentService from 'app/content/ContentService';

import * as CodeMirrorComponent from "app/components/code-mirror/CodeMirrorComponent";

import template from './template.html!';

@Component({
    // Selector matches the route alias?
    selector: 'compiled-content',
    template: template,
    directives: [ CORE_DIRECTIVES, ROUTER_DIRECTIVES, CodeMirrorComponent.Class ]
})
export default class ContentView {

    static parameters = [ [ Router ], [ ContentService ], [RouteParams], [DynamicComponentLoader] , [ElementRef]];

    router:Router;
    contentService:ContentService;
    routeParams: RouteParams;
    dynamicComponentLoader: DynamicComponentLoader;
    elementRef:ElementRef;

    constructor( router:Router, contentService:ContentService, routeParams:RouteParams ,  dynamicComponentLoader: DynamicComponentLoader, elementRef:ElementRef) {

        console.log(">> ContentView -> constructed");

        this.router = router;
        this.contentService = contentService;
        this.routeParams = routeParams;
        this.dynamicComponentLoader = dynamicComponentLoader;
        this.elementRef = elementRef;

        let id = this.routeParams.get('id');

        console.log("-- ContentView -> Got id: " + id);


        // START: OPTION A -------------------------------------------------------------------------
        // USE THIS TO LOAD A TEMPLATE DIRECTLY FROM TEMPLATE URL
        @Component({
            selector: 'compiled-component',
            directives: [CodeMirrorComponent.Class],
            templateUrl: 'http://127.0.0.1:8080/assets/documents/' + id + '.html'
        })
        class CompiledComponent {

            //testProperty:string = "component in scope";

        };

        dynamicComponentLoader.loadIntoLocation(CompiledComponent, elementRef, 'container');
        // END: OPTION A ---------------------------------------------------------------------------


        // START: OPTION B: ------------------------------------------------------------------------
        // USE THIS TO LOAD A TEMPLATE DYNAMICALLY FROM HTTP USING A SERVICE

        /*
        this.contentService.getDocumentById( id ).then(
            ( content )=> {
                @Component({
                    selector: 'compiled-component',
                    directives: [CodeMirrorComponent.Class],
                    template: content
                })
                class CompiledComponent {

                    //testProperty:string = "component in scope";

                };

                dynamicComponentLoader.loadIntoLocation(CompiledComponent, elementRef, 'container');
            }
        );
        */

        // END: OPTION B --------------------------------------------------------------------------



    }

}