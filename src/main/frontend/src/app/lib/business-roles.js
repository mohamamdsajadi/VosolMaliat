export const BUSINESS_ROLES = [
  { value: 'TAX_COLLECTION_OFFICE', label: 'دفتر وصول و اجرای مالیاتی', national: true },
  { value: 'SPECIAL_EXECUTION_DEPARTMENT_HEAD', label: 'رئیس اداره اجرائیات وصول و اجرای ویژه', national: true },
  { value: 'TAX_ORGANIZATION_HEAD', label: 'رئیس کل سازمان امور مالیاتی', national: true },
  { value: 'DIRECTORATE_EXECUTION_UNIT_HEAD', label: 'رئیس واحد اجرائیات اداره کل مربوطه' },
  { value: 'DIRECTORATE_EXECUTION_TEAM_SUPERVISOR', label: 'سرپرست هیئت اجرایی اداره کل مربوطه' },
  { value: 'EXECUTION_OFFICER', label: 'مأمور اجرا' },
  { value: 'SPECIAL_COLLECTION_DIRECTOR_GENERAL', label: 'مدیرکل وصول و اجرای ویژه', national: true },
  { value: 'SPECIAL_EXECUTION_DEPUTY', label: 'معاون اجرائیات وصول و اجرای ویژه', national: true },
  { value: 'EXECUTION_DEPUTY', label: 'معاون اجرائیات' },
  { value: 'TAX_DIRECTORATE_DIRECTOR_GENERAL', label: 'مدیرکل امور مالیاتی اداره کل مربوطه' },
  { value: 'ADMIN', label: 'مدیر سامانه', national: true },
];

export const businessRoleLabel = value => BUSINESS_ROLES.find(role => role.value === value)?.label || value || 'کاربر سامانه';
