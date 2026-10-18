let adminGroomingServices = [];
let groomingServicePreviewURL = null;
let currentGroomingService = null;
let groomingServiceActionPending = false;
let groomingServiceLoadVersion = 0;
let groomingServicesLoaded = false;

async function loadGroomingServicesAdmin() {
    const version = ++groomingServiceLoadVersion;
    groomingServicesLoaded = false;
    const table = document.getElementById('groomingServiceTable');
    table.innerHTML = '<tr><td colspan="5">Loading services...</td></tr>';
    try {
        const services = await api.get('/grooming/services/manage');
        if (version !== groomingServiceLoadVersion) return;
        adminGroomingServices = services;
        groomingServicesLoaded = true;
        document.getElementById('groomingServiceError').textContent = '';
        renderGroomingServicesAdmin();
    } catch (error) {
        if (version !== groomingServiceLoadVersion) return;
        adminGroomingServices = [];
        document.getElementById('groomingServiceSummary').textContent = 'Services could not be loaded.';
        table.innerHTML = '<tr><td colspan="5">Could not load services. <button class="btn btn-sm btn-pawcare-outline" onclick="loadGroomingServicesAdmin()">Retry</button></td></tr>';
        document.getElementById('groomingServiceError').textContent = error.message;
    }
}

function renderGroomingServicesAdmin() {
    if (!groomingServicesLoaded) return;
    const search = document.getElementById('groomingServiceSearch').value.trim().toLowerCase();
    const filter = document.getElementById('groomingServiceFilter').value;
    const sort = document.getElementById('groomingServiceSort').value;
    const services = adminGroomingServices.filter(s => (!search || `${s.name} ${s.description || ''}`.toLowerCase().includes(search)) && (filter === 'ALL' || (filter === 'ACTIVE' ? s.isActive : !s.isActive)));
    services.sort((a,b) => sort === 'PRICE_ASC' ? a.price-b.price : sort === 'PRICE_DESC' ? b.price-a.price : sort === 'DURATION' ? a.durationMinutes-b.durationMinutes : a.name.localeCompare(b.name));
    const active = adminGroomingServices.filter(s => s.isActive).length;
    document.getElementById('groomingServiceSummary').textContent = `${services.length} shown · ${adminGroomingServices.length} total · ${active} active · ${adminGroomingServices.length-active} inactive`;
    document.getElementById('groomingServiceTable').innerHTML = services.map(s => `<tr><td><div class="d-flex align-items-center gap-3"><img class="grooming-admin-photo" src="${escapeHtml(groomingServiceImage(s))}" alt="${escapeHtml(s.name)}" onerror="this.onerror=null;this.src='/images/grooming.png'"><div><strong>${escapeHtml(s.name)}</strong><p class="small text-muted mb-0" style="white-space:pre-wrap">${escapeHtml(s.description || '')}</p></div></div></td><td>${s.durationMinutes} min</td><td>${formatCurrency(s.price)}</td><td><span class="badge ${s.isActive ? 'bg-success' : 'bg-secondary'}">${s.isActive ? 'Active' : 'Inactive'}</span></td><td><div class="service-actions"><button class="btn btn-sm btn-pawcare-outline" onclick="editGroomingService(${s.id})">Edit</button><button class="btn btn-sm btn-outline-secondary" onclick="toggleGroomingService(${s.id})">${s.isActive ? 'Deactivate' : 'Activate'}</button><button class="btn btn-sm btn-outline-danger" onclick="deleteGroomingService(${s.id})">Delete</button></div></td></tr>`).join('') || `<tr><td colspan="5">${adminGroomingServices.length ? 'No services match your search or status filter.' : 'No grooming services yet. Add your first service.'}</td></tr>`;
    document.querySelectorAll('#groomingServiceTable button').forEach(button => button.disabled = groomingServiceActionPending);
}

function editGroomingService(id) {
    if (groomingServiceActionPending) return;
    const service = id ? adminGroomingServices.find(s => s.id === id) : null;
    if (id && !service) return;
    currentGroomingService = service;
    clearGroomingImagePreview();
    let modal = document.getElementById('groomingServiceModal');
    if (!modal) {
        document.body.insertAdjacentHTML('beforeend', `<div class="modal fade" id="groomingServiceModal" tabindex="-1" aria-labelledby="groomingServiceTitle"><div class="modal-dialog"><div class="modal-content"><div class="modal-header"><h5 id="groomingServiceTitle" class="modal-title"></h5><button class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div><form id="groomingServiceForm"><div class="modal-body">
            <div class="mb-3"><label for="serviceImage" class="form-label">Service image (optional)</label><img id="serviceImagePreview" class="grooming-service-photo-preview" src="/images/grooming.png" alt="Service image preview" onerror="this.onerror=null;this.src='/images/grooming.png'"><input id="serviceImage" class="form-control" type="file" accept="image/jpeg,image/png" onchange="previewGroomingServiceImage(this)"><small class="text-muted d-block mt-1">JPEG or PNG, up to 5 MB. Choose a photo to add or replace this service's image.</small><div id="serviceRemoveImageGroup" class="form-check mt-2"><input id="serviceRemoveImage" class="form-check-input" type="checkbox" onchange="toggleGroomingImageRemoval()"><label for="serviceRemoveImage" class="form-check-label">Remove uploaded image and use the default</label></div></div>
            <div class="mb-3"><label for="serviceName" class="form-label">Service name</label><input id="serviceName" class="form-control" required maxlength="200"></div>
            <div class="mb-3"><label for="serviceDescription" class="form-label">Description</label><textarea id="serviceDescription" class="form-control" rows="3" maxlength="5000"></textarea></div>
            <div class="mb-3"><label for="serviceDuration" class="form-label">Duration (minutes)</label><input id="serviceDuration" class="form-control" type="number" min="1" max="480" step="1" required></div>
            <div class="mb-3"><label for="servicePrice" class="form-label">Price (Rs.)</label><input id="servicePrice" class="form-control" type="number" min="0" max="99999999.99" step="0.01" required></div>
            <div class="form-check"><input id="serviceActive" type="checkbox" class="form-check-input"><label for="serviceActive" class="form-check-label">Active — available for new bookings</label></div>
            <p id="serviceSaveResult" class="text-danger mt-3 mb-0" role="alert"></p>
            </div><div class="modal-footer"><button type="button" class="btn btn-pawcare-outline" data-bs-dismiss="modal">Cancel</button><button id="serviceSaveButton" type="submit" class="btn btn-pawcare">Save service</button></div></form></div></div></div>`);
        modal = document.getElementById('groomingServiceModal');
        document.getElementById('groomingServiceForm').addEventListener('submit', saveGroomingService);
        modal.addEventListener('hide.bs.modal', event => { if (document.getElementById('serviceSaveButton').disabled) event.preventDefault(); });
        modal.addEventListener('show.bs.modal', () => { modal.dataset.ready = 'false'; });
        modal.addEventListener('shown.bs.modal', () => { modal.dataset.ready = 'true'; });
        modal.addEventListener('hidden.bs.modal', () => { modal.dataset.ready = 'false'; });
        modal.addEventListener('hidden.bs.modal', clearGroomingImagePreview);
    }
    const form = document.getElementById('groomingServiceForm');
    form.dataset.serviceId = id || '';
    form.reset();
    document.getElementById('groomingServiceTitle').textContent = id ? 'Edit grooming service' : 'Add grooming service';
    document.getElementById('serviceName').value = service?.name || '';
    const preview = document.getElementById('serviceImagePreview');
    preview.src = groomingServiceImage(service || {name:''});
    preview.onerror = () => { preview.onerror = null; preview.src = '/images/grooming.png'; };
    document.getElementById('serviceRemoveImageGroup').hidden = !service?.image;
    document.getElementById('serviceDescription').value = service?.description || '';
    document.getElementById('serviceDuration').value = service?.durationMinutes ?? 30;
    document.getElementById('servicePrice').value = service?.price ?? '';
    document.getElementById('serviceActive').checked = service?.isActive ?? true;
    document.getElementById('serviceSaveResult').textContent = '';
    bootstrap.Modal.getOrCreateInstance(modal).show();
}

async function saveGroomingService(event) {
    event.preventDefault();
    const form = event.target, button = document.getElementById('serviceSaveButton');
    if (button.disabled || !form.reportValidity()) return;
    const name = document.getElementById('serviceName').value.trim();
    if (!name) { document.getElementById('serviceSaveResult').textContent = 'Enter a service name.'; return; }
    button.disabled = true;
    document.getElementById('serviceSaveResult').textContent = '';
    try {
        const data = {name, description: document.getElementById('serviceDescription').value.trim(), durationMinutes: Number(document.getElementById('serviceDuration').value), price: Number(document.getElementById('servicePrice').value), isActive: document.getElementById('serviceActive').checked, removeImage: document.getElementById('serviceRemoveImage').checked};
        const image = document.getElementById('serviceImage').files?.[0];
        const payload = multipart('service', data, 'image', image);
        const id = form.dataset.serviceId;
        if (id) await api.put('/grooming/services/' + id, payload);
        else await api.post('/grooming/services', payload);
        button.disabled = false;
        hideGroomingServiceEditor();
        showToast('Grooming service saved.', 'success');
        await loadGroomingServicesAdmin();
    } catch (error) {
        document.getElementById('serviceSaveResult').textContent = error.message;
    } finally { button.disabled = false; }
}

async function toggleGroomingService(id) {
    const service = adminGroomingServices.find(s => s.id === id);
    if (!service || groomingServiceActionPending) return;
    const action = service.isActive ? 'Deactivate' : 'Activate';
    if (!await pawConfirm(`${action} “${service.name}”? ${service.isActive ? 'It will no longer appear for new bookings. Existing visits stay available.' : 'Pet owners will be able to book this service.'}`, {title: `${action} service?`, confirmText: action})) return;
    await runGroomingServiceAction(async () => {
        await api.put('/grooming/services/' + id, {name:service.name, description:service.description, durationMinutes:service.durationMinutes, price:service.price, isActive:!service.isActive});
        showToast(`Service ${service.isActive ? 'deactivated' : 'activated'}.`, 'success');
    });
}

async function deleteGroomingService(id) {
    const service = adminGroomingServices.find(s => s.id === id);
    if (!service || groomingServiceActionPending) return;
    if (!await pawConfirm(`Delete “${service.name}” from the service catalogue? It will be removed from this list and new bookings. Existing appointments and reviews will be kept.`, {title:'Delete grooming service?', type:'warning', confirmText:'Delete service'})) return;
    await runGroomingServiceAction(async () => {
        await api.delete('/grooming/services/' + id);
        showToast('Grooming service deleted.', 'success');
    });
}

async function runGroomingServiceAction(action) {
    if (groomingServiceActionPending) return;
    groomingServiceActionPending = true;
    document.getElementById('groomingServiceError').textContent = '';
    renderGroomingServicesAdmin();
    try { await action(); await loadGroomingServicesAdmin(); }
    catch (error) { document.getElementById('groomingServiceError').textContent = error.message; }
    finally { groomingServiceActionPending = false; renderGroomingServicesAdmin(); }
}

function hideGroomingServiceEditor() {
    const element = document.getElementById('groomingServiceModal');
    const modal = bootstrap.Modal.getInstance(element);
    if (element.dataset.ready === 'true') modal.hide();
    else element.addEventListener('shown.bs.modal', () => modal.hide(), {once:true});
}

function clearGroomingImagePreview() {
    if (groomingServicePreviewURL) URL.revokeObjectURL(groomingServicePreviewURL);
    groomingServicePreviewURL = null;
}

function previewGroomingServiceImage(input) {
    const file = input.files?.[0];
    const preview = document.getElementById('serviceImagePreview');
    clearGroomingImagePreview();
    document.getElementById('serviceSaveResult').textContent = '';
    if (!file) { preview.src = groomingServiceImage(currentGroomingService || {name:''}); return; }
    if (!['image/jpeg','image/png'].includes(file.type) || file.size > 5*1024*1024) {
        input.value = '';
        preview.src = groomingServiceImage(currentGroomingService || {name:''});
        document.getElementById('serviceSaveResult').textContent = 'Choose a JPEG or PNG image up to 5 MB.';
        return;
    }
    document.getElementById('serviceRemoveImage').checked = false;
    groomingServicePreviewURL = URL.createObjectURL(file);
    preview.src = groomingServicePreviewURL;
}

function toggleGroomingImageRemoval() {
    clearGroomingImagePreview();
    document.getElementById('serviceImage').value = '';
    document.getElementById('serviceImagePreview').src = groomingServiceImage(document.getElementById('serviceRemoveImage').checked ? {name:currentGroomingService?.name || ''} : currentGroomingService || {name:''});
}
