import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class FeatureToggleService {
  private features: Record<string, boolean> = {};
  private initialized$ = new BehaviorSubject<boolean>(false);
  private http = inject(HttpClient);

  // Invoke during application boot routines (or App Component initialization)
  public initFeatures(): Observable<Record<string, boolean>> {
    return this.http.get<Record<string, boolean>>('/userservice/features').pipe(
      tap(flags => {
        this.features = flags;
        this.initialized$.next(true);
      })
    );
  }

  public isEnabled(featureKey: string): boolean {
    return !!this.features[featureKey];
  }

  public get initializationStatus$(): Observable<boolean> {
    return this.initialized$.asObservable();
  }
}
