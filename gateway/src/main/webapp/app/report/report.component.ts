import { Component, OnInit } from '@angular/core';
import { ReportService } from './report.service';
import { DomSanitizer } from '@angular/platform-browser';

@Component({
  selector: 'jhi-report',
  templateUrl: './report.component.html',
  styleUrls: ['report.scss']
})
export class ReportComponent implements OnInit {
  reportSrc: any;

  constructor(private reportService: ReportService, private sanitizer: DomSanitizer) {}

  ngOnInit() {
    this.reportService.getChartioMemberDashboardJwt().subscribe(res => {
      const url = res.body.url;
      const token = res.body.jwt;
      this.reportSrc = url + '?embed_token=' + token;
    });
  }

  safeUrl(url: string) {
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }
}
