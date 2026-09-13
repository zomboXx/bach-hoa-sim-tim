const loginScreen = document.querySelector("#login-screen");
const workspace = document.querySelector("#workspace");
const loginForm = document.querySelector("#login-form");
const employeeInput = document.querySelector("#employee-id");
const passwordInput = document.querySelector("#password");
const loginError = document.querySelector("#login-error");
const dashboardView = document.querySelector("#dashboard-view");
const trainingView = document.querySelector("#training-view");
const modal = document.querySelector("#modal");
const toast = document.querySelector("#toast");
let toastTimer;

const rememberedEmployee = localStorage.getItem("simtim-employee");
if (rememberedEmployee) {
  employeeInput.value = rememberedEmployee;
  document.querySelector("#remember").checked = true;
}

const dateFormatter = new Intl.DateTimeFormat("vi-VN", {
  weekday: "long",
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
});
const dateLabel = document.querySelector("#today-label");
dateLabel.textContent = dateFormatter.format(new Date()).replace(/^./, (letter) => letter.toUpperCase());
const currentHour = new Date().getHours();
const greeting = currentHour < 11 ? "Chào buổi sáng" : currentHour < 18 ? "Chào buổi chiều" : "Chào buổi tối";
document.querySelector(".page-heading h1").firstChild.textContent = `${greeting}, Lan! `;

function setSession(isLoggedIn) {
  loginScreen.hidden = isLoggedIn;
  workspace.hidden = !isLoggedIn;
  document.body.style.overflow = "";
}

function showToast(message) {
  clearTimeout(toastTimer);
  toast.querySelector("p").textContent = message;
  toast.classList.add("show");
  toastTimer = setTimeout(() => toast.classList.remove("show"), 2600);
}

function showUpdateModal() {
  modal.hidden = false;
  document.body.style.overflow = "hidden";
  setTimeout(() => modal.querySelector(".modal-confirm").focus(), 20);
}

function hideUpdateModal() {
  modal.hidden = true;
  document.body.style.overflow = "";
}

function changeView(viewName) {
  const isTraining = viewName === "training";
  dashboardView.hidden = isTraining;
  trainingView.hidden = !isTraining;
  document.querySelector(".topbar").hidden = isTraining;
  document.querySelector(".sidebar").hidden = isTraining;
  workspace.style.gridTemplateColumns = isTraining ? "1fr" : "240px minmax(0, 1fr)";
  document.querySelector(".mentor-fab").hidden = isTraining;
  document.querySelectorAll(".nav-item[data-view]").forEach((item) => {
    item.classList.toggle("active", item.dataset.view === viewName);
  });
  window.scrollTo({ top: 0, behavior: "smooth" });
}

loginForm.addEventListener("submit", (event) => {
  event.preventDefault();
  const employeeId = employeeInput.value.trim().toUpperCase();
  const password = passwordInput.value;
  employeeInput.classList.remove("error");
  passwordInput.classList.remove("error");
  loginError.textContent = "";

  if (!employeeId || !password) {
    if (!employeeId) employeeInput.classList.add("error");
    if (!password) passwordInput.classList.add("error");
    loginError.textContent = "Vui lòng nhập đầy đủ mã nhân viên và mật khẩu.";
    return;
  }

  if (employeeId !== "NV001" || password !== "demo123") {
    employeeInput.classList.add("error");
    passwordInput.classList.add("error");
    loginError.textContent = "Thông tin đăng nhập chưa đúng. Hãy dùng tài khoản demo.";
    return;
  }

  if (document.querySelector("#remember").checked) {
    localStorage.setItem("simtim-employee", employeeId);
  } else {
    localStorage.removeItem("simtim-employee");
  }
  setSession(true);
  changeView("dashboard");
  showToast("Đăng nhập thành công. Chào mừng Lan trở lại!");
});

document.querySelector("[data-demo]").addEventListener("click", () => {
  employeeInput.value = "NV001";
  passwordInput.value = "demo123";
  loginError.textContent = "";
  employeeInput.classList.remove("error");
  passwordInput.classList.remove("error");
  showToast("Đã điền tài khoản demo");
});

document.querySelector(".password-toggle").addEventListener("click", (event) => {
  const showPassword = passwordInput.type === "password";
  passwordInput.type = showPassword ? "text" : "password";
  event.currentTarget.setAttribute("aria-label", showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu");
});

document.querySelectorAll("[data-training]").forEach((button) => {
  button.addEventListener("click", () => changeView("training"));
});

document.querySelectorAll("[data-view]").forEach((button) => {
  button.addEventListener("click", (event) => {
    event.preventDefault();
    changeView(button.dataset.view);
  });
});

document.querySelectorAll("[data-soon]").forEach((button) => {
  button.addEventListener("click", showUpdateModal);
});

document.querySelectorAll(".chapter-button").forEach((button) => {
  button.addEventListener("click", showUpdateModal);
});

document.querySelector(".modal-close").addEventListener("click", hideUpdateModal);
document.querySelector(".modal-confirm").addEventListener("click", hideUpdateModal);
modal.addEventListener("click", (event) => {
  if (event.target === modal) hideUpdateModal();
});
document.addEventListener("keydown", (event) => {
  if (event.key === "Escape" && !modal.hidden) hideUpdateModal();
  if (event.key === "F2" && !workspace.hidden && trainingView.hidden) {
    event.preventDefault();
    showUpdateModal();
  }
});

const profileMenu = document.querySelector(".profile-menu");
document.querySelector(".profile-menu-button").addEventListener("click", () => {
  profileMenu.hidden = !profileMenu.hidden;
});

document.querySelector("#logout-button").addEventListener("click", () => {
  profileMenu.hidden = true;
  setSession(false);
  passwordInput.value = "";
  showToast("Bạn đã đăng xuất an toàn");
});

document.querySelector(".mobile-menu").addEventListener("click", () => {
  workspace.classList.toggle("menu-open");
});

document.addEventListener("click", (event) => {
  if (!event.target.closest(".profile-block")) profileMenu.hidden = true;
  if (workspace.classList.contains("menu-open") && !event.target.closest(".sidebar") && !event.target.closest(".mobile-menu")) {
    workspace.classList.remove("menu-open");
  }
});

setSession(false);
