import { ComponentFixture, TestBed } from '@angular/core/testing'

import { ActivatedRoute } from '@angular/router'
import { RouterTestingModule } from '@angular/router/testing'
import { of } from 'rxjs'
import { MemberDetailComponent } from './member-detail.component'
import { IMember } from './model/member.model'

describe('MemberDetailComponent', () => {
  let component: MemberDetailComponent
  let fixture: ComponentFixture<MemberDetailComponent>
  let activatedRoute: jasmine.SpyObj<ActivatedRoute>

  const defaultMember: IMember = { id: 'default', clientId: '', clientName: '', type: 'unset', status: 'unset' } as IMember

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [MemberDetailComponent],
      imports: [RouterTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { data: of({ member: defaultMember }) } },
      ],
    })
    fixture = TestBed.createComponent(MemberDetailComponent)
    component = fixture.componentInstance
    activatedRoute = TestBed.inject(ActivatedRoute) as jasmine.SpyObj<ActivatedRoute>
    fixture.detectChanges()
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
