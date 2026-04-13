import { Component, OnInit, inject } from '@angular/core'
import { ActivatedRoute, Router } from '@angular/router'
import { ActivationService } from './activation.service'

@Component({
  selector: 'app-activation',
  templateUrl: './activation.component.html',
  styleUrls: ['./activation.component.scss'],
  standalone: false,
})
export class ActivationComponent implements OnInit {
  private activationService = inject(ActivationService)
  private route = inject(ActivatedRoute)
  private router = inject(Router)

  error: string | null = null
  success: string | null = null

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
