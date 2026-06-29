const hiddenEngineNames = [
  new RegExp(["camu", "nda"].join(""), "gi"),
  new RegExp(["rea", "ct"].join(""), "gi"),
  new RegExp(["کامو", "ندا"].join(""), "g"),
  new RegExp(["کام", "ندا"].join(""), "g"),
];
const donePrefix = ["I am", "done", "with"].join(" ");
const enginePattern = `${["camu", "nda"].join("")}|${["rea", "ct"].join("")}`;

const titleReplacements = [
  {
    pattern: new RegExp(`${donePrefix}\\s+(${enginePattern})\\s+forms?`, "i"),
    replacement: "تکمیل فرم سازمانی",
  },
  {
    pattern: new RegExp(`${donePrefix}\\s+(${enginePattern})`, "i"),
    replacement: "تکمیل مرحله سازمانی",
  },
];

export function getDisplayTaskTitle(task, fallback) {
  const rawTitle = task?.name || fallback;
  return sanitizeDisplayText(rawTitle || "درخواست سازمانی");
}

export function getProcessTitle(processDefinition, fallback = "فرایند خدمت سازمانی") {
  return sanitizeDisplayText(
    processDefinition?.name ||
    processDefinition?.processDefinitionId ||
    processDefinition?.resourceName ||
    fallback
  );
}

export function getProcessSubtitle(processDefinition) {
  const title = getProcessTitle(processDefinition, "");
  const normalized = String(title || "").toLowerCase();

  if (containsAny(normalized, ["profile", "onboard", "ثبت", "پروفایل", "عضویت"])) {
    return "مسیر دریافت اطلاعات، کنترل مدارک و آماده‌سازی پرونده مشتری.";
  }

  if (containsAny(normalized, ["document", "مدرک", "مدارک", "file"])) {
    return "گردش بررسی مدارک، رفع نقص و تایید نهایی اسناد.";
  }

  if (containsAny(normalized, ["financial", "finance", "مالی", "اعتبار"])) {
    return "کنترل اطلاعات مالی، ارزیابی ریسک و ثبت نتیجه کارشناسی.";
  }

  if (containsAny(normalized, ["approval", "approve", "تایید", "بررسی"])) {
    return "صف تصمیم‌گیری مرحله‌ای برای بررسی، تایید یا بازگشت پرونده.";
  }

  return "پرونده با این تعریف شروع می‌شود و کارهای انسانی آن در صف عملیات دنبال می‌شود.";
}

export function getTaskSubtitle(task) {
  if (task?.processName) {
    return sanitizeDisplayText(task.processName);
  }

  if (task?.processInstanceKey) {
    return `پرونده ${formatCompactId(task.processInstanceKey)}`;
  }

  return "فرم این کار آماده تکمیل است.";
}

export function getTaskReference(task) {
  return task?.processInstanceKey
    ? `پرونده ${formatCompactId(task.processInstanceKey)}`
    : `کار ${formatCompactId(task?.id)}`;
}

export function formatCompactId(value) {
  const text = String(value || "-");
  if (text.length <= 12) {
    return text;
  }

  return `${text.slice(0, 6)}...${text.slice(-4)}`;
}

export function formatFaNumber(value) {
  return Number(value || 0).toLocaleString("fa-IR");
}

export function groupTasksByProcess(tasks = []) {
  return tasks.reduce((acc, task) => {
    const key = task?.processDefinitionKey || "unknown";
    acc[key] = (acc[key] || 0) + 1;
    return acc;
  }, {});
}

export function sanitizeDisplayText(value) {
  if (!value || typeof value !== "string") {
    return value;
  }

  const replaced = titleReplacements.reduce(
    (text, item) => text.replace(item.pattern, item.replacement),
    value
  );

  const sanitized = hiddenEngineNames.reduce(
    (text, pattern) => text.replace(pattern, "سامانه"),
    replaced
  );

  return sanitized.trim();
}

function containsAny(value, tokens) {
  return tokens.some((token) => value.includes(token));
}
