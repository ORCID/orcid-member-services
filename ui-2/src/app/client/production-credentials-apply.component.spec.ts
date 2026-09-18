import { ComponentFixture, TestBed } from '@angular/core/testing'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { ActivatedRoute, convertToParamMap, Router, RouterModule } from '@angular/router'
import { AlertService } from '../shared/service/alert.service'
import { ProductionCredentialsApplyComponent } from './production-credentials-apply.component'

type ProductionCredentialsApplyComponentInternals = {
  showRedirectUris: () => boolean
}

const internals = (component: ProductionCredentialsApplyComponent): ProductionCredentialsApplyComponentInternals =>
  component as unknown as ProductionCredentialsApplyComponentInternals

describe('ProductionCredentialsApplyComponent', () => {
  let component: ProductionCredentialsApplyComponent
  let fixture: ComponentFixture<ProductionCredentialsApplyComponent>
  let router: Router
  let alertService: jasmine.SpyObj<AlertService>

  beforeEach(() => {
    const alertServiceSpy = jasmine.createSpyObj('AlertService', ['broadcast'])

    TestBed.configureTestingModule({
      imports: [RouterModule.forRoot([]), ProductionCredentialsApplyComponent],
      providers: [
        { provide: AlertService, useValue: alertServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: { parent: { snapshot: { paramMap: convertToParamMap({ memberId: 'memberId' }) } } },
        },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    })
    router = TestBed.inject(Router)
    spyOn(router, 'navigate')
    alertService = TestBed.inject(AlertService) as jasmine.SpyObj<AlertService>
    fixture = TestBed.createComponent(ProductionCredentialsApplyComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  it('should create with an invalid form', () => {
    expect(component).toBeTruthy()
    expect(component.applyForm.invalid).toBeTrue()
  })

  it('should hide and un-require redirectUris when authenticateOrcidIds is NO', () => {
    component.applyForm.controls.authenticateOrcidIds.setValue('NO')
    fixture.detectChanges()

    expect(internals(component).showRedirectUris()).toBeFalse()
    expect(component.applyForm.controls.redirectUris.errors).toBeNull()
    expect(fixture.nativeElement.querySelector('#field_redirectUris')).toBeNull()
  })

  it('should show and require redirectUris when authenticateOrcidIds is YES', () => {
    component.applyForm.controls.authenticateOrcidIds.setValue('YES')
    fixture.detectChanges()

    expect(internals(component).showRedirectUris()).toBeTrue()
    expect(component.applyForm.controls.redirectUris.hasError('required')).toBeTrue()
    expect(fixture.nativeElement.querySelector('#field_redirectUris')).not.toBeNull()
  })

  it('should not navigate and should mark all as touched when save() is called on an invalid form', () => {
    component.save()

    expect(router.navigate).not.toHaveBeenCalled()
    expect(component.applyForm.controls.displayName.touched).toBeTrue()
  })

  it('should broadcast a toast and navigate to the list when save() succeeds', () => {
    component.applyForm.setValue({
      integrationType: 'IN_HOUSE',
      authenticateOrcidIds: 'YES',
      displayName: 'My App',
      homepageUrl: 'https://example.org',
      description: 'A test integration',
      redirectUris: 'https://example.org/callback',
      notes: null,
    })

    component.save()

    expect(alertService.broadcast).toHaveBeenCalled()
    expect(router.navigate).toHaveBeenCalledWith(['/api-credentials', 'memberId'])
  })
})
