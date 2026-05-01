import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, finalize, map, of } from 'rxjs';

import { COUNTRY_CODES, INDIA_CITIES_BY_STATE, INDIA_STATES } from '../../../core/constants/location.constants';
import { CandidateProfileRequest, ProfileResponse } from '../../../core/models/profile.models';
import { ProfileService } from '../../../core/services/profile.service';
import { SessionService } from '../../../core/services/session.service';
import { ToastService } from '../../../core/services/toast.service';
import { ViewerProfileService } from '../../../core/services/viewer-profile.service';
import { getErrorMessage } from '../../../core/utils/http-error.util';
import { indianMobileValidator } from '../../../core/validators/phone.validator';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';

type AddressForm = FormGroup;

@Component({
  selector: 'app-candidate-profile-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  template: `
    <section class="page-section profile-workspace">
      <app-page-header
        eyebrow="Candidate profile"
        title="Shape the profile recruiters will see"
        description="Keep your resume link, skills, and contact details in sync with the backend profile service."
      />

      @if (existingProfile() && !editing()) {
        <section class="profile-board">
          <article class="profile-spotlight">
            <div class="profile-spotlight__identity">
              <span class="profile-avatar material-symbols-rounded">person</span>
              <div>
                <h2>{{ existingProfile()!.fullName }}</h2>
                <strong>{{ primarySkill() }}</strong>
                <p>
                  <span class="material-symbols-rounded">location_on</span>
                  {{ locationSummary() }}
                  <span>&middot;</span>
                  {{ existingProfile()!.experience || 0 }} years experience
                </p>
                <p class="profile-bio">{{ profileOverview() }}</p>
              </div>
            </div>

            <div class="profile-checklist">
              <p><span class="material-symbols-rounded">{{ existingProfile()!.resumeUrl ? 'check_circle' : 'radio_button_unchecked' }}</span> Resume URL added</p>
              <p><span class="material-symbols-rounded">{{ existingProfile()!.skills.length ? 'check_circle' : 'radio_button_unchecked' }}</span> Skills added</p>
              <p><span class="material-symbols-rounded">{{ existingProfile()!.experience ? 'check_circle' : 'radio_button_unchecked' }}</span> Experience added</p>
              <p><span class="material-symbols-rounded">{{ existingProfile()!.addresses.length ? 'check_circle' : 'radio_button_unchecked' }}</span> Address added</p>
            </div>

            <div class="profile-standout">
              <span class="material-symbols-rounded">auto_awesome</span>
              <div>
                <strong>Stand out to recruiters</strong>
                <p>This overview is generated only from the fields saved in your profile form.</p>
                <button type="button" class="primary-button" (click)="startEditing()">Update profile</button>
              </div>
            </div>
          </article>

          <section class="profile-dashboard-grid">
            <article class="profile-dashboard-card">
              <div class="profile-card-head">
                <span class="material-symbols-rounded">badge</span>
                <div>
                  <h3>Personal details</h3>
                  <p>Core identity and contact details synced from profile-service.</p>
                </div>
              </div>

              <div class="profile-detail-grid">
                <div class="profile-detail-item">
                  <span class="material-symbols-rounded">mail</span>
                  <label>Email</label>
                  <strong>{{ existingProfile()!.email }}</strong>
                </div>
                <div class="profile-detail-item">
                  <span class="material-symbols-rounded">phone_iphone</span>
                  <label>Mobile</label>
                  <strong>{{ existingProfile()!.mobile || 'Not added' }}</strong>
                </div>
                <div class="profile-detail-item">
                  <span class="material-symbols-rounded">calendar_month</span>
                  <label>Date of birth</label>
                  <strong>{{ existingProfile()!.dob ? (existingProfile()!.dob | date:'mediumDate') : 'Not added' }}</strong>
                </div>
                <div class="profile-detail-item">
                  <span class="material-symbols-rounded">transgender</span>
                  <label>Gender</label>
                  <strong>{{ formatLabel(existingProfile()!.gender) }}</strong>
                </div>
                <div class="profile-detail-item">
                  <span class="material-symbols-rounded">business_center</span>
                  <label>Experience</label>
                  <strong>{{ existingProfile()!.experience || 0 }} years</strong>
                </div>
              </div>
            </article>

            <article class="profile-dashboard-card">
              <div class="profile-card-head">
                <span class="material-symbols-rounded">description</span>
                <div>
                  <h3>Resume and skills</h3>
                  <p>Your resume and top skills as seen by recruiters.</p>
                </div>
              </div>

              <div class="resume-row">
                <span class="material-symbols-rounded">article</span>
                <div>
                  <label>Resume</label>
                  @if (existingProfile()!.resumeUrl) {
                    <strong>{{ resumeFileName() }}</strong>
                    <a [href]="existingProfile()!.resumeUrl!" target="_blank" rel="noreferrer">Preview</a>
                  } @else {
                    <strong>Not added</strong>
                  }
                </div>
              </div>

              <div class="resume-row">
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

              <label class="profile-label">Top skills</label>
              <div class="chip-group">
                @for (skill of existingProfile()!.skills; track skill) {
                  <span class="chip">{{ skill }}</span>
                }
              </div>
            </article>

            <article class="profile-dashboard-card">
              <div class="profile-card-head">
                <span class="material-symbols-rounded">summarize</span>
                <div>
                  <h3>Profile overview</h3>
                  <p>Generated from your experience, skills, resume, and location fields.</p>
                </div>
              </div>

              <div class="generated-overview">
                <strong>{{ profileHeadline() }}</strong>
                <p>{{ profileOverview() }}</p>
                <div class="chip-group">
                  <span class="chip">{{ existingProfile()!.experience || 0 }} years</span>
                  <span class="chip">{{ existingProfile()!.resumeUrl ? 'Resume linked' : 'Resume missing' }}</span>
                  <span class="chip">{{ locationSummary() }}</span>
                </div>
              </div>
            </article>

            <article class="profile-dashboard-card">
              <div class="profile-card-head">
                <span class="material-symbols-rounded">contact_mail</span>
                <div>
                  <h3>Location and contact</h3>
                  <p>Where you're based and how recruiters can reach you.</p>
                </div>
              </div>

              <div class="profile-location-grid profile-location-grid--single">
                <div>
                  <span class="material-symbols-rounded">pin_drop</span>
                  <label>Current location</label>
                  @if (primaryAddress(); as address) {
                    <strong>{{ address.houseNo }}</strong>
                    <p>{{ address.street }}</p>
                    <p>{{ address.city }}, {{ address.state }} {{ address.pincode }}</p>
                    <p>India</p>
                  } @else {
                    <strong>Not added</strong>
                  }
                </div>
              </div>
            </article>
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
                <div class="phone-field">
                  <select formControlName="countryCode" aria-label="Country code">
                    @for (country of countryCodes; track country.code) {
                      <option [value]="country.code">{{ country.code }} {{ country.label }}</option>
                    }
                  </select>
                  <input id="mobile" inputmode="numeric" maxlength="10" formControlName="mobile" placeholder="9876543210" />
                </div>
                @if (showError('mobile')) {
                  <small>Enter a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9.</small>
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
                        <select formControlName="city">
                          <option value="">Select city</option>
                          @for (city of citiesFor($index); track city) {
                            <option [value]="city">{{ city }}</option>
                          }
                        </select>
                      </div>
                      <div class="field-block">
                        <label>State</label>
                        <select formControlName="state" (change)="onStateChange($index)">
                          <option value="">Select state</option>
                          @for (state of states; track state) {
                            <option [value]="state">{{ state }}</option>
                          }
                        </select>
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
  protected readonly countryCodes = COUNTRY_CODES;
  protected readonly states = INDIA_STATES;

  protected readonly form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required]],
    email: [this.userEmail(), [Validators.required, Validators.email]],
    countryCode: ['+91'],
    mobile: ['', [indianMobileValidator()]],
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

  protected primarySkill(): string {
    return this.existingProfile()?.skills?.[0] || 'Candidate';
  }

  protected profileHeadline(): string {
    const profile = this.existingProfile();
    const experience = profile?.experience ?? 0;
    return `${this.primarySkill()} candidate with ${experience} ${experience === 1 ? 'year' : 'years'} of experience`;
  }

  protected profileOverview(): string {
    const profile = this.existingProfile();
    if (!profile) {
      return '';
    }

    const experience = profile.experience ?? 0;
    const skills = profile.skills?.length ? profile.skills.join(', ') : 'skills not added yet';
    const resume = profile.resumeUrl
      ? 'A resume link is available for recruiters to review.'
      : 'Resume link is not added yet.';

    return `${profile.fullName} is a ${this.primarySkill()} candidate with ${experience} ${experience === 1 ? 'year' : 'years'} of experience. Key skills include ${skills}. Based in ${this.locationSummary()}. ${resume}`;
  }

  protected primaryAddress(): ProfileResponse['addresses'][number] | null {
    return this.existingProfile()?.addresses?.[0] ?? null;
  }

  protected locationSummary(): string {
    const address = this.primaryAddress();
    if (!address) {
      return 'Location not added';
    }
    return `${address.city}, ${address.state}`;
  }

  protected resumeFileName(): string {
    const resumeUrl = this.existingProfile()?.resumeUrl;
    if (!resumeUrl) {
      return 'Not added';
    }

    const cleanUrl = resumeUrl.split('?')[0];
    const fileName = cleanUrl.substring(cleanUrl.lastIndexOf('/') + 1);
    return fileName || 'Candidate_Resume.pdf';
  }

  protected formatLabel(value: string | null): string {
    if (!value) {
      return 'Not added';
    }
    return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
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
      countryCode: '+91',
      mobile: profile.mobile ? String(profile.mobile) : '',
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

  protected citiesFor(index: number): string[] {
    const state = String(this.addresses.at(index)?.get('state')?.value ?? '');
    return INDIA_CITIES_BY_STATE[state] ?? [];
  }

  protected onStateChange(index: number): void {
    const group = this.addresses.at(index);
    const cityControl = group?.get('city');
    const cities = this.citiesFor(index);
    if (cityControl && !cities.includes(String(cityControl.value ?? ''))) {
      cityControl.setValue('');
    }
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
