export type Role = 'EMPLOYEE' | 'ADMIN';
export type EmploymentStatus = 'ACTIVE' | 'TERMINATED';
export type BackgroundCheckStatus =
  | 'REQUESTING'
  | 'PENDING'
  | 'CLEAR'
  | 'FLAGGED'
  | 'SUBMISSION_UNKNOWN'
  | 'SUBMISSION_FAILED';

export type CurrentUser = {
  accountId: number;
  employeeId: number;
  employeeNumber: string;
  username: string;
  role: Role;
  passwordChangeRequired: boolean;
};

export type CsrfToken = { headerName: string; parameterName: string; token: string };
export type ApiError = { code?: string; message?: string };

export type Profile = {
  id: number;
  employeeNumber: string;
  lastName: string;
  firstName: string;
  fullName: string;
  dateOfBirth: string | null;
  employmentStatus: EmploymentStatus;
  username: string;
};

export interface CheckStatusLike {
  status: BackgroundCheckStatus;
  trackingActive: boolean;
  trackingStopReason: string | null;
}

export type LatestCheck = CheckStatusLike & { id: number; requestedAt: string };

export type Employee = {
  latestBackgroundCheck: LatestCheck | null;
  id: number;
  employeeNumber: string;
  fullName: string;
  dateOfBirth: string | null;
  employmentStatus: EmploymentStatus;
};

export type EmployeeDetail = Employee & {
  lastName: string;
  firstName: string;
  terminationDate: string | null;
  terminatedAt: string | null;
  account: { username: string; enabled: boolean; passwordChangeRequired: boolean } | null;
};

export type Check = CheckStatusLike & {
  id: number;
  externalCheckId: string | null;
  submittedFirstName: string;
  submittedLastName: string;
  submittedDateOfBirth: string;
  result: {
    criminalRecord: boolean | null;
    educationVerified: boolean | null;
    employmentVerified: boolean | null;
    creditScore: string | null;
  } | null;
  requestedAt: string;
  completedAt: string | null;
  lastCheckedAt: string | null;
};
