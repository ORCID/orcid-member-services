import { Component, OnInit } from '@angular/core';
import { MSMemberService } from 'app/entities/MSUserService/ms-members/ms-member.service.ts';

@Component({
  selector: 'jhi-report',
  templateUrl: './report.component.html',
  styleUrls: ['report.scss']
})
export class ReportComponent implements OnInit {
  token: string;

  constructor(private memberService: MSMemberService) {}

  ngOnInit() {}
}
