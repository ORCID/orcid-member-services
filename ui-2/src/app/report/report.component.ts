import { Component, ChangeDetectionStrategy, OnInit, signal, inject } from '@angular/core'
import { ReportService } from './report.service'
import { DomSanitizer } from '@angular/platform-browser'
import { ActivatedRoute } from '@angular/router'

@Component({
  selector: 'app-report',
  templateUrl: './report.component.html',
  styleUrls: ['report.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportComponent implements OnInit {
  private reportService = inject(ReportService)
  private sanitizer = inject(DomSanitizer)
  private activatedRoute = inject(ActivatedRoute)

  protected reportSrc = signal<any>(null)
  protected reportType = signal<string | undefined>(undefined)

  ngOnInit() {
    this.activatedRoute.data.subscribe((data) => {
      this.reportType.set(data['reportType'])

      const reportType = this.reportType()
      if (reportType) {
        this.reportService.getDashboardInfo(reportType).subscribe((res) => {
          const url = res.url
          const token = res.jwt
          this.reportSrc.set(this.safeUrl(url + '?_token=' + token))
        })
      }
    })
  }

  safeUrl(url: string) {
    return this.sanitizer.bypassSecurityTrustResourceUrl(url)
  }
}
