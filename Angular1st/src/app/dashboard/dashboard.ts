import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';

interface Booking {
  bookingId: number;
  userId: number;
  showId: number;
  seatIds: number[];
  seatNumbers: string[];
  totalAmount: number;
  status: string;
  createdAt: string;
  movieTitle: string;
  moviePoster: string;
  showDate: string;
  showTime: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink,FormsModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {

  bookings: Booking[] = [];
  loading = true;
  errorMessage = '';
isEditingProfile = false;
profileMessage = '';
profileError = '';
  userName = '';
  userEmail = '';
  userPhone = '';
  activeSection = 'overview';

  private apiUrl = 'http://localhost:8081/api';

  constructor(
    private http: HttpClient,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {

      const params = new URLSearchParams(window.location.search);
const transferredUserId = params.get('userId');

if (transferredUserId) {
  sessionStorage.setItem('userId', transferredUserId);
}

const userId = sessionStorage.getItem('userId');

if (!userId) {
  this.router.navigate(['/login']);
  return;
}
      this.userName = sessionStorage.getItem('userName') || 'Customer';
      this.userEmail = sessionStorage.getItem('userEmail') || '';
      this.userPhone = sessionStorage.getItem('userPhone') || '';
      
      this.loadBookings();
    }
  }

  loadBookings(): void {

    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const userId = sessionStorage.getItem('userId');

    if (!userId) {
      this.router.navigate(['/login']);
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.http.get<Booking[]>(
      `${this.apiUrl}/bookings/user/${userId}`
    ).subscribe({

      next: (data) => {
        this.bookings = data;
        this.loading = false;
      },

      error: (error) => {
        console.error('Dashboard booking error:', error);
        this.errorMessage = 'Unable to load your bookings.';
        this.loading = false;
      }

    });
  }

  get confirmedCount(): number {
    return this.bookings.filter(
      b => b.status === 'CONFIRMED'
    ).length;
  }

  get cancelledCount(): number {
    return this.bookings.filter(
      b => b.status === 'CANCELLED'
    ).length;
  }

  get expiredCount(): number {
    return this.bookings.filter(
      b => b.status === 'EXPIRED'
    ).length;
  }

  get upcomingCount(): number {
    const today = new Date();

    return this.bookings.filter(b =>
      b.status === 'CONFIRMED' &&
      new Date(b.showDate) >= today
    ).length;
  }

  setActiveSection(section: string): void {
    this.activeSection = section;
  }

  cancelBooking(bookingId: number): void {

  const confirmed = window.confirm(
    'Are you sure you want to cancel this booking? Cancellation charges will apply.'
  );

  if (!confirmed) {
    return;
  }

  this.http.post<any>(
    `${this.apiUrl}/bookings/${bookingId}/cancel`,
    {}
  ).subscribe({

    next: (response) => {
      alert(
        `Booking cancelled successfully!\n` +
        `Cancellation penalty: ₹${response.cancellationPenalty}\n` +
        `Refund amount: ₹${response.refundAmount}`
      );

      this.loadBookings();
    },

    error: (error) => {
      console.error('Booking cancellation error:', error);

      alert(
        error.error?.message ||
        error.error ||
        'Unable to cancel booking.'
      );
    }

  });
}
 logout(): void {
  sessionStorage.removeItem('userId');
  sessionStorage.removeItem('userName');
  sessionStorage.removeItem('userEmail');
  sessionStorage.removeItem('userPhone');

  this.router.navigate(['/']);
}
  updateProfile(): void {

  const userId = sessionStorage.getItem('userId');

  if (!userId) {
    this.router.navigate(['/login']);
    return;
  }

  const profileData = {
    name: this.userName,
    phone: this.userPhone
  };

  this.http.put<any>(
    `${this.apiUrl}/auth/profile/${userId}`,
    profileData
  ).subscribe({

    next: (response) => {

      this.userName = response.name;
      this.userPhone = response.phone || '';

      sessionStorage.setItem('userName', response.name);
      sessionStorage.setItem('userPhone', response.phone || '');

      this.profileMessage = 'Profile updated successfully!';
      this.profileError = '';
      this.isEditingProfile = false;
    },

    error: (error) => {
      console.error('Profile update error:', error);

      this.profileError =
        error.error || 'Unable to update profile.';
      this.profileMessage = '';
    }

  });
}
}