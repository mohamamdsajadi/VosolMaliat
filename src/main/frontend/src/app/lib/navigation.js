export const NAV_ITEMS = [
  {
    key: "processes",
    href: "/",
    label: "داشبورد فرایندها",
    helper: "شروع پرونده و انتخاب مسیر",
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
    helper: "کاربران و دسترسی‌ها",
    permissions: ["USER_PROFILE_VIEW", "PERMISSION_VIEW", "GROUP_PERMISSION_VIEW"]
  },
  {
    key: "users",
    href: "/admin/users",
    label: "مدیریت کاربران",
    helper: "پروفایل و وضعیت کاربران",
    permissions: ["USER_PROFILE_VIEW"]
  },
  {
    key: "permissions",
    href: "/admin/permissions",
    label: "مجوزها",
    helper: "اعطا و لغو دسترسی",
    permissions: ["PERMISSION_VIEW", "PERMISSION_ASSIGN", "GROUP_PERMISSION_VIEW", "GROUP_PERMISSION_UPDATE"]
  },
  {
    key: "organizations",
    href: "/admin/organizations",
    label: "واحدهای سازمانی",
    helper: "اداره کل، اداره و واحد مالیاتی",
    permissions: ["USER_PROFILE_ASSIGN_ORGANIZATION"]
  },
  {
    key: "rules",
    href: "/admin/rules",
    label: "قوانین صدور",
    helper: "شرایط جلوگیری از صدور",
    permissions: ["PERMISSION_ASSIGN"]
  },
  {
    key: "inform-rules",
    href: "/admin/inform-rules",
    label: "قوانین ابلاغ",
    helper: "شرایط جلوگیری از ابلاغ",
    permissions: ["PERMISSION_ASSIGN"]
  },
  {
    key: "fake-electronic-notification",
    href: "/admin/fake-electronic-notification",
    label: "سامانه ابلاغ آزمایشی",
    helper: "تایید دریافت ابلاغ",
    permissions: ["AUDIT_VIEW"]
  },
  {
    key: "fake-sanim",
    href: "/admin/fake-sanim",
    label: "سنیم آزمایشی",
    helper: "تایید دریافت تاریخ ابلاغ",
    permissions: ["AUDIT_VIEW"]
  },
  {
    key: "data-entry",
    href: "/admin/data-entry",
    label: "ورود دستی داده",
    helper: "برگ بدهی و برگ اجرا",
    permissions: ["USER_PROFILE_CREATE", "REPORT_VIEW"]
  }
];

export function visibleNavItems(currentUser) {
  return NAV_ITEMS.filter((item) => canSeeNavItem(currentUser, item));
}

export function canSeeNavItem(currentUser, item) {
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

export function isAdminUser(currentUser) {
  const markers = [
    ...(currentUser?.keycloakRoles || []),
    ...(currentUser?.keycloakGroups || []),
    ...(currentUser?.allowedActions || [])
  ];
  return markers.includes("ADMIN");
}
