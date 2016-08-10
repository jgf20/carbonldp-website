// There are files that reference this two dependencies and therefore they get included in the bundled file
// This causes a conflict with angular2-polyfills.js, as that file also declares them
// To avoid this, angular2-polyfills.js is no longer included in the index.html and zone and reflect are declared here instead
import "zone.js";
import "reflect-metadata";

import { bootstrap } from "@angular/platform-browser-dynamic";
import { Title } from "@angular/platform-browser";
import { provide, enableProdMode, Provider, ComponentRef } from "@angular/core";
import { FORM_PROVIDERS, APP_BASE_HREF } from "@angular/common";
import { ROUTER_PROVIDERS } from "@angular/router-deprecated";
import { HTTP_PROVIDERS } from "@angular/http";

import { appInjector, activeContext, CARBON_PROVIDERS } from "angular2-carbonldp/boot";
import { CARBON_SERVICES_PROVIDERS } from "angular2-carbonldp/services";

import Carbon from "carbonldp/Carbon";

import { AppComponent } from "./app.component";

import { BLOG_PROVIDERS } from "app/website/blog/Blog";
import { APP_DEV_PROVIDERS } from "./app-dev/app-dev";

import { Angulartics2 } from "angulartics2";

let carbon: Carbon = new Carbon();
carbon.setSetting( "domain", "<%- carbon.domain %>" );
activeContext.initialize( carbon );

let providers: Provider[] = [];
providers = providers
	.concat( CARBON_PROVIDERS )
	.concat( CARBON_SERVICES_PROVIDERS );

if( "<%- angular.debug %>" === "false" ) enableProdMode();

bootstrap( AppComponent, [
	FORM_PROVIDERS,
	ROUTER_PROVIDERS,
	HTTP_PROVIDERS,
	Title,
	Angulartics2,

	provide( APP_BASE_HREF, { useValue: "<%- url.base %>" } ),

	providers,
	BLOG_PROVIDERS,
	APP_DEV_PROVIDERS,
] ).then( ( appRef: ComponentRef<AppComponent> ) => {
	appInjector( appRef.injector );
} );
