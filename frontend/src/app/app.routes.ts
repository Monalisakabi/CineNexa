import { Routes } from '@angular/router';
import { SeatMap } from './seat-map/seat-map';

export const routes: Routes = [
  {
    path: 'seat-map',
    component: SeatMap
  },
  {
    path: '',
    redirectTo: 'seat-map',
    pathMatch: 'full'
  }
];