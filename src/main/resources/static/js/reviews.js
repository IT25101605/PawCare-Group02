(() => {
  const modal = `<div class="modal fade" id="reviewModal" tabindex="-1" aria-hidden="true"><div class="modal-dialog"><div class="modal-content">
    <div class="modal-header"><div><h5 class="modal-title" id="reviewModalTitle">Review</h5><small class="text-muted" id="reviewModalContext"></small></div><button class="btn-close" data-bs-dismiss="modal"></button></div>
    <div class="modal-body">
      <div class="mb-3"><label class="form-label fw-semibold">Rating</label><div id="reviewStars" class="d-flex gap-1" role="radiogroup" aria-label="Rating"></div><input id="reviewRating" type="hidden" value="5"></div>
      <div class="mb-3"><label for="reviewText" class="form-label fw-semibold">Review</label><textarea id="reviewText" class="form-control form-control-pawcare" minlength="2" maxlength="2000" rows="4" placeholder="Share your experience"></textarea><div class="small text-muted mt-1">2–2000 characters</div></div>
      <div id="reviewStatus" class="small text-muted"></div>
    </div>
    <div class="modal-footer"><button id="reviewDelete" class="btn btn-outline-danger me-auto" hidden>Delete</button><button class="btn btn-pawcare-outline" data-bs-dismiss="modal">Close</button><button id="reviewSave" class="btn btn-pawcare">Submit Review</button></div>
  </div></div></div>`;

  const publicModal = `<div class="modal fade" id="productReviewsModal" tabindex="-1" aria-hidden="true"><div class="modal-dialog modal-lg modal-dialog-scrollable"><div class="modal-content">
    <div class="modal-header"><div><h5 class="modal-title" id="productReviewsTitle">Product Reviews</h5><div id="productReviewSummary" class="small text-muted"></div></div><button class="btn-close" data-bs-dismiss="modal"></button></div>
    <div class="modal-body"><div id="productRatingBreakdown" class="mb-4"></div><div id="productReviewList"></div></div>
  </div></div></div>`;

  if (!document.getElementById('reviewModal')) document.body.insertAdjacentHTML('beforeend', modal);
  if (!document.getElementById('productReviewsModal')) document.body.insertAdjacentHTML('beforeend', publicModal);
  if (!document.getElementById('reviewReportModal')) document.body.insertAdjacentHTML('beforeend', `<div class="modal fade" id="reviewReportModal" tabindex="-1"><div class="modal-dialog modal-dialog-centered"><div class="modal-content"><div class="modal-header"><h5 class="modal-title">Report Review</h5><button class="btn-close" data-bs-dismiss="modal"></button></div><div class="modal-body"><label for="reviewReportReason" class="form-label">Why are you reporting this review?</label><textarea id="reviewReportReason" class="form-control form-control-pawcare" minlength="2" maxlength="500" rows="3"></textarea></div><div class="modal-footer"><button class="btn btn-pawcare-outline" data-bs-dismiss="modal">Close</button><button id="reviewReportSubmit" class="btn btn-pawcare">Submit Report</button></div></div></div></div>`);

  let state = null;
  let reportReviewId = null;
  let existing = null;

  function drawStars(value) {
    const root=document.getElementById('reviewStars');
    document.getElementById('reviewRating').value=value;
    root.innerHTML=[1,2,3,4,5].map(n=>`<button type="button" class="btn btn-link p-0 fs-3 text-decoration-none" aria-label="${n} star${n===1?'':'s'}" aria-pressed="${n===value}" onclick="setReviewRating(${n})"><i class="bi ${n<=value?'bi-star-fill text-warning':'bi-star text-muted'}"></i></button>`).join('');
  }
  window.setReviewRating = drawStars;

  async function openEditor(nextState) {
    state=nextState; existing=null;
    document.getElementById('reviewModalTitle').textContent = state.type==='SERVICE' ? 'Review Completed Service' : 'Review Purchased Product';
    document.getElementById('reviewModalContext').textContent = state.context || '';
    document.getElementById('reviewText').value='';
    document.getElementById('reviewStatus').textContent='';
    document.getElementById('reviewDelete').hidden=true;
    document.getElementById('reviewSave').textContent='Submit Review';
    drawStars(5);
    bootstrap.Modal.getOrCreateInstance(document.getElementById('reviewModal')).show();
    try {
      existing = state.type==='SERVICE' ? await api.getMyServiceReview(state.appointmentId) : await api.getMyProductReview(state.orderId,state.productId);
      if(existing){
        drawStars(existing.rating || 5);
        document.getElementById('reviewText').value=existing.review || '';
        document.getElementById('reviewModalTitle').textContent = state.type==='SERVICE' ? 'Your Service Review' : 'Your Product Review';
        document.getElementById('reviewStatus').innerHTML=existing.hidden
          ? '<span class="text-danger"><i class="bi bi-eye-slash me-1"></i>This review is hidden by Admin.</span>'
          : '<span class="text-success"><i class="bi bi-check-circle me-1"></i>Your review is published.</span>';
        document.getElementById('reviewDelete').hidden=false;
        document.getElementById('reviewSave').textContent='Save Changes';
      }
    } catch(e){ showToast(e.message,'error'); }
  }

  window.openServiceReview = (appointmentId,serviceName,groomerName,petName) => openEditor({
    type:'SERVICE', appointmentId:Number(appointmentId), context:`${decodeURIComponent(serviceName||'Service')} · ${decodeURIComponent(groomerName||'Groomer')} · ${decodeURIComponent(petName||'Pet')}`
  });

  window.openProductReview = (orderId,productId,productName) => openEditor({
    type:'PRODUCT', orderId:Number(orderId), productId:Number(productId), context:`${decodeURIComponent(productName||'Product')} · Order #${orderId}`
  });

  document.getElementById('reviewSave').addEventListener('click', async()=>{
    if(!state)return;
    const btn=document.getElementById('reviewSave'), review=document.getElementById('reviewText').value.trim(), rating=Number(document.getElementById('reviewRating').value);
    if(review.length<2){showToast('Please enter at least 2 characters for your review.','warning');return;}
    btn.disabled=true;
    try{
      const data={rating,review};
      if(existing) existing=await api.updateReview(existing.id,data);
      else existing=state.type==='SERVICE' ? await api.createServiceReview(state.appointmentId,data) : await api.createProductReview(state.orderId,state.productId,data);
      document.getElementById('reviewModalTitle').textContent = state.type==='SERVICE' ? 'Your Service Review' : 'Your Product Review';
      document.getElementById('reviewStatus').innerHTML=existing.hidden
        ? '<span class="text-danger"><i class="bi bi-eye-slash me-1"></i>Your changes were saved, but this review remains hidden by Admin.</span>'
        : '<span class="text-success"><i class="bi bi-check-circle me-1"></i>Your review is published.</span>';
      document.getElementById('reviewDelete').hidden=false;
      document.getElementById('reviewSave').textContent='Save Changes';
      showToast(existing.hidden?'Review changes saved. The review remains hidden by Admin.':'Review saved successfully.',existing.hidden?'warning':'success');
      if(typeof loadGroom==='function' && state.type==='SERVICE') loadGroom();
      if(typeof loadOrders==='function' && state.type==='PRODUCT') loadOrders();
    }catch(e){showToast(e.message,'error');}
    finally{btn.disabled=false;}
  });

  document.getElementById('reviewDelete').addEventListener('click', async()=>{
    if(!existing || !await pawConfirm('Delete this review? You can add a new review again while the order/service remains eligible.',{title:'Delete review?',type:'danger',confirmText:'Delete Review'}))return;
    const btn=document.getElementById('reviewDelete');btn.disabled=true;
    try{await api.deleteReview(existing.id);existing=null;bootstrap.Modal.getOrCreateInstance(document.getElementById('reviewModal')).hide();showToast('Review deleted.','success');if(typeof loadGroom==='function')loadGroom();if(typeof loadOrders==='function')loadOrders();}
    catch(e){showToast(e.message,'error');}finally{btn.disabled=false;}
  });

  window.openProductReviews = async function(productId, productName) {
    document.getElementById('productReviewsTitle').textContent=decodeURIComponent(productName||'Product')+' Reviews';
    document.getElementById('productReviewSummary').textContent='Loading…';
    document.getElementById('productRatingBreakdown').innerHTML='';
    document.getElementById('productReviewList').innerHTML='<div class="loading-spinner"></div>';
    bootstrap.Modal.getOrCreateInstance(document.getElementById('productReviewsModal')).show();
    try{
      const data=await api.getProductReviews(productId);
      const avg=Number(data.averageRating||0).toFixed(1), total=Number(data.totalReviews||0);
      document.getElementById('productReviewSummary').textContent=`${avg} / 5 · ${total} review${total===1?'':'s'}`;
      const rows=[5,4,3,2,1].map(n=>{const c=Number(data.ratingBreakdown?.[n]||0),pct=total?Math.round(c*100/total):0;return `<div class="d-flex align-items-center gap-2 small mb-1"><span style="width:36px">${n}★</span><div class="progress flex-grow-1" style="height:8px"><div class="progress-bar" style="width:${pct}%"></div></div><span style="width:32px" class="text-end">${c}</span></div>`}).join('');
      document.getElementById('productRatingBreakdown').innerHTML=rows;
      const list=data.recentReviews||[];
      document.getElementById('productReviewList').innerHTML=list.length?list.map(r=>`<article class="border-bottom py-3"><div class="d-flex justify-content-between"><strong>${escapeHtml(r.userName||'Customer')}</strong><span class="text-warning">${'★'.repeat(r.rating)}${'☆'.repeat(5-r.rating)}</span></div><p class="mb-1 mt-2" style="white-space:pre-wrap">${escapeHtml(r.review||'')}</p><div class="d-flex justify-content-between align-items-center"><small class="text-muted">${r.verifiedPurchase?'Verified purchase · ':''}${formatDate(r.createdAt)}</small>${api.isLoggedIn() && r.userId!==api.user?.id?`<button class="btn btn-sm btn-link text-muted" onclick="openReviewReport(${r.id})">Report</button>`:''}</div></article>`).join(''):'<div class="text-muted text-center py-4">No reviews yet.</div>';
    }catch(e){document.getElementById('productReviewList').innerHTML=`<div class="text-danger">${escapeHtml(e.message)}</div>`;}
  };

  window.openReviewReport = function(id) {
    reportReviewId=Number(id);
    document.getElementById('reviewReportReason').value='';
    bootstrap.Modal.getOrCreateInstance(document.getElementById('reviewReportModal')).show();
  };
  document.getElementById('reviewReportSubmit').addEventListener('click', async()=>{
    const reason=document.getElementById('reviewReportReason').value.trim(), btn=document.getElementById('reviewReportSubmit');
    if(!reportReviewId || reason.length<2){showToast('Please provide a short reason.','warning');return;}
    btn.disabled=true;
    try{await api.reportReview(reportReviewId,reason);bootstrap.Modal.getOrCreateInstance(document.getElementById('reviewReportModal')).hide();showToast('Review reported to Admin.','success');}
    catch(e){showToast(e.message,'error');}
    finally{btn.disabled=false;}
  });
})();
