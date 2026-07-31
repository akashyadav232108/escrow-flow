export type Role = 'CLIENT' | 'FREELANCER' | 'BOTH' | 'ADMIN' | 'SUPER_ADMIN';

export type AccountStatus = 'ACTIVE' | 'WARNED' | 'SUSPENDED' | 'DELETED';

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
  accountStatus?: AccountStatus;
  createdAt?: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Wallet {
  id: number;
  balance: number;
  updatedAt: string;
}

export type TransactionType = 'CREDIT' | 'DEBIT';

export interface Transaction {
  id: number;
  type: TransactionType;
  amount: number;
  referenceType: string;
  referenceId: number;
  balanceAfter: number;
  createdAt: string;
}

export interface TransactionPage {
  content: Transaction[];
  page: number;
  size: number;
  totalElements: number;
}

export type MilestoneStatus =
  | 'PENDING'
  | 'FUNDS_LOCKED'
  | 'SUBMITTED'
  | 'APPROVED'
  | 'DISPUTED'
  | 'REFUNDED';

export interface Milestone {
  id: number;
  title: string;
  description?: string;
  amount: number;
  status: MilestoneStatus;
}

export type ProjectStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface ProjectPerson {
  id: number;
  name: string;
}

export interface Project {
  id: number;
  title: string;
  description?: string;
  status: ProjectStatus;
  client?: ProjectPerson;
  freelancer?: ProjectPerson | null;
  milestones?: Milestone[];
}

export interface CreateMilestoneInput {
  title: string;
  description?: string;
  amount: number;
}

export interface CreateProjectInput {
  title: string;
  description?: string;
  milestones: CreateMilestoneInput[];
}

export interface ApiErrorResponse {
  error: string;
  message: string;
  timestamp: string;
}

export type DisputeStatus = 'OPEN' | 'RESOLVED';
export type DisputeResolution = 'FREELANCER_WINS' | 'CLIENT_WINS';
export type EscrowHoldStatus = 'HELD' | 'RELEASED' | 'REFUNDED';

export interface AdminDashboardStats {
  totalUsers: number;
  clients: number;
  freelancers: number;
  both: number;
  admins: number;
  warnedUsers: number;
  suspendedUsers: number;
  openProjects: number;
  inProgressProjects: number;
  completedProjects: number;
  cancelledProjects: number;
  totalEscrowHeld: number;
  disputedMilestones: number;
}

export interface AdminUser {
  id: number;
  name: string;
  email: string;
  role: Role;
  createdAt: string;
  createdById: number | null;
  createdByName: string | null;
}

export interface UserWarning {
  id: number;
  reason: string;
  issuedByAdminId: number;
  issuedByAdminName: string;
  createdAt: string;
}

export interface ManagedUser {
  id: number;
  name: string;
  email: string;
  role: Role;
  accountStatus: AccountStatus;
  createdAt: string;
  deletedAt: string | null;
  warningCount: number;
  warnings: UserWarning[];
}

export interface DisputeDetail {
  id: number;
  milestoneId: number;
  milestoneTitle: string;
  amount: number;
  milestoneStatus: MilestoneStatus;
  escrowHoldStatus: EscrowHoldStatus | null;
  projectId: number;
  projectTitle: string;
  clientId: number;
  clientName: string;
  freelancerId: number | null;
  freelancerName: string | null;
  raisedById: number;
  raisedByName: string;
  reason: string;
  status: DisputeStatus;
  resolution: DisputeResolution | null;
  resolvedByAdminId: number | null;
  resolvedByAdminName: string | null;
  adminNote: string | null;
  submittedNote: string | null;
  createdAt: string;
  resolvedAt: string | null;
}

