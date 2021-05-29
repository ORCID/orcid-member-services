import { Component, OnInit } from '@angular/core';
import { ReportService } from './report.service';
import { DomSanitizer } from '@angular/platform-browser';

@Component({
  selector: 'jhi-report',
  templateUrl: './report.component.html',
  styleUrls: ['report.scss']
})
export class ReportComponent implements OnInit {
  memberReportSrc: any;
  integrationReportSrc: any;

  constructor(private reportService: ReportService, private sanitizer: DomSanitizer) {}

  ngOnInit() {
    this.reportService.getChartioMemberDashboardInfo().subscribe(res => {
      console.log('member dashboard info: ' + JSON.stringify(res.body));
      const url = res.body.url;
      const token = res.body.jwt;
      this.memberReportSrc = url + '?embed_token=' + token;
    });

    this.reportService.getChartioIntegrationDashboardInfo().subscribe(res => {
      console.log('integration dashboard info: ' + JSON.stringify(res.body));
      const url = res.body.url;
      const token = res.body.jwt;
      this.integrationReportSrc = url + '?embed_token=' + token;
    });
  }

  safeUrl(url: string) {
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }
}
