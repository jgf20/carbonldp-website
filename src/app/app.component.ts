import { Component } from "@angular/core";

import { Router, Event, NavigationEnd, ActivatedRoute, ActivatedRouteSnapshot } from "@angular/router";
import { Title } from "@angular/platform-browser";

import { Angulartics2GoogleAnalytics } from "angulartics2/src/providers/angulartics2-google-analytics";
import { Angulartics2 } from "angulartics2";


import template from "./app.component.html!";
import style from "./app.component.css!text";

@Component( {
	selector: "app",
	template: template,
	styles: [ style ],
} )

export class AppComponent {
	router:Router;
	title:Title;
	route:ActivatedRoute;
	// Importing angulartics2, angulartics2GoogleAnalytics as per documentation of angulartics2 plug-in
	constructor( route:ActivatedRoute, title:Title, router:Router, angulartics2:Angulartics2, angulartics2GoogleAnalytics:Angulartics2GoogleAnalytics ) {
		this.router = router;
		this.title = title;
		this.route = route;
		this.router.events.subscribe( ( event:Event ) => {
			if( event instanceof NavigationEnd ) {
				this.defineTitle();
			}
		} );
	}

	private defineTitle() {
		let title:string = "",
			activatedRoutes:ActivatedRouteSnapshot[] = [],
			currentRoute:ActivatedRoute = this.route.root;
		do {
			if( currentRoute.snapshot.data[ "title" ] !== false ) {
				if( ! ! (typeof currentRoute.snapshot.data[ "title" ] !== 'undefined' || typeof currentRoute.snapshot.data[ "displayName" ] !== "undefined") && currentRoute.snapshot )
					activatedRoutes.push( currentRoute.snapshot );
			}
			currentRoute = currentRoute.children[ 0 ];
		} while ( currentRoute );

		activatedRoutes.forEach( ( snapshot:ActivatedRouteSnapshot, idx:number )=> {
			if( idx === 0 ) return;
			if( idx !== (activatedRoutes.length - 1) && typeof snapshot.data[ "title" ] === "undefined" ) return;
			title += this.getTitle( snapshot );
			if( idx < activatedRoutes.length - 1 ) title += " > ";
		} );
		title = title + (activatedRoutes.length > 1 ? " | " : " ") + this.getTitle( activatedRoutes[ 0 ] );
		this.title.setTitle( title );
	}

	private getTitle( snapShot:ActivatedRouteSnapshot ):string {
		let title:string = "";
		if( typeof snapShot.data[ "param" ] !== "undefined" ) {
			title += snapShot.data[ "displayName" ] + " ( " + snapShot.params[ snapShot.data[ "param" ] ] + " ) ";
		} else if( typeof snapShot.data[ "title" ] === 'string' ) {
			title += snapShot.data[ "title" ]
		} else title += snapShot.data[ "displayName" ];
		return title;
	}
}

export default AppComponent;
