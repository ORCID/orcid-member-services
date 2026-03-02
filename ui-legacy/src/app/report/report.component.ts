import { Component, OnInit } from '@angular/core'
import { ReportService } from './report.service'
import { DomSanitizer } from '@angular/platform-browser'
import { ActivatedRoute } from '@angular/router'

@Component({
  selector: 'app-report',
  templateUrl: './report.component.html',
  styleUrls: ['report.scss'],
})
export class ReportComponent implements OnInit {
  reportSrc: any
  reportType: string | undefined

  constructor(
    private reportService: ReportService,
    private sanitizer: DomSanitizer,
    private activatedRoute: ActivatedRoute
  ) {}

  ngOnInit() {
    this.activatedRoute.data.subscribe((data) => {
      this.reportType = data['reportType']

      if (this.reportType) {
        this.reportService.getDashboardInfo(this.reportType).subscribe((res) => {
          const url = res.url
          const token = res.jwt
          this.reportSrc = this.safeUrl(url + '?_token=' + token)
        })
      }
    })
  }

  safeUrl(url: string) {
    return this.sanitizer.bypassSecurityTrustResourceUrl(url)
  }
}
