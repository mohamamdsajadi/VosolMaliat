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

export function fetchUserProfile(id) {
  return apiJson(`/api/v1/users/${id}`);
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

export function deleteUserProfile(id) {
  return apiJson(`/api/v1/users/${id}`, {
    method: 'DELETE'
  });
}

export function assignUserTaxUnit(id, taxUnitId) {
  return apiJson(`/api/v1/users/${id}/tax-unit`, {
    method: 'PATCH',
    body: JSON.stringify({ taxUnitId })
  });
}

export function setUserStatus(id, enabled) {
  return apiJson(`/api/v1/users/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ enabled })
  });
}

export function setUserLock(id, locked) {
  return apiJson(`/api/v1/users/${id}/lock`, {
    method: 'PATCH',
    body: JSON.stringify({ locked })
  });
}

export function fetchPermissions() {
  return apiJson('/api/v1/permissions');
}

export function fetchGroupPermissions(groupName) {
  const query = groupName ? `?groupName=${encodeURIComponent(groupName)}` : '';
  return apiJson(`/api/v1/group-permissions${query}`);
}

export function assignGroupPermission(groupName, permissionCode) {
  return apiJson('/api/v1/group-permissions', {
    method: 'POST',
    body: JSON.stringify({ groupName, permissionCode })
  });
}

export function revokeGroupPermission(id) {
  return apiJson(`/api/v1/group-permissions/${id}`, {
    method: 'DELETE'
  });
}

export function fetchGeneralDirectorates() {
  return apiJson('/api/v1/masterdata/general-directorates');
}

export function createGeneralDirectorate(generalDirectorate) {
  return apiJson('/api/v1/masterdata/general-directorates', {
    method: 'POST',
    body: JSON.stringify(generalDirectorate)
  });
}

export function updateGeneralDirectorate(id, generalDirectorate) {
  return apiJson(`/api/v1/masterdata/general-directorates/${id}`, {
    method: 'PUT',
    body: JSON.stringify(generalDirectorate)
  });
}

export function deleteGeneralDirectorate(id) {
  return apiJson(`/api/v1/masterdata/general-directorates/${id}`, {
    method: 'DELETE'
  });
}

export function fetchTaxAffairsOffices(generalDirectorateId) {
  const query = generalDirectorateId ? `?generalDirectorateId=${encodeURIComponent(generalDirectorateId)}` : '';
  return apiJson(`/api/v1/masterdata/tax-affairs-offices${query}`);
}

export function createTaxAffairsOffice(office) {
  return apiJson('/api/v1/masterdata/tax-affairs-offices', {
    method: 'POST',
    body: JSON.stringify(office)
  });
}

export function updateTaxAffairsOffice(id, office) {
  return apiJson(`/api/v1/masterdata/tax-affairs-offices/${id}`, {
    method: 'PUT',
    body: JSON.stringify(office)
  });
}

export function deleteTaxAffairsOffice(id) {
  return apiJson(`/api/v1/masterdata/tax-affairs-offices/${id}`, {
    method: 'DELETE'
  });
}

export function fetchTaxUnits(taxAffairsOfficeId) {
  const query = taxAffairsOfficeId ? `?taxAffairsOfficeId=${encodeURIComponent(taxAffairsOfficeId)}` : '';
  return apiJson(`/api/v1/masterdata/tax-units${query}`);
}

export function createTaxUnit(unit) {
  return apiJson('/api/v1/masterdata/tax-units', {
    method: 'POST',
    body: JSON.stringify(unit)
  });
}

export function updateTaxUnit(id, unit) {
  return apiJson(`/api/v1/masterdata/tax-units/${id}`, {
    method: 'PUT',
    body: JSON.stringify(unit)
  });
}

export function deleteTaxUnit(id) {
  return apiJson(`/api/v1/masterdata/tax-units/${id}`, {
    method: 'DELETE'
  });
}
