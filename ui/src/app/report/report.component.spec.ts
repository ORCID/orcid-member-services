import { ComponentFixture, TestBed } from '@angular/core/testing'
import { ReportComponent } from './report.component'
import { ReportService } from './report.service'
import { ActivatedRoute } from '@angular/router'
import { of } from 'rxjs'
import { RouterTestingModule } from '@angular/router/testing'

describe('ReportComponent', () => {
  let component: ReportComponent
  let fixture: ComponentFixture<ReportComponent>
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>
  let reportService: jasmine.SpyObj<ReportService>

  beforeEach(async () => {
    const reportServiceSpy = jasmine.createSpyObj('ReportService', ['getDashboardInfo'])

    await TestBed.configureTestingModule({
      declarations: [ReportComponent],
      imports: [RouterTestingModule],
      providers: [{ provide: ReportService, useValue: reportServiceSpy }],
    }).compileComponents()

    activatedRoute = TestBed.inject(ActivatedRoute) as jasmine.SpyObj<ActivatedRoute>
    reportService = TestBed.inject(ReportService) as jasmine.SpyObj<ReportService>
    fixture = TestBed.createComponent(ReportComponent)
    component = fixture.componentInstance
  })

  it('should create the component', () => {
    expect(component).toBeTruthy()
  })

  it('should call reportService to get report data', () => {
    activatedRoute.data = of({ reportType: 'test' })
    reportService.getDashboardInfo.and.returnValue(of({ url: 'url', jwt: 'jwt' }))
    fixture.detectChanges()

    expect(reportService.getDashboardInfo).toHaveBeenCalled()
  })
})
