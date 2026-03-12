import { TestBed } from '@angular/core/testing'
import { LanguageService } from './language.service'
import { WindowLocationService } from './window-location.service'

describe('LanguageService', () => {
  let service: LanguageService
  let languageService: LanguageService
  let windowLocationService: jasmine.SpyObj<WindowLocationService>

  beforeEach(() => {
    const windowLocationServiceSpy = jasmine.createSpyObj('WindowLocationService', [
      'getWindowLocationOrigin',
      'getWindowLocationPathname',
      'getWindowLocationHref',
      'updateWindowLocation',
    ])
    languageService = new LanguageService(windowLocationServiceSpy)
    TestBed.configureTestingModule({
      providers: [
        { provide: WindowLocationService, useValue: windowLocationServiceSpy },
        { provide: languageService, useValue: WindowLocationService },
      ],
    })
    windowLocationService = TestBed.inject(WindowLocationService) as jasmine.SpyObj<WindowLocationService>
    service = TestBed.inject(LanguageService)
  })

  it('should be created', () => {
    expect(service).toBeTruthy()
  })

  it('should add locale when missing', () => {
    windowLocationService.getWindowLocationHref.and.returnValue('http://example.com/test-path')
    windowLocationService.getWindowLocationOrigin.and.returnValue('http://example.com')
    windowLocationService.getWindowLocationPathname.and.returnValue('/test-path')

    languageService.updateLanguageCodeInUrl('en')
    expect(windowLocationService.updateWindowLocation).toHaveBeenCalledWith('http://example.com/en/test-path')
  })

  it('should replace locale if different one is present', () => {
    windowLocationService.getWindowLocationHref.and.returnValue('http://example.com/es/test-path/')
    windowLocationService.getWindowLocationOrigin.and.returnValue('http://example.com')
    windowLocationService.getWindowLocationPathname.and.returnValue('/es/test-path')

    languageService.updateLanguageCodeInUrl('en')
    expect(windowLocationService.updateWindowLocation).toHaveBeenCalledWith('http://example.com/en/test-path')
  })

  it('url should remain the same if locale is matched', () => {
    windowLocationService.getWindowLocationHref.and.returnValue('http://example.com/fr/test-path/')
    windowLocationService.getWindowLocationOrigin.and.returnValue('http://example.com')
    windowLocationService.getWindowLocationPathname.and.returnValue('/fr/test-path')

    languageService.updateLanguageCodeInUrl('fr')
    expect(windowLocationService.updateWindowLocation).toHaveBeenCalledTimes(0)
  })

  it('url should remain in localhost environment', () => {
    windowLocationService.getWindowLocationHref.and.returnValue('http://localhost:4200/fr/test-path/')
    windowLocationService.getWindowLocationOrigin.and.returnValue('http://localhost:4200')
    windowLocationService.getWindowLocationPathname.and.returnValue('/test-path')

    languageService.updateLanguageCodeInUrl('fr')
    expect(windowLocationService.updateWindowLocation).toHaveBeenCalledTimes(0)
  })
})
