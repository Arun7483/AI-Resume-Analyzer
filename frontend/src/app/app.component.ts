import { Component, inject, effect } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterModule],
  template: `<router-outlet />`
})
export class AppComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  constructor() {
    effect(() => {
      if (this.auth.isAuthenticated()) {
        this.router.navigate(['/dashboard'], { replaceUrl: true });
      } else {
        this.router.navigate(['/'], { replaceUrl: true });
      }
    });
  }
}
