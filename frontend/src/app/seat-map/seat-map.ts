import {
  Component,
  OnInit,
  inject,
  signal
} from '@angular/core';

import { ActivatedRoute } from '@angular/router';

import {
  SeatService,
  SeatLockResponse,
  BookingResponse,
  PaymentResponse,
  RazorpayOrderResponse
} from '../services/seat.service';

import { Seat } from './seat.model';

@Component({
  selector: 'app-seat-map',
  standalone: true,
  imports: [],
  templateUrl: './seat-map.html',
  styleUrl: './seat-map.css'
})
export class SeatMap implements OnInit {

  private seatService = inject(SeatService);
  private route = inject(ActivatedRoute);

  // IDs received from Module 2 through URL query parameters
  showId = 0;
  screenId = 0;
  userId = 1;

  // Seat information
  seats = signal<Seat[]>([]);
  selectedSeat = signal<Seat | null>(null);
  lockToken = signal<string | null>(null);

  // Loading states
  isLocking = signal(false);
  isCheckingOut = signal(false);

  // Booking and payment
  booking = signal<BookingResponse | null>(null);
  payment = signal<PaymentResponse | null>(null);

  paymentOptionsOpen = signal(false);
  selectedPaymentMethod = signal<string | null>(null);

  // Payment methods
  paymentMethods = [
    { id: 'GOOGLE_PAY', name: 'Google Pay', icon: 'G' },
    { id: 'PHONEPE', name: 'PhonePe', icon: 'P' },
    { id: 'PAYTM', name: 'Paytm', icon: '₹' },
    { id: 'UPI', name: 'UPI', icon: '▦' },
    { id: 'CARD', name: 'Debit / Credit Card', icon: '▣' }
  ];

  message = signal('');

  // INITIALIZATION
  ngOnInit(): void {

    this.route.queryParamMap.subscribe(params => {

      this.showId = Number(params.get('showId'));
      this.screenId = Number(params.get('screenId'));

      this.userId = Number(params.get('userId')) || 1;

      if (this.showId <= 0 || this.screenId <= 0) {

        this.message.set('Invalid show or screen ID.');
        return;

      }

      this.loadSeats();

    });

  }

  // LOAD AVAILABLE SEATS
  loadSeats(): void {

    this.seatService.getAvailableSeats(this.screenId).subscribe({

      next: (data: Seat[]) => {

        this.seats.set(data);

        this.message.set(
          `Loaded ${data.length} seats successfully.`
        );

      },

      error: (error) => {

        console.error('Failed to load seats:', error);

        this.message.set(
          'Failed to load seats. Check backend connection.'
        );

      }

    });

  }

  // SELECT AND LOCK SEAT
  selectSeat(seat: Seat): void {

    if (
      seat.status !== 'AVAILABLE' ||
      this.isLocking() ||
      this.isCheckingOut()
    ) {
      return;
    }

    if (this.selectedSeat()) {

      this.message.set(
        'Release your current seat before selecting another.'
      );

      return;

    }

    this.isLocking.set(true);
    this.message.set('Locking seat...');

    this.seatService.lockSeat(
      this.showId,
      seat.seatId
    ).subscribe({

      next: (response: SeatLockResponse) => {

        this.isLocking.set(false);

        if (response.success && response.lockToken) {

          this.selectedSeat.set(seat);
          this.lockToken.set(response.lockToken);

          this.message.set(
            'Seat locked. You can now continue to payment.'
          );

        } else {

          this.message.set(
            response.message || 'Unable to lock seat.'
          );

        }

      },

      error: (error) => {

        this.isLocking.set(false);

        console.error('Seat lock failed:', error);

        this.message.set('Unable to lock seat.');

      }

    });

  }

  // OPEN PAYMENT OPTIONS
  openPaymentOptions(): void {

    if (!this.selectedSeat() || !this.lockToken()) {

      this.message.set('Select and lock a seat first.');

      return;

    }

    this.paymentOptionsOpen.set(true);
    this.selectedPaymentMethod.set(null);

    this.message.set('Choose your payment method.');

  }

  // SELECT PAYMENT METHOD
  choosePaymentMethod(method: string): void {

    this.selectedPaymentMethod.set(method);

    this.message.set(
      `${method.replace('_', ' ')} selected.`
    );

  }

  // CLOSE PAYMENT OPTIONS
  closePaymentOptions(): void {

    this.paymentOptionsOpen.set(false);
    this.selectedPaymentMethod.set(null);

  }

  // CHECKOUT
  checkout(): void {

    const method = this.selectedPaymentMethod();

    if (!method) {

      this.message.set(
        'Please select your payment method.'
      );

      return;

    }

    if (this.isCheckingOut()) {
      return;
    }

    this.paymentOptionsOpen.set(false);

    this.processBookingAndPayment();

  }

  // CREATE BOOKING AND PAYMENT ORDER
  private processBookingAndPayment(): void {

    const seat = this.selectedSeat();
    const token = this.lockToken();
    const method = this.selectedPaymentMethod();

    if (!seat || !token || !method) {

      this.message.set(
        'Seat lock or payment method is missing.'
      );

      return;

    }

    this.isCheckingOut.set(true);
    this.message.set('Creating booking...');

    // STEP 1: CREATE BOOKING
    this.seatService.createBooking(
      this.userId,
      this.showId,
      seat.seatId,
      token
    ).subscribe({

      next: (booking: BookingResponse) => {

        this.booking.set(booking);

        // Backend releases Redis lock after booking creation.
        this.lockToken.set(null);

        this.message.set(
          'Creating secure Razorpay order...'
        );

        // STEP 2: CREATE RAZORPAY ORDER
        this.seatService.createRazorpayOrder(
          booking.bookingId,
          method
        ).subscribe({

          next: (order: RazorpayOrderResponse) => {

            this.openRazorpayCheckout(order);

          },

          error: (error) => {

            console.error(
              'Razorpay order creation failed:',
              error
            );

            this.isCheckingOut.set(false);

            this.message.set(
              error.error?.message ||
              'Unable to create payment order.'
            );

          }

        });

      },

      error: (error) => {

        this.isCheckingOut.set(false);

        console.error(
          'Booking creation failed:',
          error
        );

        this.message.set(
          error.error?.message ||
          'Unable to create booking.'
        );

      }

    });

  }

  // OPEN RAZORPAY CHECKOUT
  private openRazorpayCheckout(
    order: RazorpayOrderResponse
  ): void {

    if (typeof Razorpay === 'undefined') {

      this.isCheckingOut.set(false);

      this.message.set(
        'Razorpay Checkout script is not loaded.'
      );

      return;

    }

    const options = {

      key: order.keyId,

      amount: order.amount,

      currency: order.currency,

      name: 'Smart Ticket Booking',

      description: 'Movie ticket booking',

      order_id: order.orderId,

      handler: (response: {
        razorpay_payment_id: string;
        razorpay_order_id: string;
        razorpay_signature: string;
      }) => {

        this.message.set(
          'Verifying payment securely...'
        );

        // STEP 3: VERIFY PAYMENT
        this.seatService.verifyRazorpayPayment({

          bookingId: order.bookingId,

          razorpayOrderId:
            response.razorpay_order_id,

          razorpayPaymentId:
            response.razorpay_payment_id,

          razorpaySignature:
            response.razorpay_signature

        }).subscribe({

          next: (payment: PaymentResponse) => {

            this.payment.set(payment);

            if (payment.status === 'SUCCESS') {

              this.refreshBooking(order.bookingId);

            } else {

              this.isCheckingOut.set(false);

              this.message.set(
                'Payment has not been confirmed.'
              );

            }

          },

          error: (error) => {

            console.error(
              'Payment verification failed:',
              error
            );

            this.isCheckingOut.set(false);

            this.message.set(
              'Payment verification failed. Please check your booking status.'
            );

          }

        });

      },

      modal: {

        ondismiss: () => {

          this.isCheckingOut.set(false);

          this.message.set(
            'Payment window closed. Check booking status before retrying.'
          );

        }

      },

      theme: {
        color: '#7c3aed'
      }

    };

    try {

      const razorpay = new Razorpay(options);

      razorpay.open();

    } catch (error) {

      console.error(
        'Unable to open Razorpay Checkout:',
        error
      );

      this.isCheckingOut.set(false);

      this.message.set(
        'Unable to open payment window.'
      );

    }

  }

  // REFRESH BOOKING AFTER PAYMENT
  private refreshBooking(bookingId: number): void {

    this.seatService.getBooking(bookingId).subscribe({

      next: (updatedBooking: BookingResponse) => {

        this.booking.set(updatedBooking);

        this.isCheckingOut.set(false);

        if (updatedBooking.status === 'CONFIRMED') {

          this.message.set(
            'Payment successful! Booking confirmed.'
          );

        } else {

          this.message.set(
            `Payment received, but booking status is ${updatedBooking.status}.`
          );

        }

        this.loadSeats();

      },

      error: (error) => {

        this.isCheckingOut.set(false);

        console.error(
          'Failed to refresh booking:',
          error
        );

        this.message.set(
          'Payment verified, but booking details could not be refreshed.'
        );

      }

    });

  }

  // RELEASE SEAT
  releaseSeat(): void {

    const seat = this.selectedSeat();
    const token = this.lockToken();

    if (
      !seat ||
      !token ||
      this.isCheckingOut()
    ) {
      return;
    }

    this.seatService.unlockSeat(
      this.showId,
      seat.seatId,
      token
    ).subscribe({

      next: (response: SeatLockResponse) => {

        if (response.success) {

          this.selectedSeat.set(null);
          this.lockToken.set(null);

          this.message.set(
            'Seat released successfully.'
          );

          this.loadSeats();

        } else {

          this.message.set(response.message);

        }

      },

      error: (error) => {

        console.error(
          'Seat unlock failed:',
          error
        );

        this.message.set(
          'Unable to release seat.'
        );

      }

    });

  }

  // START ANOTHER BOOKING
  startAnotherBooking(): void {

    this.selectedSeat.set(null);
    this.lockToken.set(null);

    this.booking.set(null);
    this.payment.set(null);

    this.paymentOptionsOpen.set(false);

    this.selectedPaymentMethod.set(null);

    this.message.set(
      'Select a seat to begin another booking.'
    );

    this.loadSeats();

  }

}