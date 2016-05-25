import { Component, ElementRef, Input } from "@angular/core";
import { CORE_DIRECTIVES } from "@angular/common";

import $ from "jquery";
import "semantic-ui/semantic";

import * as SDKContext from "carbonldp/SDKContext";
import * as RDFDocument from "carbonldp/RDF/Document";
import * as HTTP from "carbonldp/HTTP";

import DocumentsResolverService from "./../document-explorer/DocumentsResolverService"
import DocumentViewerComponent from "./document-viewer/DocumentViewerComponent";
import DocumentTreeViewComponent from "./document-treeview/DocumentTreeViewComponent";
import { Message } from "./../../../../components/errors-area/ErrorsAreaComponent";

import template from "./template.html!";
import "./style.css!";

@Component( {
	selector: "document-explorer",
	template: template,
	directives: [ CORE_DIRECTIVES, DocumentTreeViewComponent, DocumentViewerComponent ],
} )

export default class DocumentExplorerComponent {

	element:ElementRef;
	$element:JQuery;

	loadingDocument:boolean = false;
	inspectingDocument:RDFDocument.Class;
	documentsResolverService:DocumentsResolverService;
	messages:Message[] = [];

	@Input() documentContext:SDKContext.Class;

	constructor( element:ElementRef, documentsResolverService:DocumentsResolverService ) {
		this.element = element;
		this.documentsResolverService = documentsResolverService;
	}

	ngAfterViewInit():void {
		this.$element = $( this.element.nativeElement );
	}

	onLoadingDocument( loadingDocument:boolean ):void {
		this.loadingDocument = loadingDocument;
	}

	resolveDocument( uri:string ):void {
		this.loadingDocument = true;
		this.documentsResolverService.get( uri, this.documentContext ).then( ( document:RDFDocument.Class )=> {
			this.inspectingDocument = document;
			this.loadingDocument = false;
		} );
	}

	handleError( error:HTTP.Errors.Error ):void {
		let message:Message = {
			title: error.name,
			content: (<XMLHttpRequest>error.response.request).statusText,
			statusCode: "" + error.response.status,
			statusMessage: (<XMLHttpRequest>error.response.request).statusText,
			endpoint: (<any>error.response.request).responseURL,
		};
		this.messages.push( message );
	}
}
