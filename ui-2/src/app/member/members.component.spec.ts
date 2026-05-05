import { ComponentFixture, TestBed, tick } from '@angular/core/testing'

import { MembersComponent } from './members.component'
import { provideHttpClientTesting } from '@angular/common/http/testing'
import { AccountService } from '../account'
import { RouterTestingModule } from '@angular/router/testing'
import { MemberService } from './service/member.service'
import { of } from 'rxjs'
import { Member } from './model/member.model'
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/compiler'
import { Router } from '@angular/router'
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('MembersComponent', () => {
  let component: MembersComponent
  let fixture: ComponentFixture<MembersComponent>
  let accountServiceSpy: jasmine.SpyObj<AccountService>
  let memberServiceSpy: jasmine.SpyObj<MemberService>
  let routerSpy: jasmine.SpyObj<Router>

  beforeEach(() => {
    accountServiceSpy = jasmine.createSpyObj('AccountService', ['getAccountData'])
    memberServiceSpy = jasmine.createSpyObj('MemberService', ['query'])
    routerSpy = jasmine.createSpyObj('Router', ['navigate'])

    TestBed.configureTestingModule({
    declarations: [MembersComponent],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [RouterTestingModule],
    providers: [
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: MemberService, useValue: memberServiceSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
    ]
})

    fixture = TestBed.createComponent(MembersComponent)
    component = fixture.componentInstance

    accountServiceSpy = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>
    memberServiceSpy = TestBed.inject(MemberService) as jasmine.SpyObj<MemberService>
    routerSpy = TestBed.inject(Router) as jasmine.SpyObj<Router>

    memberServiceSpy.query.and.returnValue(
      of({
        content: [new Member('id')],
        page: {
          totalElements: 1,
          number: 0,
          size: 20,
          totalPages: 1,
        },
      })
    )

    accountServiceSpy.getAccountData.and.returnValue(
      of({
        id: 'id',
        activated: true,
        authorities: ['ROLE_USER'],
        email: 'email@email.com',
        firstName: 'name',
        langKey: 'en',
        lastName: 'surname',
        imageUrl: 'url',
        salesforceId: 'sfid',
        loggedAs: false,
        loginAs: 'sfid',
        mainContact: false,
        mfaEnabled: false,
      })
    )
    spyOn(routerSpy, 'navigate').and.returnValue(Promise.resolve(true))
  })

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('load all should call member service query', () => {
    component.loadAll()
    expect(memberServiceSpy.query).toHaveBeenCalled()
  })

  it('clear should navigate with router and call member service query', () => {
    component.clear()
    expect(component.page).toEqual(0)
    expect(routerSpy.navigate).toHaveBeenCalledWith([
      '/members',
      Object({ page: 0, sort: 'undefined,desc', filter: '' }),
    ])
    expect(memberServiceSpy.query).toHaveBeenCalled()
  })

  it('resetSearch should reset search fields', () => {
    component.page = 50
    component.searchTerm = 'test'
    component.submittedSearchTerm = 'testing'

    component.resetSearch()

    expect(component.page).toEqual(1)
    expect(component.searchTerm).toEqual('')
    expect(component.submittedSearchTerm).toEqual('')
  })

  it('submitSearch should set submittedSearchTerm to value of searchTerm and then call member service query', () => {
    component.page = 50
    component.searchTerm = 'new search'
    component.submittedSearchTerm = 'previously submitted search'

    component.submitSearch()

    expect(component.page).toEqual(1)
    expect(component.searchTerm).toEqual('new search')
    expect(component.submittedSearchTerm).toEqual('new search')

    expect(memberServiceSpy.query).toHaveBeenCalled()
  })

  it('updateSort should set sortColumn and then call member service query', () => {
    component.sortColumn = 'some sort column'

    component.updateSort('different column')

    expect(component.sortColumn).toEqual('different column')
    expect(memberServiceSpy.query).toHaveBeenCalled()
  })
})
