import { Routes } from '@angular/router';

import {
  LandingComponent
} from './components/landing/landing.component';

import {
  AuthComponent
} from './components/auth/auth.component';

import {
  DashboardComponent
} from './components/dashboard/dashboard.component';


export const APP_ROUTES: Routes = [

  /*
   * Landing page
   */
  {
    path: '',
    component: LandingComponent
  },


  /*
   * Login / Register
   */
  {
    path: 'auth',
    component: AuthComponent
  },


  /*
   * Dashboard
   */
  {
    path: 'dashboard',
    component: DashboardComponent
  },


  /*
   * Unknown URL
   */
  {
    path: '**',
    redirectTo: ''
  }

];