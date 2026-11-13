import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.css'
})
export class AdminDashboard implements OnInit {

  moviesCount = 0;
  theatersCount = 0;
  screensCount = 0;
  showsCount = 0;
  bookingsCount = 0;
  usersCount = 0;

  loading = true;
  errorMessage = '';

  private apiUrl = 'http://localhost:8081/api';

  constructor(
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {

    if (sessionStorage.getItem('adminLoggedIn') !== 'true') {
      this.router.navigate(['/admin-login']);
      return;
    }

    this.loadAdminStats();
  }

  loadAdminStats(): void {

    this.loading = true;

    forkJoin({
      movies: this.http.get<any[]>(`${this.apiUrl}/movies`).pipe(
        catchError(() => of([]))
      ),

      theaters: this.http.get<any[]>(`${this.apiUrl}/theaters`).pipe(
        catchError(() => of([]))
      ),

      screens: this.http.get<any[]>(`${this.apiUrl}/screens`).pipe(
        catchError(() => of([]))
      ),

      shows: this.http.get<any[]>(`${this.apiUrl}/shows`).pipe(
        catchError(() => of([]))
      )
    }).subscribe({

      next: (data) => {
        this.moviesCount = data.movies.length;
        this.theatersCount = data.theaters.length;
        this.screensCount = data.screens.length;
        this.showsCount = data.shows.length;

        this.loading = false;
      },

      error: () => {
        this.errorMessage = 'Unable to load admin statistics.';
        this.loading = false;
      }

    });
  }

  openManagement(): void {
    this.router.navigate(['/admin']);
  }
  openBookings(): void {
  this.router.navigate(['/admin-bookings']);
}
  logout(): void {
  sessionStorage.removeItem('adminLoggedIn');
  this.router.navigateByUrl('/');
}
}