import { ComponentFixture, TestBed } from '@angular/core/testing'

import { MemberDetailComponent } from './member-detail.component'
import { ActivatedRoute } from '@angular/router'
import { RouterTestingModule } from '@angular/router/testing'
import { of } from 'rxjs'
import { IMember } from './model/member.model'

describe('MemberDetailComponent', () => {
  let component: MemberDetailComponent
  let fixture: ComponentFixture<MemberDetailComponent>
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [MemberDetailComponent],
      imports: [RouterTestingModule],
    })
    fixture = TestBed.createComponent(MemberDetailComponent)
    component = fixture.componentInstance
    fixture.detectChanges()

    activatedRoute = TestBed.inject(ActivatedRoute) as jasmine.SpyObj<ActivatedRoute>
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should set member provided by activated route', () => {
    activatedRoute.data = of({
      member: {
        id: 'id',
        clientId: 'client-id',
        clientName: 'client-name',
      } as IMember,
    })

    component.ngOnInit()

    expect(component.member).toBeTruthy()
    expect(component.member!.id).toEqual('id')
    expect(component.member!.clientId).toEqual('client-id')
    expect(component.member!.clientName).toEqual('client-name')
  })
})
