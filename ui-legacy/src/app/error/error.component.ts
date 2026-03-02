import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AccountService } from '../account';

@Component({
  selector: 'app-error',
  templateUrl: './error.component.html'
})
export class ErrorComponent implements OnInit {
  errorMessage: string | undefined;
  error403: boolean | undefined;
  error404: boolean | undefined;

  constructor(private route: ActivatedRoute, private accountService: AccountService) {}

  ngOnInit() {
    this.route.data.subscribe(routeData => {
      if (routeData['error403']) {
        this.error403 = routeData['error403'];
      }
      if (routeData['error404']) {
        this.error404 = routeData['error404'];
      }
      if (routeData['errorMessage']) {
        this.errorMessage = routeData['errorMessage'];
      }
    });
  }
}
