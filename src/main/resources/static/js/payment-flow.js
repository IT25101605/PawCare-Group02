document.body.insertAdjacentHTML('beforeend', `<div class="modal fade" id="paymentModal" tabindex="-1" aria-labelledby="paymentTitle"><div class="modal-dialog"><div class="modal-content"><div class="modal-header"><h5 id="paymentTitle" class="modal-title">Card Details</h5><button class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button></div><div class="modal-body"><div id="paymentSummary"></div><form id="cardForm" class="mt-3"><div class="mb-3"><label class="form-label">Card Number</label><input id="cardNumber" class="form-control form-control-pawcare" inputmode="numeric" autocomplete="cc-number" maxlength="19" placeholder="1234 5678 9012 3456"><div class="invalid-feedback" id="cardNumberError"></div></div><div class="row g-2"><div class="col-4"><label class="form-label">Expiry Month</label><input id="expiryMonth" class="form-control form-control-pawcare" inputmode="numeric" autocomplete="cc-exp-month" maxlength="2" placeholder="MM"><div class="invalid-feedback" id="expiryMonthError"></div></div><div class="col-4"><label class="form-label">Expiry Year</label><input id="expiryYear" class="form-control form-control-pawcare" inputmode="numeric" autocomplete="cc-exp-year" maxlength="4" placeholder="YYYY"><div class="invalid-feedback" id="expiryYearError"></div></div><div class="col-4"><label class="form-label">CVC</label><input id="cardCvc" type="password" class="form-control form-control-pawcare" inputmode="numeric" autocomplete="cc-csc" maxlength="3" placeholder="CVC"><div class="invalid-feedback" id="cardCvcError"></div></div></div><p class="small text-muted mt-2 mb-0">Card details are validated for this project demonstration and are not stored.</p></form><div id="cashMessage" class="alert alert-info mt-3" hidden><strong>Pay at Shop</strong><br>Your order is confirmed for in-store payment. No online-delivery shipping charge is added.</div><p id="paymentError" class="text-danger mt-3" role="alert"></p><button id="completePaymentButton" class="btn btn-pawcare mt-2">Pay Now</button><div id="billResult" class="mt-3"></div></div></div></div></div>`);
let paymentOrder=null,paymentDoneCallback=null,paymentSelectedMethod='CREDIT_CARD',paymentProcessing=false;
function fieldError(id,message){const el=document.getElementById(id);el.classList.toggle('is-invalid',!!message);document.getElementById(id+'Error').textContent=message||'';return !message;}
function validateCardForm(){const number=value('cardNumber').replace(/[\s-]/g,''),month=Number(value('expiryMonth')),year=Number(value('expiryYear')),cvc=value('cardCvc');let ok=true;ok=fieldError('cardNumber',!/^[0-9]{16}$/.test(number)?'Card number must contain exactly 16 digits.':'')&&ok;ok=fieldError('expiryMonth',!Number.isInteger(month)||month<1||month>12?'Expiry month must be between 01 and 12.':'')&&ok;const now=new Date(),expired=year<now.getFullYear()||(year===now.getFullYear()&&month<now.getMonth()+1);ok=fieldError('expiryYear',!/^\d{4}$/.test(value('expiryYear'))?'Enter a 4-digit expiry year.':expired?'This card has expired.':'')&&ok;ok=fieldError('cardCvc',!/^[0-9]{3}$/.test(cvc)?'Invalid CVC. CVC must contain exactly 3 digits.':'')&&ok;return ok;}
function orderLines(o){return (o.items||[]).map(i=>`<tr><td>${escapeHtml(i.productName)}</td><td>${i.quantity}</td><td>${formatCurrency(i.totalPrice??(Number(i.unitPrice||0)*Number(i.quantity||0)))}</td></tr>`).join('');}
function billHtml(o,label,cash=false){return `<div id="printableBill"><div class="text-center mb-3"><h4 class="mb-1">PawCare</h4><div class="text-muted">${escapeHtml(label)}</div></div><p><strong>Order:</strong> ${escapeHtml(o.orderNumber)}<br><strong>Subtotal:</strong> ${formatCurrency(o.subtotal)}<br><strong>Tax:</strong> ${formatCurrency(o.tax)}<br><strong>Shipping:</strong> ${formatCurrency(o.shippingCost)}<br><strong>Total:</strong> ${formatCurrency(o.totalAmount)}</p><table class="table table-sm"><thead><tr><th>Product</th><th>Qty</th><th>Amount</th></tr></thead><tbody>${orderLines(o)}</tbody></table>${cash?'<p class="mb-0"><strong>Payment:</strong> Pay at shop</p>':''}</div><button class="btn btn-pawcare-outline" onclick="printPaymentBill()"><i class="bi bi-printer me-1"></i>Print Invoice</button> <button type="button" class="btn btn-pawcare ms-2 pc-invoice-modal-button" onclick="downloadPaymentSlip(null,null,this)"><i class="bi bi-download me-1" aria-hidden="true"></i><span>Download Invoice</span></button>`;}
const activeInvoiceDownloads = new Set();
/** Download the existing authenticated PDF receipt without changing server behavior. */
async function downloadPaymentSlip(orderId=null,orderNumber=null,button=null){
    const id=Number(orderId || paymentOrder?.id);
    if(!Number.isSafeInteger(id) || id<=0){showToast('No invoice is available for this order.','error');return false;}
    if(activeInvoiceDownloads.has(id))return false;
    const actionButton=button || (typeof event!=='undefined' ? event?.target?.closest?.('button') : null);
    const original=actionButton?.innerHTML;
    activeInvoiceDownloads.add(id);
    if(actionButton){actionButton.disabled=true;actionButton.setAttribute('aria-busy','true');actionButton.innerHTML='<span class="spinner-border spinner-border-sm" aria-hidden="true"></span><span>Preparing PDF…</span>';}
    try{
        const headers=api.headers();
        delete headers['Content-Type'];
        headers.Accept='application/pdf';
        const response=await fetch('/api/orders/'+id+'/receipt.pdf',{method:'GET',headers,credentials:'same-origin',cache:'no-store'});
        if(!response.ok){
            let message='Could not download this invoice.';
            try{const error=await response.json();message=error.message || error.error || message;}catch(_){ }
            if(response.status===401)message='Your session has expired. Sign in again to download the invoice.';
            if(response.status===403)message='You do not have permission to download this invoice.';
            if(response.status===404)message='No invoice was found for this order.';
            throw new Error(message);
        }
        const contentType=(response.headers.get('content-type')||'').toLowerCase();
        if(!contentType.includes('application/pdf'))throw new Error('The server did not return a PDF invoice.');
        const blob=await response.blob();
        if(blob.size<8)throw new Error('The invoice PDF is empty.');
        const signature=await blob.slice(0,5).text();
        if(signature!=='%PDF-')throw new Error('The server returned an invalid PDF file.');
        const rawOrder=orderNumber ? (()=>{try{return decodeURIComponent(orderNumber);}catch(_){return String(orderNumber);}})() : paymentOrder?.orderNumber || String(id);
        const safeOrder=String(rawOrder).replace(/[^a-zA-Z0-9_-]/g,'').slice(0,70)||String(id);
        const objectUrl=URL.createObjectURL(blob);
        const link=document.createElement('a');
        link.href=objectUrl;link.download='PawCare-Invoice-'+safeOrder+'.pdf';link.style.display='none';
        document.body.appendChild(link);link.click();link.remove();
        setTimeout(()=>URL.revokeObjectURL(objectUrl),60000);
        showToast('Invoice download started.','success');
        return true;
    }catch(error){showToast(error.message || 'Invoice download failed. Please try again.','error');return false;}
    finally{
        activeInvoiceDownloads.delete(id);
        if(actionButton){actionButton.disabled=false;actionButton.removeAttribute('aria-busy');actionButton.innerHTML=original;}
    }
}
function printPaymentBill(){
    const html=document.getElementById('printableBill')?.outerHTML;if(!html)return;
    const preview=window.open('','_blank','width=800,height=650');
    if(!preview){showToast('Allow the invoice preview to open, then try Print Invoice again.','info');return;}
    const stylesheet=new URL('/vendor/bootstrap/bootstrap.min.css',location.origin).href;
    preview.onload=()=>{preview.focus();preview.print();};
    preview.document.write(`<html><head><title>PawCare Invoice</title><link href="${stylesheet}" rel="stylesheet"><style>body{font-family:Arial,sans-serif}.table{margin-top:20px}</style></head><body class="p-4">${html}</body></html>`);
    preview.document.close();
}
function renderPaymentSummary(){document.getElementById('paymentSummary').innerHTML=`<div class="rounded-4 p-3" style="background:#f8f5fb"><p class="mb-1"><strong>Order:</strong> ${escapeHtml(paymentOrder.orderNumber)}</p><p class="mb-1"><strong>Subtotal:</strong> ${formatCurrency(paymentOrder.subtotal)}</p><p class="mb-1"><strong>Shipping:</strong> ${formatCurrency(paymentOrder.shippingCost)}</p><p class="mb-0 fs-5"><strong>Total:</strong> ${formatCurrency(paymentOrder.totalAmount)}</p></div>`;}
async function completeSelectedPayment(){if(paymentProcessing)return;const btn=document.getElementById('completePaymentButton');if(paymentSelectedMethod==='CREDIT_CARD'&&!validateCardForm())return;paymentProcessing=true;btn.disabled=true;document.getElementById('paymentError').textContent='';try{const card=paymentSelectedMethod==='CREDIT_CARD'?{cardNumber:value('cardNumber'),expiryMonth:Number(value('expiryMonth')),expiryYear:Number(value('expiryYear')),cvc:value('cardCvc')}:{};await api.processPayment(paymentOrder.id,paymentSelectedMethod,card);paymentOrder=await api.getOrder(paymentOrder.id);renderPaymentSummary();document.getElementById('cardForm').hidden=true;document.getElementById('cashMessage').hidden=true;btn.hidden=true;document.getElementById('paymentTitle').textContent=paymentSelectedMethod==='CASH'?'In-Store Invoice':'Payment Complete';document.getElementById('billResult').innerHTML=billHtml(paymentOrder,paymentSelectedMethod==='CASH'?'In-Store Payment Invoice':'PawCare Payment Invoice',paymentSelectedMethod==='CASH');if(paymentDoneCallback)await paymentDoneCallback();showToast(paymentSelectedMethod==='CASH'?'In-store invoice created.':'Payment completed.','success');}catch(e){document.getElementById('paymentError').textContent=e.message;}finally{paymentProcessing=false;btn.disabled=false;}}
async function openPaymentFlow(orderOrId,callback){paymentDoneCallback=callback||null;paymentOrder=typeof orderOrId==='object'?orderOrId:await api.getOrder(orderOrId);paymentSelectedMethod=(paymentOrder.checkoutPaymentMethod||'CREDIT_CARD').toUpperCase()==='CASH'?'CASH':'CREDIT_CARD';renderPaymentSummary();document.getElementById('cardForm').reset();document.querySelectorAll('#cardForm .is-invalid').forEach(e=>e.classList.remove('is-invalid'));document.getElementById('billResult').replaceChildren();document.getElementById('paymentError').textContent='';document.getElementById('completePaymentButton').hidden=false;document.getElementById('cashMessage').hidden=paymentSelectedMethod!=='CASH';document.getElementById('cardForm').hidden=paymentSelectedMethod==='CASH';document.getElementById('paymentTitle').textContent=paymentSelectedMethod==='CASH'?'Preparing In-Store Invoice':'Card Details';document.getElementById('completePaymentButton').textContent=paymentSelectedMethod==='CASH'?'Create Invoice':'Pay '+formatCurrency(paymentOrder.totalAmount);bootstrap.Modal.getOrCreateInstance(document.getElementById('paymentModal')).show();if(paymentSelectedMethod==='CASH')await completeSelectedPayment();}
document.getElementById('completePaymentButton').addEventListener('click',completeSelectedPayment);
document.getElementById('paymentModal').addEventListener('hidden.bs.modal',()=>{document.getElementById('completePaymentButton').hidden=false;document.getElementById('cardForm').hidden=false;document.getElementById('cashMessage').hidden=true;});
const cardNumberInput=document.getElementById('cardNumber');cardNumberInput.addEventListener('input',()=>{const digits=cardNumberInput.value.replace(/\D/g,'').slice(0,16);cardNumberInput.value=(digits.match(/.{1,4}/g)||[]).join(' ');fieldError('cardNumber','');});
for(const id of ['expiryMonth','expiryYear','cardCvc'])document.getElementById(id).addEventListener('input',event=>{const max=id==='cardCvc'?3:id==='expiryMonth'?2:4;event.target.value=event.target.value.replace(/\D/g,'').slice(0,max);fieldError(id,'');});
