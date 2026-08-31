import {
  bootstrapApplication
} from '@angular/platform-browser';

import {
  provideHttpClient,
  withInterceptors
} from '@angular/common/http';

import {
  provideRouter
} from '@angular/router';

import {
  AppComponent
} from './app/app.component';

import {
  APP_ROUTES
} from './app/app.routes';

import {
  authInterceptor
} from './app/services/auth.service';


bootstrapApplication(
  AppComponent,
  {
    providers: [

      provideRouter(
        APP_ROUTES
      ),

      provideHttpClient(
        withInterceptors([
          authInterceptor
        ])
      )

    ]
  }
)
.catch(
  console.error
);