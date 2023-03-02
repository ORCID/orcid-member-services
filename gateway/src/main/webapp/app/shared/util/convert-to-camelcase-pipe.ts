import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'camelcase' })
export class ConvertToCamelCasePipe implements PipeTransform {
  transform(str: string): string {
    var out = '';
    str.split(' ').forEach(function(el, idx) {
      var add = el.toLowerCase();
      out += idx === 0 ? add : add[0].toUpperCase() + add.slice(1);
    });
    return out;
  }
}
