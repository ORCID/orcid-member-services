import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing'
import { FormsModule, ReactiveFormsModule } from '@angular/forms'
import { RouterModule } from '@angular/router'
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome'
import * as moment from 'moment';
import { EMPTY, of } from 'rxjs'
import { AccountService } from 'src/app/account'
import { HasAnyAuthorityDirective } from 'src/app/shared/directive/has-any-authority.directive'
import { LocalizePipe } from '../shared/pipe/localize'
import { AlertService } from '../shared/service/alert.service'
import { EventService } from '../shared/service/event.service'
import { AffiliationsComponent } from './affiliations.component'
import { Affiliation } from './model/affiliation.model'
import { AffiliationService } from './service/affiliation.service'

describe('AffiliationsComponent', () => {
  let component: AffiliationsComponent
  let fixture: ComponentFixture<AffiliationsComponent>
  let affiliationService: jasmine.SpyObj<AffiliationService>
  let accountService: jasmine.SpyObj<AccountService>
  let eventService: jasmine.SpyObj<EventService>
  const baseTime = new Date('2026-03-31T10:00:00Z');

  beforeEach(() => {
    jasmine.clock().install();
    jasmine.clock().mockDate(baseTime);

    const accountServiceSpy = jasmine.createSpyObj('AccountService', [
      'getAccountData',
      'isAuthenticated',
      'hasAnyAuthority',
      'isOrganizationOwner',
      'getImageUrl',
      'getSalesforceId',
    ])
    const affiliationServiceSpy = jasmine.createSpyObj('AffiliationService', [
      'query',
      'findBySalesForceId',
      'sendActivate',
    ])
    const eventServiceSpy = jasmine.createSpyObj('EventService', ['on', 'broadcast'])
    const alertServiceSpy = jasmine.createSpyObj('AlertService', ['on', 'broadcast'])

    TestBed.configureTestingModule({
      declarations: [AffiliationsComponent, HasAnyAuthorityDirective, LocalizePipe],
      imports: [
        ReactiveFormsModule,
        RouterModule.forRoot([{ path: 'affiliations', component: AffiliationsComponent }]),
        FormsModule,
        FontAwesomeModule,
      ],
      providers: [
        { provide: AffiliationService, useValue: affiliationServiceSpy },
        { provide: AccountService, useValue: accountServiceSpy },
        { provide: EventService, useValue: eventServiceSpy },
        { provide: AlertService, useValue: alertServiceSpy },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }).compileComponents()

    fixture = TestBed.createComponent(AffiliationsComponent)
    component = fixture.componentInstance
    affiliationService = TestBed.inject(AffiliationService) as jasmine.SpyObj<AffiliationService>
    accountService = TestBed.inject(AccountService) as jasmine.SpyObj<AccountService>
    eventService = TestBed.inject(EventService) as jasmine.SpyObj<EventService>

    affiliationService.query.and.returnValue(
      of({
        content: [new Affiliation('123')],
        page: {
          totalElements: 1,
          number: 0,
          size: 20,
          totalPages: 1,
        },
      })
    )

    accountService.getAccountData.and.returnValue(
      of({
        id: 'id',
        activated: true,
        authorities: ['ROLE_USER', 'ROLE_ADMIN'],
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

    accountService.hasAnyAuthority.and.returnValue(true)
    eventService.on.and.returnValue(EMPTY)
  })

  afterEach(() => {
    jasmine.clock().uninstall();
  });

  it('should create', () => {
    expect(component).toBeTruthy()
  })

  it('should call load all on init', fakeAsync(() => {
    component.ngOnInit()

    expect(affiliationService.query).toHaveBeenCalled()
    expect(component.affiliations![0]).toEqual(jasmine.objectContaining({ id: '123' }))
  }))

  it('should load a page', () => {
    component.page = 1
    component.loadPage()

    expect(affiliationService.query).toHaveBeenCalled()
    expect(component.affiliations![0]).toEqual(jasmine.objectContaining({ id: '123' }))
  })

  it('sort should be id,desc by default', () => {
    const result = component.sort()

    expect(result).toEqual(['id,desc'])
  })

  it('direction should be desc and id should be secondary sort column by default', () => {
    component.sortColumn = 'name'
    const result = component.sort()
    expect(result).toEqual(['name,desc', 'id'])
  })

  it('updating sort column to different value should maintain sort direction', () => {
    component.sortColumn = 'name'
    let result = component.sort()
    expect(result).toEqual(['name,desc', 'id'])

    component.updateSort('email')
    result = component.sort()
    expect(result).toEqual(['email,desc', 'id'])
  })

  it('updating sort column with same value should flip sort direction', () => {
    component.sortColumn = 'name'
    let result = component.sort()
    expect(result).toEqual(['name,desc', 'id'])

    component.updateSort('name')
    result = component.sort()
    expect(result).toEqual(['name,asc', 'id'])
  })

  it('clear should reset page to zero', () => {
    component.page = 10
    component.clear()
    expect(component.page).toEqual(0)
  })

  it('reset search should clear search term', () => {
    component.searchTerm = 'what the user typed'
    component.submittedSearchTerm = 'what the user typed'
    component.resetSearch()
    expect(component.searchTerm).toEqual('')
    expect(component.submittedSearchTerm).toEqual('')
  })

  it('should render affiliations list with email, org name, role title, and created date', fakeAsync(() => {
    // The service's convertDateFromServer wraps raw API strings in moment() before
    // handing data to the component. The mock must reflect that post-deserialization
    // state, otherwise the template's .toDate() call will throw at render time.
    const created = moment('2026-03-31T10:00:00Z');
    affiliationService.query.and.returnValue(
      of({
        content: [
          new Affiliation(
            '456',       // id
            null,        // addedToORCID
            undefined,   // affiliationSection
            created,     // created — Moment object, as returned by convertDateFromServer
            undefined,   // deletedFromORCID
            undefined,   // departmentName
            undefined,   // disambiguatedOrgId
            undefined,   // disambiguationSource
            'user@example.com', // email
            undefined, undefined, undefined, undefined, undefined, undefined,
            undefined,   // modified
            undefined, undefined,
            'Springfield', // orgCity
            undefined,
            'Acme Corp', // orgName
            undefined, undefined, undefined,
            'Software Engineer', // roleTitle
          ),
        ],
        page: { totalElements: 1, number: 0, size: 20, totalPages: 1 },
      })
    )

    component.ngOnInit()
    expect(() => fixture.detectChanges()).not.toThrow()

    const rows = fixture.nativeElement.querySelectorAll('tbody tr')
    expect(rows.length).toBe(1)

    const rowText = rows[0].textContent as string
    expect(rowText).toContain('user@example.com')
    expect(rowText).toContain('Acme Corp')
    expect(rowText).toContain('Springfield')
    expect(rowText).toContain('Software Engineer')
    expect(rowText).toContain('Mar')
  }))
})
