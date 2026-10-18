/* PawCare - API Module */
const API_BASE = '/api';
// Storage can be unavailable in private browsers, or contain data from an older build.
const authStorage = {
    get(key) { try { return localStorage.getItem(key); } catch (_) { return null; } },
    set(key, value) { try { localStorage.setItem(key, value); } catch (_) {} },
    remove(key) { try { localStorage.removeItem(key); } catch (_) {} }
};
function readSavedUser() {
    try {
        const saved = JSON.parse(authStorage.get('pawcare_user') || 'null');
        return saved && typeof saved === 'object' && !Array.isArray(saved) ? saved : null;
    } catch (_) { authStorage.remove('pawcare_user'); return null; }
}

const api = {
    token: authStorage.get('pawcare_token'),
    user: readSavedUser(),

    setAuth(data) {
        if (!data || typeof data.token !== 'string' || !data.token || !data.role) {
            throw new Error('Sign-in could not be completed. Please try again.');
        }
        this.token = data.token;
        this.user = { id: data.userId, email: data.email, firstName: data.firstName, lastName: data.lastName, role: data.role };
        authStorage.set('pawcare_token', data.token);
        authStorage.set('pawcare_user', JSON.stringify(this.user));
    },

    clearAuth() {
        this.token = null;
        this.user = null;
        authStorage.remove('pawcare_token');
        authStorage.remove('pawcare_user');
    },

    isLoggedIn() { return typeof this.token === 'string' && !!this.token && !['null','undefined'].includes(this.token); },
    getRole() { return this.user?.role; },

    isSharedPreview() { return location.hostname.endsWith('.devtunnels.ms'); },

    headers() {
        const h = { 'Accept': 'application/json', 'Content-Type': 'application/json' };
        if (this.isSharedPreview()) h['X-Tunnel-Skip-AntiPhishing-Page'] = 'true';
        if (this.isLoggedIn()) h['Authorization'] = `Bearer ${this.token}`;
        return h;
    },

    async request(method, url, body = null, extraHeaders = {}) {
        const sentToken = this.token;
        const opts = { method, headers: { ...this.headers(), ...extraHeaders }, credentials: 'same-origin', cache: 'no-store' };
        if (body instanceof FormData) { delete opts.headers['Content-Type']; opts.body = body; }
        else if (body !== null) opts.body = JSON.stringify(body);
        let res;
        try { res = await fetch(API_BASE + url, opts); }
        catch (_) {
            const error = new Error('Could not reach PawCare. Check your connection and try again.');
            error.status = 0; throw error;
        }
        if (res.status === 204) return null;
        const raw = await res.text();
        let data = {};
        try { data = raw ? JSON.parse(raw) : {}; }
        catch (_) {
            const error = new Error(this.isSharedPreview()
                ? 'The shared preview returned a sign-in or access page. Open the shared site link in this browser, complete its access screen, then try again.'
                : 'PawCare returned an unexpected response. Please try again.');
            error.status = res.status; error.unexpectedResponse = true; throw error;
        }
        if (res.status === 401 && !url.startsWith('/auth/')) {
            // A delayed response from an old session must not sign out a newly signed-in user.
            if (this.token === sentToken || !this.isLoggedIn()) {
                this.clearAuth();
                if (!location.pathname.endsWith('/login.html')) window.location.href = '/pages/login.html';
            }
            const error = new Error('Your session has expired. Please sign in again.');
            error.status = 401; throw error;
        }
        if (!res.ok) {
            const error = new Error(data?.errors ? Object.values(data.errors).join('; ') : data?.message || data?.error || 'Request failed. Please try again.');
            error.fields = data?.errors || {}; error.status = res.status; throw error;
        }
        return data;
    },

    requestConfirmed(method, url, body, password) {
        return this.request(method, url, body, { 'X-Confirm-Password': password || '' });
    },

    get(url) { return this.request('GET', url); },
    post(url, body) { return this.request('POST', url, body); },
    put(url, body) { return this.request('PUT', url, body); },
    delete(url) { return this.request('DELETE', url); },
    putConfirmed(url, body, password) { return this.requestConfirmed('PUT', url, body, password); },
    deleteConfirmed(url, password) { return this.requestConfirmed('DELETE', url, null, password); },

    // Auth
    login: (data) => api.post('/auth/login', data),
    register: (data) => api.post('/auth/register', data),
    me: () => api.get('/auth/me'),
    confirmPassword: (password) => api.post('/auth/confirm-password', {password}),

    // Pets
    getMyPets: () => api.get('/pets/my'),
    getPet: (id) => api.get(`/pets/${id}`),
    createPet: (data, photo) => api.post('/pets', multipart('pet',data,'photo',photo)),
    updatePet: (id, data, photo) => api.put(`/pets/${id}`, multipart('pet',data,'photo',photo)),
    updatePetConfirmed: (id, data, photo, password) => api.putConfirmed(`/pets/${id}`, multipart('pet',data,'photo',photo), password),
    deletePet: (id) => api.delete(`/pets/${id}`),
    deletePetConfirmed: (id, password) => api.deleteConfirmed(`/pets/${id}`, password),
    getPetPassport: (id) => api.get(`/pets/${id}/passport`),
    updatePetPassport: (id, data) => api.put(`/pets/${id}/passport`, data),
    getPublicPetPassport: (token) => api.get(`/passports/${encodeURIComponent(token)}`),

    // Species & Breeds
    getSpecies: () => api.get('/species'),
    getBreeds: (speciesId) => api.get(`/breeds/species/${speciesId}`),

    // Appointments
    getMyAppointments: (page = 0) => api.get(`/appointments/my?page=${page}`),
    getAvailableSlots: (providerId, date, kind, excludeId = null, durationMinutes = null) => api.get(`/availability/slots?providerId=${providerId}&date=${encodeURIComponent(date)}&kind=${encodeURIComponent(kind)}${excludeId ? '&excludeId=' + excludeId : ''}${durationMinutes ? '&durationMinutes=' + encodeURIComponent(durationMinutes) : ''}`),
    getProviderAvailability: (providerId) => api.get(`/availability/provider/${providerId}`),
    saveMyAvailability: (data) => api.put('/availability/me', data),
    getMyUnavailableDates: () => api.get('/availability/me/unavailable-dates'),
    addMyUnavailableDate: (data) => api.post('/availability/me/unavailable-dates', data),
    deleteMyUnavailableDate: (id) => api.delete(`/availability/me/unavailable-dates/${id}`),
    getVetAppointments: (page = 0) => api.get(`/appointments/vet?page=${page}`),
    getAppointment: (id) => api.get(`/appointments/${id}`),
    getAllAppointments: (page = 0) => api.get(`/appointments?page=${page}`),
    createAppointment: (data) => api.post('/appointments', data),
    updateAppointment: (id, data) => api.put(`/appointments/${id}`, data),
    updateAppointmentConfirmed: (id, data, password) => api.putConfirmed(`/appointments/${id}`, data, password),
    deleteAppointment: (id) => api.delete(`/appointments/${id}`),

    // Medical Records
    getMedicalRecords: (petId) => api.get(`/medical-records/pet/${petId}`),
    getVetRecords: (page = 0) => api.get(`/medical-records/vet?page=${page}`),
    createMedicalRecord: (data) => api.post('/medical-records', data),
    searchMedicalRecords: (params='') => api.get(`/medical-records/search${params ? '?' + params : ''}`),
    getMedicalSummary: (petId) => api.get(`/medical-records/pet/${petId}/ai-summary`),
    explainMedicalTerm: (term) => api.get(`/medical-records/explain?term=${encodeURIComponent(term)}`),
    getMedicalAttachments: (recordId) => api.get(`/medical-records/${recordId}/attachments`),
    addMedicalAttachment: (recordId,file) => { const f=new FormData(); f.append('file',file); return api.post(`/medical-records/${recordId}/attachments`,f); },
    getMessages: () => api.get('/messages'),
    getConversations: (search='') => api.get(`/messages/conversations${search ? '?search='+encodeURIComponent(search) : ''}`),
    getConversation: (conversationId) => api.get(`/messages/conversations/${conversationId}`),
    markConversationRead: (conversationId) => api.put(`/messages/conversations/${conversationId}/read`),
    getMessageThread: (petId,otherUserId) => api.get(`/messages/thread?petId=${petId}&otherUserId=${otherUserId}`),
    sendMessage: (petId,recipientId,appointmentId,message,file) => { const f=new FormData(); f.append('petId',petId); f.append('recipientId',recipientId); if(appointmentId)f.append('appointmentId',appointmentId); if(message)f.append('message',message); if(file)f.append('file',file); return api.post('/messages',f); },
    getStoreAnalytics: () => api.get('/analytics/store'),

    // Care professionals
    getVeterinarians: () => api.get('/veterinarians'),
    getGroomers: () => api.get('/groomers'),

    // Grooming
    getGroomingServices: () => api.get('/grooming/services'),
    getMyGrooming: (page = 0) => api.get(`/grooming/appointments/my?page=${page}`),
    getGroomerAppointments: (page = 0) => api.get(`/grooming/appointments/groomer?page=${page}`),
    getAllGrooming: (page = 0) => api.get(`/grooming/appointments?page=${page}`),
    createGrooming: (data) => api.post('/grooming/appointments', data),
    updateGrooming: (id, data) => api.put(`/grooming/appointments/${id}`, data),
    updateGroomingConfirmed: (id, data, password) => api.putConfirmed(`/grooming/appointments/${id}`, data, password),


    // Products
    getProducts: (page = 0, category = null, search = null) => {
        let url = `/products?page=${page}&size=12`;
        if (category) url += `&category=${category}`;
        if (search) url += `&search=${encodeURIComponent(search)}`;
        return api.get(url);
    },
    getFeaturedProducts: () => api.get('/products/featured'),
    getProduct: (id) => api.get(`/products/${id}`),
    createProduct: (data, image) => api.post('/products', multipart('product',data,'image',image)),
    updateProduct: (id, data, image) => api.put(`/products/${id}`, multipart('product',data,'image',image)),
    getCategories: () => api.get('/categories'),

    // Cart
    getCart: () => api.get('/cart'),
    addToCart: (productId, quantity = 1) => api.post('/cart/add', { productId, quantity }),
    updateCartItem: (itemId, quantity) => api.put(`/cart/items/${itemId}`, { quantity }),
    removeCartItem: (itemId) => api.delete(`/cart/items/${itemId}`),

    // Orders
    checkout: (shippingAddress, productId, quantity, city = null, postalCode = null, paymentMethod = 'CREDIT_CARD') => api.post('/orders/checkout', { shippingAddress, city, postalCode, paymentMethod, productId, quantity }),
    getOrder: (id) => api.get(`/orders/${id}`),
    getMyOrders: (page = 0) => api.get(`/orders/my?page=${page}`),
    getAllOrders: (page = 0) => api.get(`/orders?page=${page}`),
    updateOrderStatus: (id, status) => api.put(`/orders/${id}/status`, { status }),

    // Reviews
    getMyReviews: (page = 0) => api.get(`/reviews/mine?page=${page}`),
    getMyServiceReview: (appointmentId) => api.get(`/reviews/service/${appointmentId}/mine`),
    createServiceReview: (appointmentId,data) => api.post(`/reviews/service/${appointmentId}`,data),
    getMyProductReview: (orderId,productId) => api.get(`/reviews/product/${orderId}/${productId}/mine`),
    createProductReview: (orderId,productId,data) => api.post(`/reviews/product/${orderId}/${productId}`,data),
    updateReview: (id,data) => api.put(`/reviews/${id}`,data),
    deleteReview: (id) => api.delete(`/reviews/${id}`),
    getProductReviews: (productId) => api.get(`/reviews/product/${productId}`),
    getReviewModeration: (status='ALL',page=0) => api.get(`/reviews/moderation?status=${encodeURIComponent(status)}&page=${page}`),
    moderateReview: (id,status) => api.put(`/reviews/${id}/moderation`,{status}),
    setReviewHidden: (id,hidden) => api.put(`/reviews/${id}/visibility?hidden=${hidden}`),
    reportReview: (id,reason) => api.post(`/reviews/${id}/report`,{reason}),
    getStoreProductReviews: (page=0) => api.get(`/reviews/store-products?page=${page}&size=20`),
    deleteReviewAsAdmin: (id) => api.delete(`/reviews/admin/${id}`),

    // Payments
    processPayment: (orderId, paymentMethod = 'SIMULATED', card = {}) => api.post('/payments/process', { orderId: orderId.toString(), paymentMethod, ...card }),
    getMyPayments: (page = 0) => api.get(`/payments/my?page=${page}`),
    getAllPayments: (page = 0) => api.get(`/payments?page=${page}&size=20`),
    updatePaymentStatus: (id, status) => api.put(`/payments/${id}/status`, { status }),

    // Notifications
    getNotifications: (page = 0) => api.get(`/notifications?page=${page}`),
    getUnreadCount: () => api.get('/notifications/unread-count'),
    markAsRead: (id) => api.put(`/notifications/${id}/read`),
    markAllNotificationsRead: () => api.put('/notifications/read-all'),
    deleteNotification: (id) => api.delete(`/notifications/${id}`),

    // PawCare Assistant
    askAssistant: (question, petId = null) => api.post('/assistant/ask', { question, petId }),

    // Dashboard
    getAdminDashboard: () => api.get('/dashboard/admin'),
    getOwnerDashboard: () => api.get('/dashboard/owner'),
    getServiceDashboard: () => api.get('/dashboard/service'),
    getManagementSummary: () => api.get('/dashboard/management'),

    // Users (Admin)
    getAllUsers: (page = 0, search = '') => api.get(`/users?page=${page}${search ? '&search=' + encodeURIComponent(search) : ''}`),
    getUsersByRole: (role, page = 0) => api.get(`/users/role/${role}?page=${page}`),
    uploadMyProfileImage: (image) => { const f = new FormData(); f.append('image', image); return api.post('/users/me/profile-image', f); },
    removeMyProfileImage: () => api.delete('/users/me/profile-image'),
    deleteUser: (id) => api.delete(`/users/${id}`),
};

/* Toast notification */
function showToast(message, type = 'info') {
    let container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    const icons = { success: 'bi-check-circle-fill', error: 'bi-x-circle-fill', warning: 'bi-exclamation-triangle-fill', info: 'bi-info-circle-fill' };
    const toast = document.createElement('div');
    toast.className = `toast-pawcare toast-${type}`;
    toast.innerHTML = `<i class="bi ${icons[type] || icons.info}" style="font-size:1.2rem"></i><span>${escapeHtml(message)}</span>`;
    container.appendChild(toast);
    setTimeout(() => { toast.style.opacity = '0'; toast.style.transform = 'translateX(100%)'; setTimeout(() => toast.remove(), 300); }, 4000);
}

/* Theme-native async dialogs. Replaces browser alert()/confirm() so PawCare stays visually consistent. */
function pawDialog(message, options = {}) {
    const settings = {
        title: 'PawCare',
        type: 'info',
        confirmText: 'OK',
        cancelText: null,
        ...options
    };
    const iconMap = {
        success: 'bi-check-circle-fill',
        warning: 'bi-exclamation-triangle-fill',
        danger: 'bi-trash3-fill',
        error: 'bi-x-octagon-fill',
        ai: 'bi-stars',
        info: 'bi-heart-pulse-fill'
    };
    const dialogType = Object.prototype.hasOwnProperty.call(iconMap, settings.type) ? settings.type : 'info';
    return new Promise(resolve => {
        const layer = document.createElement('div');
        layer.className = 'paw-dialog-layer';
        layer.setAttribute('role', 'presentation');
        layer.innerHTML = `<div class="paw-dialog-card" role="dialog" aria-modal="true" aria-labelledby="paw-dialog-title">
            <div class="paw-dialog-icon paw-dialog-${dialogType}"><i class="bi ${iconMap[dialogType]}"></i></div>
            <button type="button" class="paw-dialog-close" aria-label="Close"><i class="bi bi-x-lg"></i></button>
            <div class="paw-dialog-copy">
                <div class="paw-dialog-kicker">PawCare</div>
                <h3 id="paw-dialog-title"></h3>
                <p class="paw-dialog-message"></p>
            </div>
            <div class="paw-dialog-actions"></div>
        </div>`;
        const title = layer.querySelector('#paw-dialog-title');
        const text = layer.querySelector('.paw-dialog-message');
        const actions = layer.querySelector('.paw-dialog-actions');
        title.textContent = settings.title || 'PawCare';
        text.textContent = String(message ?? '');

        let settled = false;
        const finish = value => {
            if (settled) return;
            settled = true;
            layer.classList.remove('is-open');
            document.removeEventListener('keydown', onKey);
            setTimeout(() => layer.remove(), 180);
            resolve(value);
        };
        const cancelValue = settings.cancelText ? false : true;
        layer.querySelector('.paw-dialog-close').addEventListener('click', () => finish(cancelValue));
        layer.addEventListener('click', event => { if (event.target === layer) finish(cancelValue); });
        const onKey = event => {
            if (event.key === 'Escape') finish(cancelValue);
            if (event.key === 'Tab') {
                const focusable = [...layer.querySelectorAll('button:not([disabled])')];
                if (!focusable.length) return;
                const first = focusable[0], last = focusable[focusable.length - 1];
                if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
                else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
            }
        };
        document.addEventListener('keydown', onKey);

        if (settings.cancelText) {
            const cancel = document.createElement('button');
            cancel.type = 'button'; cancel.className = 'btn btn-pawcare-outline'; cancel.textContent = settings.cancelText;
            cancel.addEventListener('click', () => finish(false));
            actions.appendChild(cancel);
        }
        const confirm = document.createElement('button');
        confirm.type = 'button';
        confirm.className = settings.type === 'danger' || settings.type === 'error' ? 'btn btn-danger paw-dialog-primary' : 'btn btn-pawcare paw-dialog-primary';
        confirm.textContent = settings.confirmText || 'OK';
        confirm.addEventListener('click', () => finish(true));
        actions.appendChild(confirm);

        document.body.appendChild(layer);
        requestAnimationFrame(() => layer.classList.add('is-open'));
        setTimeout(() => confirm.focus(), 70);
    });
}
function pawAlert(message, options = {}) { return pawDialog(message, { ...options, cancelText: null }); }
function pawConfirm(message, options = {}) { return pawDialog(message, { cancelText: 'Cancel', ...options }); }

async function openSecuredFile(url, fileName = 'attachment', download = false) {
    // Reserve the preview tab during the click; mobile browsers block tabs opened after fetch.
    const preview = download ? null : window.open('', '_blank');
    if (preview) preview.document.body.textContent = 'Loading document…';
    try {
        const headers = api.headers(); delete headers['Content-Type']; headers.Accept = 'application/pdf,image/*,application/octet-stream';
        const res = await fetch(url, {headers, credentials:'same-origin', cache:'no-store'});
        if (!res.ok || (res.headers.get('Content-Type') || '').includes('text/html')) throw new Error('Unable to open attachment. Please check your connection and try again.');
        const blob=await res.blob(), objectUrl=URL.createObjectURL(blob);
        if (preview) preview.location.href=objectUrl;
        else {const a=document.createElement('a');a.href=objectUrl;a.download=fileName;document.body.append(a);a.click();a.remove();}
        setTimeout(()=>URL.revokeObjectURL(objectUrl),60000);
    } catch (error) { preview?.close(); throw error; }
}

function formatCurrency(amount) { return 'Rs. ' + parseFloat(amount || 0).toFixed(2); }
function formatDate(date) {
    if (!date) return '-';
    return new Date(date).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
}
function statusBadge(status) {
    if (!status) return '';
    const cls = status.toLowerCase().replace(/_/g, '-');
    return `<span class="status-badge status-${cls}">${status.replace(/_/g, ' ')}</span>`;
}

async function updateNavbar() {
    const authBtns = document.getElementById('authButtons');
    const userMenu = document.getElementById('userMenu');
    if (!authBtns) return;
    let authenticated = api.isLoggedIn();
    let verifiedOwner = false;
    const refreshingToken = api.token;
    if (authenticated) {
        try {
            const current = await api.me();
            if (api.token === refreshingToken && api.isLoggedIn()) {
                api.user = current;
                authStorage.set('pawcare_user', JSON.stringify(current));
                verifiedOwner = current?.role === 'PET_OWNER';
            }
            authenticated = api.isLoggedIn();
        } catch (error) {
            // Connectivity and server failures do not invalidate the account session.
            if (api.token === refreshingToken && error.status === 401 && !error.unexpectedResponse) api.clearAuth();
            authenticated = api.isLoggedIn();
        }
    }
    if (authenticated) {
        authBtns.style.setProperty('display', 'none', 'important');
        if (userMenu) {
            userMenu.style.display = 'block';
            const displayName = [api.user?.firstName, api.user?.lastName].filter(Boolean).join(' ') || api.user?.email || 'My Account';
            userMenu.querySelectorAll('.user-name').forEach(el => el.textContent = displayName);
            const dashLink = userMenu.querySelector('.dash-link');
            if (dashLink) {
                const dashPages = {ADMIN:'/pages/dashboard/admin.html',PET_OWNER:'/pages/dashboard/owner.html',VETERINARIAN:'/pages/dashboard/vet.html',GROOMER:'/pages/dashboard/groomer.html',STORE_MANAGER:'/pages/dashboard/store.html',SERVICE_STAFF:'/pages/dashboard/service.html'};
                dashLink.href = dashPages[api.getRole()] || '/';
                const dashboardButton=document.getElementById('mainDashboardLink');
                if(dashboardButton){dashboardButton.href=dashLink.href;dashboardButton.style.display='inline-flex';}
            }
        }
    } else { authBtns.style.setProperty('display', 'flex', 'important'); if (userMenu) userMenu.style.display = 'none'; const db=document.getElementById('mainDashboardLink');if(db)db.style.display='none'; }
    // Owner shortcut is a convenience UI, never an authorization boundary.
    window.PawCareOwnerShortcut?.sync(verifiedOwner && authenticated && api.getRole() === 'PET_OWNER');
}

function logout() { api.clearAuth(); window.location.href = '/'; }
document.addEventListener('DOMContentLoaded', updateNavbar);
window.addEventListener('storage', event => {
    if (!['pawcare_token','pawcare_user'].includes(event.key)) return;
    api.token = authStorage.get('pawcare_token');
    api.user = readSavedUser();
    updateNavbar();
});

function requestPasswordConfirmation(title, message = 'Enter your current account password to continue.', confirmLabel = 'Continue') {
    let modal = document.getElementById('passwordConfirmModal');
    if (!modal) {
        document.body.insertAdjacentHTML('beforeend', `<div class="modal fade" id="passwordConfirmModal" tabindex="-1" aria-hidden="true"><div class="modal-dialog modal-dialog-centered"><div class="modal-content"><div class="modal-header"><h5 id="passwordConfirmTitle" class="modal-title"></h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div><div class="modal-body"><p id="passwordConfirmMessage" class="text-muted small mb-3"></p><label for="passwordConfirmInput" class="form-label">Current password <span class="text-danger">*</span></label><input id="passwordConfirmInput" type="password" class="form-control form-control-pawcare" required autocomplete="current-password"><div id="passwordConfirmError" class="text-danger small mt-2" role="alert"></div></div><div class="modal-footer"><button type="button" class="btn btn-pawcare-outline" data-bs-dismiss="modal">Cancel</button><button type="button" id="passwordConfirmButton" class="btn btn-pawcare">Continue</button></div></div></div></div>`);
        modal = document.getElementById('passwordConfirmModal');
    }
    document.getElementById('passwordConfirmTitle').textContent = title;
    document.getElementById('passwordConfirmMessage').textContent = message;
    const input = document.getElementById('passwordConfirmInput');
    const error = document.getElementById('passwordConfirmError');
    const confirm = document.getElementById('passwordConfirmButton');
    input.value = ''; error.textContent = ''; confirm.textContent = confirmLabel;
    return new Promise(resolve => {
        const instance = bootstrap.Modal.getOrCreateInstance(modal);
        let settled = false;
        const finish = value => { if (settled) return; settled = true; resolve(value); };
        const hidden = () => { modal.removeEventListener('hidden.bs.modal', hidden); finish(null); };
        modal.addEventListener('hidden.bs.modal', hidden);
        confirm.onclick = async () => {
            const password = input.value;
            if (!password) { error.textContent = 'Password is required.'; input.focus(); return; }
            confirm.disabled = true; error.textContent = '';
            try {
                const result = await api.confirmPassword(password);
                if (!result.valid) { error.textContent = 'Incorrect password. Please try again.'; input.select(); return; }
                finish(password); instance.hide();
            } catch (e) { error.textContent = e.message || 'Unable to verify password.'; }
            finally { confirm.disabled = false; }
        };
        input.onkeydown = event => { if (event.key === 'Enter') { event.preventDefault(); confirm.click(); } };
        instance.show(); setTimeout(() => input.focus(), 180);
    });
}
