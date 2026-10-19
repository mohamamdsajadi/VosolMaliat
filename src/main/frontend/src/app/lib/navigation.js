export const NAV_ITEMS = [
  {
    key: "processes",
    href: "/",
    label: "میزکار مدیریتی",
    helper: "",
    permissions: []
  },
  {
    key: "execution-papers",
    href: "/execution-papers",
    label: "برگ‌های اجرا",
    helper: "مشاهده و پیگیری برگ‌ها",
    permissions: ["REPORT_VIEW"]
  },
  {
    key: "tasks",
    href: "/tasks",
    label: "همه کارهای فعال",
    helper: "صف مشترک کارشناسان",
    permissions: ["TASK_VIEW_OWN", "TASK_VIEW_CANDIDATE"]
  },
  {
    key: "operations",
    href: "/operations",
    label: "دید عملیاتی",
    helper: "کنترل ظرفیت و سلامت",
    permissions: ["REPORT_VIEW", "AUDIT_VIEW"]
  },
  {
    key: "admin",
    href: "/admin",
    label: "مدیریت سامانه",
    helper: "کاربران، مجوزها و سازمان",
    permissions: ["USER_PROFILE_VIEW", "PERMISSION_VIEW", "GROUP_PERMISSION_VIEW", "USER_PROFILE_ASSIGN_ORGANIZATION"]
  },
  {
    key: "rule-management",
    href: "/admin/rule-management",
    label: "مدیریت قوانین سامانه",
    helper: "قوانین صدور و موتور ابلاغ",
    permissions: ["PERMISSION_ASSIGN"]
  },
  {
    key: "data-entry",
    href: "/admin/data-entry",
    label: "ورود دستی داده",
    helper: "برگ بدهی و برگ اجرا",
    permissions: ["USER_PROFILE_CREATE", "REPORT_VIEW"]
  },
  {
    key: "test-front",
    href: "/test-front",
    label: "Test Front",
    helper: "سامانه‌ها و فرایندهای آزمایشی",
    permissions: ["AUDIT_VIEW"]
  }
];

export function visibleNavItems(currentUser) {
  return NAV_ITEMS.filter((item) => canSeeNavItem(currentUser, item));
}

export function canSeeNavItem(currentUser, item) {
  if (item.key === "processes") {
    return isAdminUser(currentUser) || isGeneralOfficeHead(currentUser);
  }
  if (!item.permissions || item.permissions.length === 0) {
    return true;
  }
  if (!currentUser) {
    return false;
  }
  if (isAdminUser(currentUser)) {
    return true;
  }
  const permissions = new Set([
    ...(currentUser.localBusinessPermissions || []),
    ...(currentUser.allowedActions || [])
  ]);
  return item.permissions.some((permission) => permissions.has(permission));
}

export function isGeneralOfficeHead(currentUser) {
  return currentUser?.businessRole === 'GENERAL_OFFICE_HEAD';
}

export function isAdminUser(currentUser) {
  if (currentUser?.businessRole === "ADMIN") {
    return true;
  }
  const markers = [
    ...(currentUser?.keycloakRoles || []),
    ...(currentUser?.keycloakGroups || []),
    ...(currentUser?.allowedActions || [])
  ];
  return markers.includes("ADMIN");
}
