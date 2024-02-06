import { Component, OnInit } from '@angular/core'
import { ActivatedRoute, Router } from '@angular/router'
import { ActivationService } from './activation.service'

@Component({
  selector: 'app-activation',
  templateUrl: './activation.component.html',
  styleUrls: ['./activation.component.scss'],
})
export class ActivationComponent implements OnInit {
  error: string | null = null
  success: string | null = null

  constructor(
    private activationService: ActivationService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe((params) => {
      this.activationService.get(params['key']).subscribe({
        next: () => {
          this.error = null
          this.success = 'OK'
        },
        error: () => {
          this.success = null
          this.error = 'ERROR'
        },
      })
    })
  }

  login() {
    this.router.navigate(['/login'])
  }
}
