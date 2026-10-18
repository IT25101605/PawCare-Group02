(() => {
  const modalHtml = `<div class="modal fade" id="secureMessageModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-xl modal-dialog-scrollable"><div class="modal-content">
      <div class="modal-header"><div><h5 class="modal-title mb-0">Messages <span id="secureMessageUnread" class="badge bg-danger ms-1" hidden>0</span></h5><small class="text-muted">Private owner ↔ veterinarian conversations</small></div><button class="btn-close" data-bs-dismiss="modal"></button></div>
      <div class="modal-body p-0"><div class="row g-0" style="min-height:540px">
        <aside class="col-md-4 border-end p-3">
          <div class="input-group mb-3"><span class="input-group-text"><i class="bi bi-search"></i></span><input id="secureConversationSearch" class="form-control" maxlength="100" placeholder="Search conversations"></div>
          <div id="secureConversationList" class="d-grid gap-2" style="max-height:450px;overflow:auto"></div>
        </aside>
        <section class="col-md-8 d-flex flex-column">
          <div id="secureMessageHeader" class="p-3 border-bottom"><strong>Select a conversation</strong><div class="small text-muted">Choose a conversation or use Message from an appointment.</div></div>
          <div id="secureMessageList" class="p-3 flex-grow-1" style="height:360px;overflow:auto"><div class="text-muted text-center py-5">No conversation selected.</div></div>
          <div class="border-top p-3"><div class="row g-2 align-items-end">
            <div class="col-md-7"><label class="form-label small" for="secureMessageText">Message</label><textarea id="secureMessageText" class="form-control form-control-pawcare" rows="2" maxlength="2000" placeholder="Write a message"></textarea></div>
            <div class="col-md-3"><label class="form-label small" for="secureMessageFile">Attachment</label><input id="secureMessageFile" type="file" accept="application/pdf,image/jpeg,image/png" class="form-control"><small class="text-muted">PDF/JPG/PNG · max 5 MB</small></div>
            <div class="col-md-2"><button id="secureMessageSend" class="btn btn-pawcare w-100" disabled><i class="bi bi-send me-1"></i>Send</button></div>
          </div></div>
        </section>
      </div></div>
    </div></div>
  </div>`;

  if (!document.getElementById('secureMessageModal')) document.body.insertAdjacentHTML('beforeend', modalHtml);

  let context = null;
  let conversationCache = new Map();
  let searchTimer = null;
  let pollTimer = null;
  const modalEl = document.getElementById('secureMessageModal');

  function formatWhen(value) {
    if (!value) return '';
    const d = new Date(value), now = new Date();
    if (d.toDateString() === now.toDateString()) return d.toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'});
    const yesterday = new Date(now); yesterday.setDate(now.getDate()-1);
    if (d.toDateString() === yesterday.toDateString()) return 'Yesterday';
    return d.toLocaleDateString([], {month:'short', day:'numeric'});
  }

  function setHeader(summary) {
    const header = document.getElementById('secureMessageHeader');
    if (!summary && context) {
      header.innerHTML = `<strong>${escapeHtml(context.label || 'Conversation')}</strong><div class="small text-muted">Regarding: ${escapeHtml(context.petName || 'Pet')}${context.appointmentId ? ` · Appointment #${context.appointmentId}` : ''}</div>`;
      return;
    }
    if (!summary) return;
    header.innerHTML = `<div class="d-flex justify-content-between gap-2"><div><strong>${escapeHtml(summary.otherUserName)}</strong><div class="small text-muted">Regarding: <strong>${escapeHtml(summary.petName)}</strong>${summary.appointmentId ? ` · Appointment #${summary.appointmentId}${summary.appointmentDate ? ' · '+escapeHtml(summary.appointmentDate) : ''}` : ''}</div></div><span class="badge text-bg-light align-self-start">${escapeHtml(summary.otherUserRole || '')}</span></div>`;
  }

  async function loadConversations(search = document.getElementById('secureConversationSearch').value || '') {
    const list = document.getElementById('secureConversationList');
    try {
      const items = await api.getConversations(search);
      conversationCache = new Map(items.map(x => [x.id, x]));
      const totalUnread = items.reduce((sum, x) => sum + Number(x.unreadCount || 0), 0);
      const badge = document.getElementById('secureMessageUnread');
      badge.textContent = totalUnread; badge.hidden = totalUnread < 1;
      document.querySelectorAll('[data-message-unread-badge]').forEach(el => { el.textContent=totalUnread; el.hidden=totalUnread<1; });
      list.innerHTML = items.length ? items.map(c => `
        <button type="button" class="btn text-start border rounded-3 p-3 ${context?.conversationId===c.id?'border-primary':''}" onclick="openConversationById(${c.id})">
          <div class="d-flex justify-content-between gap-2"><strong>${escapeHtml(c.petName)} — ${escapeHtml(c.otherUserName)}</strong><small class="text-muted">${formatWhen(c.latestMessageAt)}</small></div>
          <div class="small text-muted text-truncate mt-1">${escapeHtml(c.latestMessage || 'No messages yet')}</div>
          <div class="d-flex justify-content-between mt-2"><small class="text-muted">${c.appointmentId ? 'Appointment #'+c.appointmentId : 'Pet conversation'}</small>${c.unreadCount ? `<span class="badge bg-danger">${c.unreadCount} unread</span>` : ''}</div>
        </button>`).join('') : '<div class="text-muted text-center py-4">No conversations found.</div>';
      return items;
    } catch (e) {
      list.innerHTML = `<div class="text-danger small">${escapeHtml(e.message || 'Unable to load conversations')}</div>`;
      return [];
    }
  }

  async function renderMessages(items) {
    const box = document.getElementById('secureMessageList');
    box.innerHTML = items.length ? items.map(m => {
      const mine = m.senderId === api.user?.id;
      const attach = m.attachmentUrl ? `<div class="mt-2"><button type="button" class="btn btn-sm btn-link p-0" onclick="openSecuredFile('${escapeHtml(m.attachmentUrl)}','${escapeHtml(m.attachmentName||'Attachment')}')"><i class="bi ${m.attachmentType?.startsWith('image/')?'bi-image':'bi-paperclip'} me-1"></i>${escapeHtml(m.attachmentName||'Attachment')}</button></div>` : '';
      return `<div class="d-flex ${mine?'justify-content-end':'justify-content-start'} mb-2"><div class="card-pawcare p-3" style="max-width:82%;${mine?'background:rgba(128,98,170,.08)':''}"><div class="small fw-semibold">${mine?'You':escapeHtml(m.senderName)}</div><div style="white-space:pre-wrap;overflow-wrap:anywhere">${escapeHtml(m.message||'')}</div>${attach}<div class="small text-muted mt-1">${new Date(m.createdAt).toLocaleString()}${mine ? (m.readAt ? ' · Read' : ' · Sent') : ''}</div></div></div>`;
    }).join('') : '<div class="text-muted text-center py-5">No messages yet. Start the conversation below.</div>';
    box.scrollTop = box.scrollHeight;
  }

  async function refreshThread(markRead = true) {
    if (!context) return;
    const box = document.getElementById('secureMessageList');
    box.innerHTML = '<div class="loading-spinner"></div>';
    try {
      const items = context.conversationId
        ? await api.getConversation(context.conversationId)
        : await api.getMessageThread(context.petId, context.otherUserId);
      if (!context.conversationId && items[0]?.conversationId) context.conversationId = items[0].conversationId;
      await renderMessages(items);
      if (markRead && context.conversationId) {
        await api.markConversationRead(context.conversationId);
        await loadConversations();
      }
    } catch (e) {
      box.innerHTML = `<p class="text-danger">${escapeHtml(e.message)}</p>`;
    }
  }

  window.openConversationById = async function(id) {
    const c = conversationCache.get(Number(id));
    if (!c) return;
    context = {conversationId:c.id, petId:c.petId, otherUserId:c.otherUserId, appointmentId:c.appointmentId, label:c.otherUserName, petName:c.petName};
    document.getElementById('secureMessageSend').disabled = false;
    setHeader(c);
    await refreshThread(true);
  };

  window.openMessagesInbox = async function() {
    bootstrap.Modal.getOrCreateInstance(modalEl).show();
    const items = await loadConversations();
    if (!context && items.length) await window.openConversationById(items[0].id);
  };

  window.openMessageThread = async function(petId, otherUserId, appointmentId, label, petName='Pet') {
    context = {conversationId:null, petId:Number(petId), otherUserId:Number(otherUserId), appointmentId:appointmentId?Number(appointmentId):null, label:decodeURIComponent(label||'PawCare user'), petName:decodeURIComponent(petName||'Pet')};
    document.getElementById('secureMessageSend').disabled = false;
    setHeader(null);
    bootstrap.Modal.getOrCreateInstance(modalEl).show();
    await loadConversations();
    const existing = [...conversationCache.values()].find(c => c.petId===context.petId && c.otherUserId===context.otherUserId);
    if (existing) { context.conversationId=existing.id; context.petName=existing.petName; setHeader(existing); }
    await refreshThread(true);
    if (!context.conversationId) {
      const updated = await loadConversations();
      const created = updated.find(c => c.petId===context.petId && c.otherUserId===context.otherUserId);
      if (created) { context.conversationId=created.id; context.petName=created.petName; setHeader(created); }
    }
  };

  document.getElementById('secureMessageSend').addEventListener('click', async () => {
    if (!context) return;
    const btn=document.getElementById('secureMessageSend'), text=document.getElementById('secureMessageText'), file=document.getElementById('secureMessageFile');
    if (!text.value.trim() && !file.files[0]) { showToast('Enter a message or choose a file.','warning'); return; }
    btn.disabled=true;
    try {
      const sent = await api.sendMessage(context.petId, context.otherUserId, context.appointmentId, text.value, file.files[0]);
      if (sent.conversationId) context.conversationId=sent.conversationId;
      text.value=''; file.value='';
      await refreshThread(false); await loadConversations();
      showToast('Message sent.','success');
    } catch(e) { showToast(e.message,'error'); }
    finally { btn.disabled=false; }
  });

  document.getElementById('secureMessageText').addEventListener('keydown', e => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') document.getElementById('secureMessageSend').click();
  });

  document.getElementById('secureConversationSearch').addEventListener('input', () => {
    clearTimeout(searchTimer); searchTimer=setTimeout(()=>loadConversations(),250);
  });

  modalEl.addEventListener('shown.bs.modal', () => {
    clearInterval(pollTimer);
    pollTimer=setInterval(async()=>{ if(context) await refreshThread(false); await loadConversations(); }, 7000);
  });
  modalEl.addEventListener('hidden.bs.modal', () => { clearInterval(pollTimer); pollTimer=null; });

  if (api.isLoggedIn() && ['PET_OWNER','VETERINARIAN'].includes(api.getRole())) {
    loadConversations();
    if (location.hash === '#messages') setTimeout(()=>window.openMessagesInbox(), 200);
  }
})();
