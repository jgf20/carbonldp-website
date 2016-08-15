import { Component, ElementRef, AfterViewInit } from "@angular/core";
import { CORE_DIRECTIVES } from "@angular/common";

import $ from "jquery";
import "semantic-ui/semantic";

import template from "./linked-data-concepts.view.html!";
import style from "./linked-data-concepts.view.css!text";

@Component( {
	selector: "linked-data-concepts",
	template: template,
	directives: [ CORE_DIRECTIVES ],
	styles: [ style ],
} )
export class LinkedDataConceptsView implements AfterViewInit {
	private element:ElementRef;
	private $element:JQuery;

	constructor( element:ElementRef ) {
		this.element = element;
	}

	ngAfterViewInit():void {
		this.$element = $( this.element.nativeElement );
	}

}

export default LinkedDataConceptsView;