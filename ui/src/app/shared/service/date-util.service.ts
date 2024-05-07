import { Injectable } from '@angular/core'
import * as moment from 'moment'
import { DEFAULT_EARLIEST_YEAR } from '../constants/orcid-api.constants'

@Injectable({ providedIn: 'root' })
export class DateUtilService {
  getCurrentMonthNumber(): number {
    return +moment().format('M')
  }

  getCurrentYear(): number {
    return moment().year()
  }

  getYearsList(futureYears?: number) {
    if (!futureYears) {
      futureYears = 0
    }
    const years = []
    const dateStart = moment(DEFAULT_EARLIEST_YEAR, 'YYYY')
    const dateEnd = moment().add(futureYears - 1, 'y')

    while (dateEnd.diff(dateStart, 'years') >= 0) {
      years.push(dateStart.format('YYYY'))
      dateStart.add(1, 'year')
    }
    return years.reverse()
  }

  getFutureYearsIncludingCurrent(futureYears: number) {
    const years = []
    const yearStart: number = moment().year()

    for (let i = 0; i < futureYears + 1; i++) {
      years.push(yearStart + i)
    }
    return years
  }

  getMonthsList(): [string, string][] {
    const months = moment.months()
    const monthsArray = []
    for (let _i = 1; _i <= months.length; _i++) {
      const res: [string, string] = [('0' + _i.toString()).slice(-2), months[_i - 1]]
      monthsArray.push(res)
    }
    return monthsArray
  }

  getFutureMonthsList(): [string, string][] {
    const months = moment.months()
    const monthsArray = []
    for (let _i = 1; _i <= months.length; _i++) {
      if (_i > this.getCurrentMonthNumber()) {
        const res: [string, string] = [('0' + _i.toString()).slice(-2), months[_i - 1]]
        monthsArray.push(res)
      }
    }
    return monthsArray
  }

  getDaysList(year?: string, month?: string) {
    let daysInMonth
    if (year && month) {
      daysInMonth = moment(year + '-' + month, 'YYYY-MM').daysInMonth()
    } else {
      daysInMonth = moment().daysInMonth()
    }
    const days = []
    for (let _i = 1; _i <= daysInMonth; _i++) {
      days.push(('0' + _i.toString()).slice(-2))
    }
    return days
  }

  formatDate({ year, month, day }: { year?: string; month?: string; day?: string }): string {
    let date = ''
    if (year) {
      date = year

      if (month && parseInt(month) > 0) {
        date += `-${month.padStart(2, '0')}`

        if (day && parseInt(day) > 0) {
          date += `-${day.padStart(2, '0')}`
        }
      }
    }
    return date
  }
}
