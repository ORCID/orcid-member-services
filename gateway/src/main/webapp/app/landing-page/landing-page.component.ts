import { Component, OnInit } from '@angular/core';
import { NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { AccountService, Account } from 'app/core';

@Component({
  selector: 'jhi-landing-page',
  templateUrl: './landing-page.component.html',
  styleUrls: ['landing-page.scss']
})
export class LandingPageComponent implements OnInit {
  
  constructor(
    private accountService: AccountService,
    private eventManager: JhiEventManager
  ) {}

  ngOnInit() {
    
  }
}
