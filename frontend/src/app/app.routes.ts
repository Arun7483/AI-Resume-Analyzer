import { Routes } from '@angular/router';
import { LandingComponent } from './components/landing/landing.component';
import { AuthComponent } from './components/auth/auth.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';

export const APP_ROUTES: Routes = [
  {
    path: '',
    component: LandingComponent
  },
  {
    path: 'auth',
    component: AuthComponent
  },
  {
    path: 'dashboard',
    component: DashboardComponent
  },
  {
    path: '**',
    redirectTo: ''
  }
];
