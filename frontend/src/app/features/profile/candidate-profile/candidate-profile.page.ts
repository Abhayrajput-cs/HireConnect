import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, finalize, map, of } from 'rxjs';

import { CandidateProfileRequest, ProfileResponse } from '../../../core/models/profile.models';
import { ProfileService } from '../../../core/services/profile.service';
import { SessionService } from '../../../core/services/session.service';
import { ToastService } from '../../../core/services/toast.service';
import { ViewerProfileService } from '../../../core/services/viewer-profile.service';
import { getErrorMessage } from '../../../core/utils/http-error.util';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';

type AddressForm = FormGroup;

@Component({
  selector: 'app-candidate-profile-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  template: `
    <section class="page-section">
      <app-page-header
        eyebrow="Candidate profile"
        title="Shape the profile recruiters will see"
        description="Keep your resume link, skills, and contact details in sync with the backend profile service."
      />

      @if (existingProfile() && !editing()) {
        <section class="card-shell content-card stack profile-summary-card">
          <div class="profile-card-hero">
            <div class="profile-card-hero__identity">
              <span class="profile-card-hero__icon material-symbols-rounded">person</span>
              <div>
                <span class="eyebrow">Candidate profile</span>
                <h2>{{ existingProfile()!.fullName }}</h2>
                <p>Your public candidate profile that recruiters review before they shortlist and schedule interviews.</p>
              </div>
            </div>
            <div class="profile-card-hero__actions">
              <span class="profile-chip">Candidate</span>
              <button type="button" class="primary-button" (click)="startEditing()">Update profile</button>
            </div>
          </div>

          <section class="profile-summary-grid">
            <article class="card-shell profile-section-card">
              <div class="profile-section-card__header">
                <span class="material-symbols-rounded">badge</span>
                <div>
                  <h3>Personal details</h3>
                  <p>Core identity and contact details synced from profile-service.</p>
                </div>
              </div>

              <div class="profile-detail-grid">
                <div class="profile-detail-item">
                  <label>Email</label>
                  <strong>{{ existingProfile()!.email }}</strong>
                </div>
                <div class="profile-detail-item">
                  <label>Mobile</label>
                  <strong>{{ existingProfile()!.mobile || 'Not added' }}</strong>
                </div>
                <div class="profile-detail-item">
                  <label>Date of birth</label>
                  <strong>{{ existingProfile()!.dob || 'Not added' }}</strong>
                </div>
                <div class="profile-detail-item">
                  <label>Gender</label>
                  <strong>{{ existingProfile()!.gender || 'Not added' }}</strong>
                </div>
                <div class="profile-detail-item">
                  <label>Experience</label>
                  <strong>{{ existingProfile()!.experience || 0 }} years</strong>
                </div>
              </div>
            </article>

            <article class="card-shell profile-section-card">
              <div class="profile-section-card__header">
                <span class="material-symbols-rounded">description</span>
                <div>
                  <h3>Resume and skills</h3>
                  <p>Surface the strongest signals recruiters use to evaluate fit.</p>
                </div>
              </div>

              <div class="profile-link-card">
                <span class="material-symbols-rounded">link</span>
                <div>
                  <label>Resume URL</label>
                  @if (existingProfile()!.resumeUrl) {
                    <a [href]="existingProfile()!.resumeUrl!" target="_blank" rel="noreferrer">{{ existingProfile()!.resumeUrl }}</a>
                  } @else {
                    <strong>Not added</strong>
                  }
                </div>
              </div>

              <div class="field-block">
                <label>Skills</label>
                <div class="chip-group">
                  @for (skill of existingProfile()!.skills; track skill) {
                    <span class="chip">{{ skill }}</span>
                  }
                </div>
              </div>
            </article>
          </section>

          <section class="stack">
            <div class="profile-section-card__header">
              <span class="material-symbols-rounded">home_work</span>
              <div>
                <h3>Delivery and contact locations</h3>
                <p>Addresses used across your candidate profile.</p>
              </div>
            </div>

            <div class="panel-grid profile-address-grid">
              @for (address of existingProfile()!.addresses; track address.addressId) {
                <article class="card-shell profile-address-card">
                  <span class="material-symbols-rounded">pin_drop</span>
                  <strong>{{ address.houseNo }}</strong>
                  <p>{{ address.street }}</p>
                  <p>{{ address.city }}, {{ address.state }}</p>
                  <p>{{ address.pincode }}</p>
                </article>
              }
            </div>
          </section>
        </section>
      } @else {
        <section class="card-shell content-card">
          <div class="page-header">
            <div>
              <span class="eyebrow">{{ existingProfile() ? 'Update mode' : 'Create mode' }}</span>
              <h2>{{ existingProfile() ? 'Update your candidate profile' : 'Create your candidate profile' }}</h2>
              <p>These fields map directly to the profile-service request structure.</p>
            </div>
            @if (existingProfile()) {
              <button type="button" class="ghost-button" (click)="cancelEditing()">Cancel</button>
            }
          </div>

          <form class="stack" [formGroup]="form" (ngSubmit)="submit()">
            <div class="form-grid form-grid--wide">
              <div class="field-block">
                <label for="fullName">Full name</label>
                <input id="fullName" formControlName="fullName" placeholder="Your full name" />
                @if (showError('fullName')) {
                  <small>Full name is required.</small>
                }
              </div>
              <div class="field-block">
                <label for="email">Email</label>
                <input id="email" formControlName="email" readonly />
              </div>
              <div class="field-block">
                <label for="mobile">Mobile</label>
                <input id="mobile" type="number" formControlName="mobile" placeholder="9876543210" />
                @if (showError('mobile')) {
                  <small>Mobile must be a positive number.</small>
                }
              </div>
              <div class="field-block">
                <label for="dob">Date of birth</label>
                <input id="dob" type="date" formControlName="dob" />
              </div>
              <div class="field-block">
                <label for="gender">Gender</label>
                <select id="gender" formControlName="gender">
                  <option value="">Select</option>
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              <div class="field-block">
                <label for="experience">Experience in years</label>
                <input id="experience" type="number" formControlName="experience" />
                @if (showError('experience')) {
                  <small>Experience must be at least 1 year.</small>
                }
              </div>
            </div>

            <div class="field-block">
              <label for="resumeUrl">Resume URL</label>
              <input id="resumeUrl" formControlName="resumeUrl" placeholder="https://..." />
              @if (showError('resumeUrl')) {
                <small>Resume URL is required.</small>
              }
            </div>

            <div class="field-block">
              <label for="skills">Skills</label>
              <textarea id="skills" rows="3" formControlName="skillsText" placeholder="Java, Spring Boot, MySQL"></textarea>
              @if (showError('skillsText')) {
                <small>At least one skill is required.</small>
              }
            </div>

            <section class="stack">
              <div class="page-header">
                <div>
                  <span class="eyebrow">Addresses</span>
                  <h3>Delivery and contact locations</h3>
                </div>
                <button type="button" class="ghost-button" (click)="addAddress()">Add address</button>
              </div>

              <div formArrayName="addresses" class="stack">
                @for (address of addressControls(); track $index) {
                  <div class="card-shell content-card" [formGroupName]="$index">
                    <div class="form-grid">
                      <div class="field-block">
                        <label>House no</label>
                        <input formControlName="houseNo" />
                      </div>
                      <div class="field-block">
                        <label>Street</label>
                        <input formControlName="street" />
                      </div>
                      <div class="field-block">
                        <label>City</label>
                        <input formControlName="city" />
                      </div>
                      <div class="field-block">
                        <label>State</label>
                        <input formControlName="state" />
                      </div>
                      <div class="field-block">
                        <label>Pincode</label>
                        <input type="number" formControlName="pincode" />
                        @if (showAddressPincodeError($index)) {
                          <small>Pincode must be a valid 6-digit number.</small>
                        }
                      </div>
                    </div>
                    @if (addresses.length > 1) {
                      <div class="form-actions">
                        <button type="button" class="danger-button" (click)="removeAddress($index)">Remove address</button>
                      </div>
                    }
                  </div>
                }
              </div>
            </section>

            @if (errorMessage()) {
              <small>{{ errorMessage() }}</small>
            }

            <div class="form-actions">
              <button type="submit" class="primary-button" [disabled]="saving()">
                {{ saving() ? 'Saving...' : existingProfile() ? 'Update profile' : 'Create profile' }}
              </button>
            </div>
          </form>
        </section>
      }
    </section>
  `,
})
export class CandidateProfilePageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly profiles = inject(ProfileService);
  private readonly viewerProfile = inject(ViewerProfileService);
  private readonly session = inject(SessionService);
  private readonly toast = inject(ToastService);

  protected readonly existingProfile = signal<ProfileResponse | null>(null);
  protected readonly editing = signal(false);
  protected readonly saving = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly submitted = signal(false);
  protected readonly userEmail = computed(() => this.session.user()?.email ?? '');

  protected readonly form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required]],
    email: [this.userEmail(), [Validators.required, Validators.email]],
    mobile: [0, [Validators.required, Validators.min(1)]],
    dob: [''],
    gender: [''],
    skillsText: ['', [Validators.required]],
    experience: [0, [Validators.required, Validators.min(1)]],
    resumeUrl: ['', [Validators.required]],
    addresses: this.fb.array([this.buildAddressGroup()]),
  });

  constructor() {
    this.form.controls.email.setValue(this.userEmail());
    this.loadProfile();
  }

  protected get addresses(): FormArray<AddressForm> {
    return this.form.controls.addresses as FormArray<AddressForm>;
  }

  protected addressControls(): AddressForm[] {
    return this.addresses.controls;
  }

  protected addAddress(): void {
    this.addresses.push(this.buildAddressGroup());
  }

  protected removeAddress(index: number): void {
    this.addresses.removeAt(index);
  }

  protected submit(): void {
    this.submitted.set(true);
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      this.errorMessage.set('Please complete the required candidate details before saving.');
      return;
    }

    this.errorMessage.set('');
    this.saving.set(true);
    const addresses = this.addresses.getRawValue() as Array<{
      houseNo: string;
      street: string;
      city: string;
      state: string;
      pincode: number | string;
    }>;
    const payload: CandidateProfileRequest = {
      fullName: this.form.controls.fullName.getRawValue(),
      email: this.form.controls.email.getRawValue(),
      mobile: Number(this.form.controls.mobile.getRawValue()),
      dob: this.nullable(this.form.controls.dob.getRawValue()),
      gender: this.nullable(this.form.controls.gender.getRawValue()),
      skills: this.parseList(this.form.controls.skillsText.getRawValue()),
      experience: Number(this.form.controls.experience.getRawValue()),
      resumeUrl: this.form.controls.resumeUrl.getRawValue(),
      addresses: addresses.map((address) => ({
        ...address,
        pincode: Number(address['pincode']),
      })),
    };

    const request$ = this.existingProfile()
      ? this.profiles.updateProfile(this.existingProfile()!.profileId, payload)
      : this.profiles.createCandidateProfile(payload);

    request$.pipe(
      finalize(() => this.saving.set(false)),
    ).subscribe({
      next: (profile) => {
        this.viewerProfile.setCurrentProfile(profile);
        this.applyProfile(profile);
        this.editing.set(false);
        this.toast.success('Profile saved', 'Candidate profile is synced with backend.');
      },
      error: (error: unknown) => {
        this.errorMessage.set(getErrorMessage(error, 'Unable to save the profile.'));
        if (this.errorMessage().toLowerCase().includes('email is already associated')) {
          this.recoverExistingProfile();
          return;
        }
        this.toast.error('Profile save failed', this.errorMessage());
      },
    });
  }

  protected showError(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];
    return !!control && control.invalid && (control.touched || this.submitted());
  }

  protected showAddressPincodeError(index: number): boolean {
    const control = this.addresses.at(index)?.get('pincode');
    return !!control && control.invalid && (control.touched || this.submitted());
  }

  protected startEditing(): void {
    this.editing.set(true);
    this.submitted.set(false);
    this.errorMessage.set('');
  }

  private recoverExistingProfile(): void {
    const email = this.userEmail().toLowerCase();
    this.viewerProfile.clearCache();
    this.profiles.getProfiles('CANDIDATE').pipe(
      map((profiles) => profiles.find((profile) => profile.email.toLowerCase() === email) ?? null),
      catchError(() => of(null)),
    ).subscribe((profile) => {
      if (!profile) {
        this.toast.error('Profile lookup failed', 'Your candidate profile exists in backend but could not be loaded.');
        return;
      }

      this.viewerProfile.setCurrentProfile(profile);
      this.applyProfile(profile);
      this.editing.set(false);
      this.toast.info('Profile already exists', 'Loaded your existing candidate profile from backend.');
    });
  }

  protected cancelEditing(): void {
    this.editing.set(false);
    this.loadProfile(true);
  }

  private loadProfile(force = false): void {
    this.viewerProfile.getCurrentProfile(force).subscribe((profile) => {
      if (!profile) {
        this.existingProfile.set(null);
        return;
      }
      this.applyProfile(profile);
    });
  }

  private applyProfile(profile: ProfileResponse): void {
    this.existingProfile.set(profile);
    while (this.addresses.length > 0) {
      this.addresses.removeAt(0);
    }
    (profile.addresses.length ? profile.addresses : [null]).forEach((address) =>
      this.addresses.push(this.buildAddressGroup(address ?? undefined)),
    );
    this.form.patchValue({
      fullName: profile.fullName ?? '',
      email: profile.email ?? this.userEmail(),
      mobile: Number(profile.mobile ?? 0),
      dob: profile.dob ?? '',
      gender: profile.gender ?? '',
      skillsText: profile.skills.join(', '),
      experience: Number(profile.experience ?? 0),
      resumeUrl: profile.resumeUrl ?? '',
    });
    this.form.markAsPristine();
    this.form.markAsUntouched();
    this.submitted.set(false);
    this.errorMessage.set('');
  }

  private buildAddressGroup(address?: ProfileResponse['addresses'][number]): AddressForm {
    return this.fb.nonNullable.group({
      houseNo: [address?.houseNo ?? '', [Validators.required]],
      street: [address?.street ?? '', [Validators.required]],
      city: [address?.city ?? '', [Validators.required]],
      state: [address?.state ?? '', [Validators.required]],
      pincode: [Number(address?.pincode ?? 0), [Validators.required, Validators.min(100000), Validators.max(999999)]],
    });
  }

  private parseList(value: string): string[] {
    return value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean);
  }

  private nullable(value: string): string | null {
    return value.trim() ? value.trim() : null;
  }
}
