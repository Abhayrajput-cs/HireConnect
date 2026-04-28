export interface AddressRequest {
  houseNo: string;
  street: string;
  city: string;
  state: string;
  pincode: number;
}

export interface AddressResponse extends AddressRequest {
  addressId: number;
}

export interface CandidateProfileRequest {
  fullName: string;
  email: string;
  mobile: number;
  dob?: string | null;
  gender?: string | null;
  skills: string[];
  experience: number;
  resumeUrl: string;
  addresses: AddressRequest[];
}

export interface RecruiterProfileRequest {
  fullName: string;
  email: string;
  mobile?: number | null;
  dob?: string | null;
  gender?: string | null;
  companyName: string;
  companySize?: string | null;
  industry: string;
  website?: string | null;
  addresses: AddressRequest[];
}

export interface ProfileResponse {
  profileId: number;
  role: string;
  fullName: string;
  email: string;
  mobile: number | null;
  dob: string | null;
  gender: string | null;
  skills: string[];
  experience: number | null;
  resumeUrl: string | null;
  companyName: string | null;
  companySize: string | null;
  industry: string | null;
  website: string | null;
  addresses: AddressResponse[];
}
