import { Component, ElementRef, ChangeDetectorRef, AfterViewInit  } from "@angular/core";

import "semantic-ui/semantic";

import template from "./access-points.view.html!";

@Component( {
	selector: "access-points",
	template: template,
} )

export class AccessPointsView {
	contentReady:boolean = false;

	private element:ElementRef;
	private $element:JQuery;
	private changeDetector:ChangeDetectorRef;

	constructor( element:ElementRef, changeDetector:ChangeDetectorRef ) {
		this.element = element;
		this.changeDetector = changeDetector;
	}

	ngAfterViewInit():void {
		this.$element = $( this.element.nativeElement );
		this.initializeAccordions();
		this.initializePopUp();
		this.initializeSidebar();
	}

	initializeAccordions():void {
		this.$element.find( ".ui.accordion" ).accordion();
	}

	initializeSidebar():void {
		window.setTimeout( () => {
			this.contentReady = true;
		}, 0 );
	}

	initializePopUp():void {
		this.$element.find( ".ui.definition" ).popup( {
			on: "hover"
		} );
	}
}

export default AccessPointsView;