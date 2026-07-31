export function isAdminRole(role?: string | null): boolean {
  return role === 'ADMIN' || role === 'SUPER_ADMIN';
}

export function isSuperAdminRole(role?: string | null): boolean {
  return role === 'SUPER_ADMIN';
}
