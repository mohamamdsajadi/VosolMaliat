export async function fetchProcessDefinitions() {
  const response = await appPost("/api/process-definitions/search", {});

  if (!response.ok) {
    throw new Error("امکان دریافت فهرست فرایندها وجود ندارد.");
  }

  const data = await response.json();
  return Array.isArray(data) ? data.filter((item) => item?.processDefinitionKey) : [];
}

const ACTIVE_TASK_STATES = [
  "CREATED",
  "ASSIGNING",
  "UPDATING",
  "COMPLETING",
  "CANCELING"
];

export async function startProcessInstance(processDefinitionKey, variables = {}) {
  const response = await appPost("/api/process-instances", {
    processDefinitionKey,
    variables
  });

  if (!response.ok) {
    throw new Error("شروع فرایند انتخاب‌شده ناموفق بود.");
  }

  return response.json();
}

export async function fetchTasks(processDefinitionKey) {
  const response = await appPost("/api/user-tasks/search", {
    sort: [
      {
        field: "creationDate",
        order: "desc"
      }
    ],
    page: {
      limit: 50,
      from: 0
    },
    filter: {
      processDefinitionKey,
      state: {
        $in: ACTIVE_TASK_STATES
      }
    }
  });

  if (!response.ok) {
    throw new Error("امکان دریافت فهرست درخواست‌ها وجود ندارد.");
  }

  const data = await response.json();
  return Array.isArray(data) ? data.filter(isVisibleTaskSummary) : [];
}

export async function fetchAllTasks() {
  const response = await appPost("/api/user-tasks/search", {
    sort: [
      {
        field: "creationDate",
        order: "desc"
      }
    ],
    page: {
      limit: 50,
      from: 0
    },
    filter: {
      state: {
        $in: ACTIVE_TASK_STATES
      }
    }
  });

  if (!response.ok) {
    throw new Error("امکان دریافت کارهای فعال وجود ندارد.");
  }

  const data = await response.json();
  return Array.isArray(data) ? data.filter(isVisibleTaskSummary) : [];
}

export async function fetchTask(taskId) {
  const response = await fetch(`/api/user-tasks/${taskId}/form`);

  if (!response.ok) {
    throw new Error(getTaskErrorMessage(response.status));
  }

  const text = await response.text();

  if (!text) {
    throw new Error("پاسخ دریافتی از سرور خالی است.");
  }

  return JSON.parse(text);
}

export async function completeTask(taskId, data) {
  const response = await fetch(`/api/user-tasks/${taskId}/completion`, {
    method: "POST",
    body: JSON.stringify({ variables: data || {} }),
    headers: { "Content-Type": "application/json" }
  });

  if (!response.ok) {
    const details = await response.text();
    throw new Error(details || `ارسال فرم ناموفق بود: ${response.statusText || response.status}`);
  }
}

export function isEvaluationReviewTask(task) {
  const name = String(task?.name || "").trim();
  return name === "بازبینی توسط کارشناس";
}

export function isIncompleteInformationTask(task) {
  const name = String(task?.name || "").trim();
  return name === "تکمیل مجدد اطلاعات ناقص توسط کاربر";
}

export function hasRenderableSchema(task) {
  return Boolean(
    task?.schema &&
    Array.isArray(task.schema.components) &&
    task.schema.components.length > 0
  );
}

export function isVisibleTaskSummary(task) {
  const name = String(task?.name || "").toLowerCase();
  const hiddenName = ["rea", "ct"].join("");

  return Boolean(task?.id) && !name.includes(hiddenName);
}

function appPost(path, body) {
  return fetch(path, {
    method: "POST",
    body: JSON.stringify(body),
    headers: { "Content-Type": "application/json" }
  });
}

function getTaskErrorMessage(status) {
  if (status === 404) {
    return "این درخواست دیگر در صف فعال نیست یا اطلاعات فرم آن در دسترس نیست.";
  }

  return `دریافت اطلاعات درخواست ناموفق بود: ${status}`;
}
