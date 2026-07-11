import { apiJson } from './api';

export function fetchCurrentUser() {
  return apiJson('/api/v1/auth/me');
}

export function fetchCurrentPermissions() {
  return apiJson('/api/v1/auth/me/permissions');
}

export function fetchUsers() {
  return apiJson('/api/v1/users');
}

export function createUserProfile(profile) {
  return apiJson('/api/v1/users', {
    method: 'POST',
    body: JSON.stringify(profile)
  });
}

export function updateUserProfile(id, profile) {
  return apiJson(`/api/v1/users/${id}`, {
    method: 'PUT',
    body: JSON.stringify(profile)
  });
}

export function assignUserOrganization(id, organizationUnitId) {
  return apiJson(`/api/v1/users/${id}/organization-unit`, {
    method: 'PATCH',
    body: JSON.stringify({ organizationUnitId })
  });
}

export function fetchPermissions() {
  return apiJson('/api/v1/permissions');
}

export function assignGroupPermission(groupName, permissionCode) {
  return apiJson('/api/v1/group-permissions', {
    method: 'POST',
    body: JSON.stringify({ groupName, permissionCode })
  });
}
