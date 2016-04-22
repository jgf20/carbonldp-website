import { Component, ElementRef } from "angular2/core";
import { CORE_DIRECTIVES } from "angular2/common";
import { Title } from "angular2/platform/browser";

import $ from "jquery";
import "semantic-ui/semantic";

import template from "./template.html!";
import "./style.css!";

@Component( {
	selector: "signup-thanks",
	template: template,
	directives: [ CORE_DIRECTIVES ],
	providers: [ Title ]
} )

export default class SignupThanksView {
	element:ElementRef;
	$element:JQuery;
	title:Title;

	constructor( element:ElementRef, title:Title ) {
		this.element = element;
		this.title = title;
	}

	ngAfterViewInit():void {
		this.$element = $( this.element.nativeElement );
		ga( "send", "event", "Newsletter", "Subscription" );
	}

	routerOnActivate():void {
		this.title.setTitle( "Thank you!" );
	}
}
