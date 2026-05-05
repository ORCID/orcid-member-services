import { Component, OnInit, inject } from '@angular/core'
import { ActivatedRoute } from '@angular/router'
import { AccountService } from '../account'

@Component({
  selector: 'app-error',
  templateUrl: './error.component.html',
  standalone: false,
})
export class ErrorComponent implements OnInit {
  private route = inject(ActivatedRoute)
  private accountService = inject(AccountService)

  errorMessage: string | undefined
  error403: boolean | undefined
  error404: boolean | undefined

  ngOnInit() {
    this.route.data.subscribe((routeData) => {
      if (routeData['error403']) {
        this.error403 = routeData['error403']
      }
      if (routeData['error404']) {
        this.error404 = routeData['error404']
      }
      if (routeData['errorMessage']) {
        this.errorMessage = routeData['errorMessage']
      }
    })
  }
}
