import { Component, ElementRef } from "@angular/core";
import { CORE_DIRECTIVES } from "@angular/common";
import { Title } from "@angular/platform-browser";

import $ from "jquery";
import "semantic-ui/semantic";

import template from "./template.html!";
import "./style.css!";

@Component( {
	selector: "linked-data-concepts",
	template: template,
	directives: [ CORE_DIRECTIVES ],
	providers: [ Title ],
} )
export default class LinkedDataConceptsView {
	element:ElementRef;
	$element:JQuery;
	title:Title;

	constructor( element:ElementRef, title:Title ) {
		this.element = element;
		this.title = title;
	}

	ngAfterViewInit():void {
		this.$element = $( this.element.nativeElement );
	}

	routerOnActivate():void {
		this.title.setTitle( "Linked Data Concepts" );
	}
}
