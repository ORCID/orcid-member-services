import { Component, OnInit } from '@angular/core';
import { ReportService } from './report.service';
import { DomSanitizer } from '@angular/platform-browser';
import { MSMemberService } from '../entities/MSUserService/ms-members/ms-member.service';
import { AccountService } from '../core/auth/account.service';

@Component({
  selector: 'jhi-report',
  templateUrl: './report.component.html',
  styleUrls: ['report.scss']
})
export class ReportComponent implements OnInit {
  memberReportSrc: any;
  integrationReportSrc: any;
  consortiumReportSrc: any;
  consortiumLead: boolean;

  constructor(
    private reportService: ReportService,
    private accountService: AccountService,
    private memberService: MSMemberService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit() {
    this.reportService.getChartioMemberDashboardInfo().subscribe(res => {
      const url = res.body.url;
      const token = res.body.jwt;
      this.memberReportSrc = url + '?embed_token=' + token;
    });

    this.reportService.getChartioIntegrationDashboardInfo().subscribe(res => {
      const url = res.body.url;
      const token = res.body.jwt;
      this.integrationReportSrc = url + '?embed_token=' + token;
    });

    this.accountService.fetch().subscribe(accountResponse => {
      const salesforceId = accountResponse.body.salesforceId;
      this.memberService.find(salesforceId).subscribe(memberResponse => {
        this.consortiumLead = memberResponse.body.isConsortiumLead;
        if (this.consortiumLead) {
          this.reportService.getChartioConsortiumDashboardInfo().subscribe(res => {
            const url = res.body.url;
            const token = res.body.jwt;
            this.consortiumReportSrc = url + '?embed_token=' + token;
          });
        }
      });
    });
  }

  safeUrl(url: string) {
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }
}
