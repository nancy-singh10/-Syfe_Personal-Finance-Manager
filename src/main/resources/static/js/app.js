// Application State
let currentUser = null;
let categoriesList = [];
let transactionsList = [];
let goalsList = [];

// API helper with credentials (session cookies)
async function apiCall(endpoint, options = {}) {
    options.credentials = 'include';
    if (!options.headers) {
        options.headers = {};
    }
    if (options.body && typeof options.body === 'object') {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(options.body);
    }

    const response = await fetch(endpoint, options);
    const contentType = response.headers.get('content-type');
    const isJson = contentType && contentType.includes('application/json');
    const data = isJson ? await response.json() : null;

    if (!response.ok) {
        const errorMsg = (data && data.message) || response.statusText || 'Operation failed';
        throw new Error(errorMsg);
    }

    return data;
}

// Show global notification
function showAlert(message, isError = false) {
    const alertBox = document.getElementById('alert-box');
    alertBox.textContent = message;
    alertBox.className = 'alert-box ' + (isError ? 'alert-error' : 'alert-success');
    alertBox.classList.remove('hidden');

    setTimeout(() => {
        alertBox.classList.add('hidden');
    }, 4000);
}

// Initialization
document.addEventListener('DOMContentLoaded', () => {
    // Set default transaction date to today
    const txDateInput = document.getElementById('tx-date');
    if (txDateInput) {
        const today = new Date().toISOString().split('T')[0];
        txDateInput.value = today;
        txDateInput.max = today;
    }

    // Set default goal target date to tomorrow
    const goalTargetDate = document.getElementById('goal-target-date');
    if (goalTargetDate) {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 30);
        goalTargetDate.value = tomorrow.toISOString().split('T')[0];
    }

    checkAuthStatus();
});

// Authentication Management
async function checkAuthStatus() {
    try {
        const user = await apiCall('/api/auth/me');
        currentUser = user;
        showAppView();
    } catch (err) {
        currentUser = null;
        showAuthView();
    }
}

function showAuthView() {
    document.getElementById('auth-section').classList.remove('hidden');
    document.getElementById('app-section').classList.add('hidden');
    document.getElementById('user-nav').classList.add('hidden');
}

function showAppView() {
    document.getElementById('auth-section').classList.add('hidden');
    document.getElementById('app-section').classList.remove('hidden');
    document.getElementById('user-nav').classList.remove('hidden');
    document.getElementById('nav-username').textContent = currentUser.fullName + ' (' + currentUser.username + ')';

    loadCategories();
    loadTransactions();
    loadGoals();
}

function switchAuthTab(tab) {
    const loginForm = document.getElementById('login-form');
    const regForm = document.getElementById('register-form');
    const loginBtn = document.getElementById('tab-login-btn');
    const regBtn = document.getElementById('tab-register-btn');

    if (tab === 'login') {
        loginForm.classList.remove('hidden');
        regForm.classList.add('hidden');
        loginBtn.classList.add('active');
        regBtn.classList.remove('active');
    } else {
        loginForm.classList.add('hidden');
        regForm.classList.remove('hidden');
        loginBtn.classList.remove('active');
        regBtn.classList.add('active');
    }
}

async function handleLogin(e) {
    e.preventDefault();
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;

    try {
        await apiCall('/api/auth/login', {
            method: 'POST',
            body: { username, password }
        });
        showAlert('Logged in successfully!');
        checkAuthStatus();
    } catch (err) {
        showAlert(err.message, true);
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const username = document.getElementById('reg-username').value.trim();
    const password = document.getElementById('reg-password').value;
    const fullName = document.getElementById('reg-fullname').value.trim();
    const phoneNumber = document.getElementById('reg-phone').value.trim();

    try {
        const res = await apiCall('/api/auth/register', {
            method: 'POST',
            body: { username, password, fullName, phoneNumber }
        });
        showAlert(res.message + '. Please sign in.');
        switchAuthTab('login');
        document.getElementById('login-username').value = username;
    } catch (err) {
        showAlert(err.message, true);
    }
}

async function handleLogout() {
    try {
        await apiCall('/api/auth/logout', { method: 'POST' });
        showAlert('Logged out successfully');
        currentUser = null;
        showAuthView();
    } catch (err) {
        showAlert(err.message, true);
    }
}

// Module Navigation
function switchModule(moduleName) {
    const modules = ['transactions', 'categories', 'goals', 'reports'];
    const tabs = document.querySelectorAll('.nav-tab');

    modules.forEach((mod, idx) => {
        const sec = document.getElementById('module-' + mod);
        if (sec) {
            if (mod === moduleName) {
                sec.classList.remove('hidden');
                tabs[idx].classList.add('active');
            } else {
                sec.classList.add('hidden');
                tabs[idx].classList.remove('active');
            }
        }
    });

    if (moduleName === 'categories') loadCategories();
    if (moduleName === 'transactions') loadTransactions();
    if (moduleName === 'goals') loadGoals();
}

// ================= CATEGORIES =================
async function loadCategories() {
    try {
        const data = await apiCall('/api/categories');
        categoriesList = data.categories || [];
        renderCategoriesTable();
        populateCategoryDropdowns();
    } catch (err) {
        showAlert('Error loading categories: ' + err.message, true);
    }
}

function renderCategoriesTable() {
    const tbody = document.getElementById('categories-table-body');
    if (!tbody) return;

    if (categoriesList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center">No categories found.</td></tr>';
        return;
    }

    tbody.innerHTML = categoriesList.map(cat => `
        <tr>
            <td><strong>${escapeHtml(cat.name)}</strong></td>
            <td><span class="badge ${cat.type === 'INCOME' ? 'badge-income' : 'badge-expense'}">${cat.type}</span></td>
            <td><span class="badge ${cat.isCustom ? 'badge-custom' : 'badge-default'}">${cat.isCustom ? 'Custom' : 'Default'}</span></td>
            <td>
                ${cat.isCustom 
                    ? `<button class="btn btn-danger btn-sm" onclick="handleDeleteCategory('${escapeHtml(cat.name)}')">Delete</button>` 
                    : '<span class="text-muted" style="font-size: 0.8rem;">System Locked</span>'}
            </td>
        </tr>
    `).join('');
}

function populateCategoryDropdowns() {
    const txSelect = document.getElementById('tx-category');
    const filterSelect = document.getElementById('filter-category');
    const editSelect = document.getElementById('edit-tx-category');

    const options = categoriesList.map(c => `<option value="${escapeHtml(c.name)}">${escapeHtml(c.name)} (${c.type})</option>`).join('');

    if (txSelect) txSelect.innerHTML = '<option value="">Select Category...</option>' + options;
    if (filterSelect) filterSelect.innerHTML = '<option value="">All Categories</option>' + options;
    if (editSelect) editSelect.innerHTML = options;
}

async function handleCreateCategory(e) {
    e.preventDefault();
    const name = document.getElementById('cat-name').value.trim();
    const type = document.getElementById('cat-type').value;

    try {
        await apiCall('/api/categories', {
            method: 'POST',
            body: { name, type }
        });
        showAlert('Custom category created successfully!');
        document.getElementById('cat-name').value = '';
        loadCategories();
    } catch (err) {
        showAlert(err.message, true);
    }
}

async function handleDeleteCategory(name) {
    if (!confirm(`Are you sure you want to delete custom category "${name}"?`)) return;

    try {
        const res = await apiCall(`/api/categories/${encodeURIComponent(name)}`, { method: 'DELETE' });
        showAlert(res.message);
        loadCategories();
        loadTransactions();
    } catch (err) {
        showAlert(err.message, true);
    }
}

// ================= TRANSACTIONS =================
async function loadTransactions(filterParams = '') {
    try {
        const url = '/api/transactions' + (filterParams ? '?' + filterParams : '');
        const data = await apiCall(url);
        transactionsList = data.transactions || [];
        renderTransactionsTable();
        updateKPIs();
    } catch (err) {
        showAlert('Error loading transactions: ' + err.message, true);
    }
}

function renderTransactionsTable() {
    const tbody = document.getElementById('transactions-table-body');
    const countEl = document.getElementById('tx-count');
    if (countEl) countEl.textContent = `${transactionsList.length} entries`;

    if (!tbody) return;

    if (transactionsList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center">No transactions match your search.</td></tr>';
        return;
    }

    tbody.innerHTML = transactionsList.map(t => `
        <tr>
            <td>${t.date}</td>
            <td><strong>${escapeHtml(t.category)}</strong></td>
            <td><span class="badge ${t.type === 'INCOME' ? 'badge-income' : 'badge-expense'}">${t.type}</span></td>
            <td>${escapeHtml(t.description || '-')}</td>
            <td style="font-weight: 600; color: ${t.type === 'INCOME' ? 'var(--success)' : 'var(--danger)'}">
                ${t.type === 'INCOME' ? '+' : '-'}$${Number(t.amount).toFixed(2)}
            </td>
            <td>
                <button class="btn btn-outline btn-sm" onclick="openEditModal(${t.id})">Edit</button>
                <button class="btn btn-danger btn-sm" onclick="handleDeleteTransaction(${t.id})">Delete</button>
            </td>
        </tr>
    `).join('');
}

function updateKPIs() {
    let income = 0;
    let expense = 0;

    transactionsList.forEach(t => {
        const val = Number(t.amount) || 0;
        if (t.type === 'INCOME') income += val;
        else if (t.type === 'EXPENSE') expense += val;
    });

    const net = income - expense;

    document.getElementById('kpi-income').textContent = `$${income.toFixed(2)}`;
    document.getElementById('kpi-expense').textContent = `$${expense.toFixed(2)}`;
    document.getElementById('kpi-balance').textContent = `$${net.toFixed(2)}`;
}

async function handleCreateTransaction(e) {
    e.preventDefault();
    const amount = parseFloat(document.getElementById('tx-amount').value);
    const date = document.getElementById('tx-date').value;
    const category = document.getElementById('tx-category').value;
    const description = document.getElementById('tx-description').value.trim();

    try {
        await apiCall('/api/transactions', {
            method: 'POST',
            body: { amount, date, category, description }
        });
        showAlert('Transaction recorded successfully!');
        document.getElementById('tx-amount').value = '';
        document.getElementById('tx-description').value = '';
        loadTransactions();
        loadGoals(); // Update goal calculations
    } catch (err) {
        showAlert(err.message, true);
    }
}

function handleFilterTransactions(e) {
    e.preventDefault();
    const start = document.getElementById('filter-start-date').value;
    const end = document.getElementById('filter-end-date').value;
    const cat = document.getElementById('filter-category').value;
    const type = document.getElementById('filter-type').value;

    const params = new URLSearchParams();
    if (start) params.append('startDate', start);
    if (end) params.append('endDate', end);
    if (cat) params.append('category', cat);
    if (type) params.append('type', type);

    loadTransactions(params.toString());
}

function resetFilters() {
    document.getElementById('filter-form').reset();
    loadTransactions();
}

function openEditModal(id) {
    const tx = transactionsList.find(t => t.id === id);
    if (!tx) return;

    document.getElementById('edit-tx-id').value = tx.id;
    document.getElementById('edit-tx-amount').value = tx.amount;
    document.getElementById('edit-tx-category').value = tx.category;
    document.getElementById('edit-tx-description').value = tx.description || '';

    document.getElementById('edit-tx-modal').classList.remove('hidden');
}

function closeEditModal() {
    document.getElementById('edit-tx-modal').classList.add('hidden');
}

async function submitEditTransaction(e) {
    e.preventDefault();
    const id = document.getElementById('edit-tx-id').value;
    const amount = parseFloat(document.getElementById('edit-tx-amount').value);
    const category = document.getElementById('edit-tx-category').value;
    const description = document.getElementById('edit-tx-description').value.trim();

    try {
        await apiCall(`/api/transactions/${id}`, {
            method: 'PUT',
            body: { amount, category, description }
        });
        showAlert('Transaction updated successfully');
        closeEditModal();
        loadTransactions();
        loadGoals();
    } catch (err) {
        showAlert(err.message, true);
    }
}

async function handleDeleteTransaction(id) {
    if (!confirm('Are you sure you want to delete this transaction?')) return;

    try {
        const res = await apiCall(`/api/transactions/${id}`, { method: 'DELETE' });
        showAlert(res.message);
        loadTransactions();
        loadGoals();
    } catch (err) {
        showAlert(err.message, true);
    }
}

// ================= SAVINGS GOALS =================
async function loadGoals() {
    try {
        const data = await apiCall('/api/goals');
        goalsList = data.goals || [];
        renderGoals();
    } catch (err) {
        showAlert('Error loading goals: ' + err.message, true);
    }
}

function renderGoals() {
    const container = document.getElementById('goals-container');
    if (!container) return;

    if (goalsList.length === 0) {
        container.innerHTML = '<div class="text-center text-muted" style="grid-column: 1 / -1;">No savings goals created yet.</div>';
        return;
    }

    container.innerHTML = goalsList.map(g => {
        const pct = Math.min(100, Math.max(0, g.progressPercentage || 0));
        return `
            <div class="goal-card">
                <div class="goal-header">
                    <span class="goal-name">${escapeHtml(g.goalName)}</span>
                    <span class="badge ${pct >= 100 ? 'badge-income' : 'badge-default'}">${pct.toFixed(1)}%</span>
                </div>
                <div class="progress-track">
                    <div class="progress-fill" style="width: ${pct}%"></div>
                </div>
                <div class="goal-details">
                    <span>Progress: <strong>$${Number(g.currentProgress).toFixed(2)}</strong></span>
                    <span>Target: <strong>$${Number(g.targetAmount).toFixed(2)}</strong></span>
                </div>
                <div class="goal-details">
                    <span>Remaining: $${Number(g.remainingAmount).toFixed(2)}</span>
                    <span>Target Date: ${g.targetDate}</span>
                </div>
                <div class="form-actions" style="margin-top: 1rem;">
                    <button class="btn btn-outline btn-sm" onclick="openEditGoalModal(${g.id})">Edit</button>
                    <button class="btn btn-danger btn-sm" onclick="handleDeleteGoal(${g.id})">Delete</button>
                </div>
            </div>
        `;
    }).join('');
}

async function handleCreateGoal(e) {
    e.preventDefault();
    const goalName = document.getElementById('goal-name').value.trim();
    const targetAmount = parseFloat(document.getElementById('goal-target').value);
    const targetDate = document.getElementById('goal-target-date').value;
    const startDate = document.getElementById('goal-start-date').value || null;

    try {
        await apiCall('/api/goals', {
            method: 'POST',
            body: { goalName, targetAmount, targetDate, startDate }
        });
        showAlert('Savings goal established successfully!');
        document.getElementById('goal-name').value = '';
        document.getElementById('goal-target').value = '';
        loadGoals();
    } catch (err) {
        showAlert(err.message, true);
    }
}

function openEditGoalModal(id) {
    const goal = goalsList.find(g => g.id === id);
    if (!goal) return;

    document.getElementById('edit-goal-id').value = goal.id;
    document.getElementById('edit-goal-target').value = goal.targetAmount;
    document.getElementById('edit-goal-date').value = goal.targetDate;

    document.getElementById('edit-goal-modal').classList.remove('hidden');
}

function closeEditGoalModal() {
    document.getElementById('edit-goal-modal').classList.add('hidden');
}

async function submitEditGoal(e) {
    e.preventDefault();
    const id = document.getElementById('edit-goal-id').value;
    const targetAmount = parseFloat(document.getElementById('edit-goal-target').value);
    const targetDate = document.getElementById('edit-goal-date').value;

    try {
        await apiCall(`/api/goals/${id}`, {
            method: 'PUT',
            body: { targetAmount, targetDate }
        });
        showAlert('Savings goal updated successfully');
        closeEditGoalModal();
        loadGoals();
    } catch (err) {
        showAlert(err.message, true);
    }
}

async function handleDeleteGoal(id) {
    if (!confirm('Are you sure you want to delete this savings goal?')) return;

    try {
        const res = await apiCall(`/api/goals/${id}`, { method: 'DELETE' });
        showAlert(res.message);
        loadGoals();
    } catch (err) {
        showAlert(err.message, true);
    }
}

// ================= REPORTS =================
function toggleReportType() {
    const isMonthly = document.querySelector('input[name="reportType"]:checked').value === 'monthly';
    const monthGroup = document.getElementById('month-select-group');
    if (monthGroup) {
        if (isMonthly) monthGroup.classList.remove('hidden');
        else monthGroup.classList.add('hidden');
    }
}

async function loadReport() {
    const isMonthly = document.querySelector('input[name="reportType"]:checked').value === 'monthly';
    const year = parseInt(document.getElementById('rep-year').value);
    const month = parseInt(document.getElementById('rep-month').value);

    const url = isMonthly ? `/api/reports/monthly/${year}/${month}` : `/api/reports/yearly/${year}`;

    try {
        const data = await apiCall(url);
        renderReport(data, isMonthly, year, month);
    } catch (err) {
        showAlert('Error generating report: ' + err.message, true);
    }
}

function renderReport(data, isMonthly, year, month) {
    const out = document.getElementById('report-output');
    out.classList.remove('hidden');

    document.getElementById('rep-title').textContent = isMonthly 
        ? `Net Savings for ${month}/${year}` 
        : `Net Savings for Full Year ${year}`;

    const netEl = document.getElementById('rep-net-savings');
    netEl.textContent = `$${Number(data.netSavings).toFixed(2)}`;
    netEl.style.color = Number(data.netSavings) >= 0 ? 'var(--success)' : 'var(--danger)';

    const incomeTbody = document.getElementById('rep-income-table');
    const expenseTbody = document.getElementById('rep-expense-table');

    const incomeKeys = Object.keys(data.totalIncome || {});
    incomeTbody.innerHTML = incomeKeys.length > 0
        ? incomeKeys.map(k => `<tr><td>${escapeHtml(k)}</td><td class="text-right">$${Number(data.totalIncome[k]).toFixed(2)}</td></tr>`).join('')
        : '<tr><td colspan="2" class="text-center text-muted">No income recorded</td></tr>';

    const expenseKeys = Object.keys(data.totalExpenses || {});
    expenseTbody.innerHTML = expenseKeys.length > 0
        ? expenseKeys.map(k => `<tr><td>${escapeHtml(k)}</td><td class="text-right">$${Number(data.totalExpenses[k]).toFixed(2)}</td></tr>`).join('')
        : '<tr><td colspan="2" class="text-center text-muted">No expenses recorded</td></tr>';
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
