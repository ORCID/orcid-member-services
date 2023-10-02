import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  title = 'gateway';

  constructor(
    private http: HttpClient
  ) {}

  ngOnInit(): void {
     this.http.get<any>("http://localhost:8080/" + 'services/userservice/api/account', { observe: 'response' }).subscribe();
    
  }
  
}
