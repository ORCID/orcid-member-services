import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router } from '@angular/router'
import { ActivationService } from './activation.service'

@Component({
  selector: 'app-activation',
  templateUrl: './activation.component.html',
  styleUrls: ['./activation.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActivationComponent implements OnInit {
  private readonly activationService = inject(ActivationService)
  private readonly route = inject(ActivatedRoute)
  private readonly router = inject(Router)
  private readonly destroyRef = inject(DestroyRef)

  protected readonly errorState = signal<string | null>(null)
  protected readonly successState = signal<string | null>(null)

  protected get error(): string | null {
    return this.errorState()
  }

  protected get success(): string | null {
    return this.successState()
  }

  ngOnInit() {
    this.route.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      this.activationService.get(params['key']).subscribe({
        next: () => {
          this.errorState.set(null)
          this.successState.set('OK')
        },
        error: () => {
          this.successState.set(null)
          this.errorState.set('ERROR')
        },
      })
    })
  }

  protected login() {
    this.router.navigate(['/login'])
  }
}
