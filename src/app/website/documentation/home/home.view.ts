import { Component, ElementRef, AfterViewInit } from "@angular/core";

import $ from "jquery";
import "semantic-ui/semantic";

import template from "./home.view.html!";
import style from "./home.view.css!text";

@Component( {
	selector: "documents-list",
	template: template,
	styles: [ style ],
} )
export class HomeView implements AfterViewInit {
	private element:ElementRef;
	private $element:JQuery;

	constructor( element:ElementRef ) {
		this.element = element;
	}

	ngAfterViewInit():void {
		this.$element = $( this.element.nativeElement );
		this.$element.find( ".doc-categories a[href]" ).on( "click", this.scrollTo );
	}


	scrollTo( event:any ):boolean {
		let id:string = $( event.currentTarget ).attr( "href" ).replace( "#", "" );
		let $element:JQuery = $( "#" + id );
		let position:number = $element.offset().top - 100;

		$( "html, body" ).animate( {
			scrollTop: position
		}, 500 );
		location.hash = "#" + id;
		event.stopImmediatePropagation();
		event.preventDefault();

		return false;
	}
}

export default HomeView;
