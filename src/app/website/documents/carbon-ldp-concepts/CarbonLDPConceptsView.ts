import { Component, ElementRef } from "@angular/core";
import { CORE_DIRECTIVES } from "@angular/common";
import { Title } from "@angular/platform-browser";

import $ from "jquery";
import "semantic-ui/semantic";

import template from "./template.html!";

@Component( {
	selector: "carbon-ldp-concepts",
	template: template,
	directives: [ CORE_DIRECTIVES ],
	providers: [ Title ],
} )
export default class CarbonLDPConceptsView {
	element:ElementRef;
	$element:JQuery;
	title;

	constructor( element:ElementRef, title:Title ) {
		this.element = element;
		this.title = title;
	}

	ngAfterViewInit():void {
		this.$element = $( this.element.nativeElement );
	}

	routerOnActivate():void {
		this.title.setTitle( "Carbon LDP Concepts" );
	}
}
