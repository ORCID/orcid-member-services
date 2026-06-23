import { Component, ChangeDetectionStrategy, DestroyRef, OnInit, signal, inject } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { ActivatedRoute } from '@angular/router'
import { AccountService } from '../account'

@Component({
  selector: 'app-error',
  templateUrl: './error.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErrorComponent implements OnInit {
  private route = inject(ActivatedRoute)
  private accountService = inject(AccountService)
  private destroyRef = inject(DestroyRef)

  protected errorMessage = signal<string | undefined>(undefined)
  protected error403 = signal<boolean | undefined>(undefined)
  protected error404 = signal<boolean | undefined>(undefined)

  ngOnInit() {
    this.route.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((routeData: any) => {
      if (routeData['error403']) {
        this.error403.set(routeData['error403'])
      }
      if (routeData['error404']) {
        this.error404.set(routeData['error404'])
      }
      if (routeData['errorMessage']) {
        this.errorMessage.set(routeData['errorMessage'])
      }
    })
  }
}
