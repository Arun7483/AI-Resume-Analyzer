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
import { authGuard } from './auth.guard';


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
    component: DashboardComponent,
    canActivate: [authGuard]
  },
  { path: 'jobs', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'profile', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'resumes', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'chat', component: DashboardComponent, canActivate: [authGuard] },


  /*
   * Unknown URL
   */
  {
    path: '**',
    redirectTo: ''
  }

];
