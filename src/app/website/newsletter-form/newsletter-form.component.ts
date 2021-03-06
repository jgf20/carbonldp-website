import { Component, OnInit } from "@angular/core";

import { Router } from "@angular/router";

import "semantic-ui/semantic";

import template from "./newsletter-form.component.html!";
import style from "./newsletter-form.component.css!text";

@Component( {
	selector: "newsletter-form",
	template: template,
	styles: [ style ],
} )

export class NewsletterFormComponent implements OnInit {
	private router:Router;
	private redirectPage;
	private errorPage;
	private location;

	subscribe:{ email:string } = {
		email: ""
	};

	constructor( router:Router ) {
		this.router = router;
		this.location = location;
	}

	ngOnInit() {
		this.redirectPage = document.location.origin + "/signup-thanks/";
		this.errorPage = document.location.origin + "/error/";
	}

	onSubmit( $event:any ):void {
		let icpForm:HTMLFormElement = <any>document.getElementById( "icpsignup" );
		icpForm.action = "https://app.icontact.com/icp/signup.php";
		icpForm.submit();

	}
}
