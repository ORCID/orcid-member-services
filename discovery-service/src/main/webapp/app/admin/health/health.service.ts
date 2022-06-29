import { Injectable } from '@angular/core';
import { HttpClient, HttpParameterCodec } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { Route } from 'app/shared/routes/route.model';

export type HealthStatus = 'UP' | 'DOWN' | 'UNKNOWN' | 'OUT_OF_SERVICE';

export type HealthKey =
  | 'binders'
  | 'discoveryComposite'
  | 'refreshScope'
  | 'clientConfigServer'
  | 'hystrix'
  | 'consul'
  | 'diskSpace'
  | 'mail'
  | 'elasticsearch'
  | 'db'
  | 'mongo'
  | 'cassandra'
  | 'couchbase';

export interface Health {
  status: HealthStatus;
  components: {
    [key in HealthKey]?: HealthDetails;
  };
}

export interface HealthDetails {
  status: HealthStatus;
  details: any;
}

@Injectable({ providedIn: 'root' })
export class HealthService {
  separator: string;

  constructor(private http: HttpClient, private httpParamCodec: HttpParameterCodec) {
    this.separator = '.';
  }

  checkHealth(): Observable<Health> {
    return this.http.get<Health>(SERVER_API_URL + 'management/health');
  }

  // get the instance's health
  checkInstanceHealth(instance: Route | undefined): Observable<Health> {
    if (instance && instance.prefix && instance.prefix.length > 0) {
      const url = SERVER_API_URL + instance.prefix + '/management/health';
      const encodedUrl = this.httpParamCodec.encodeValue(url);
      return this.http.get<Health>(SERVER_API_URL + '/health/check/' + encodedUrl);
    }
    return this.checkHealth();
  }
}
