/* Shared input rules for every PawCare form. Backend validation is authoritative. */
function escapeHtml(v) { return String(v ?? '').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }
function value(id) { return document.getElementById(id).value.trim(); }
function multipart(key,data,fileKey,file) {
    if(!file) return data;
    const body=new FormData();
    body.append(key,new Blob([JSON.stringify(data)],{type:'application/json'}));
    body.append(fileKey,file);
    return body;
}
function setOptions(id,rows,placeholder) {
    const select=document.getElementById(id);
    select.replaceChildren(new Option(placeholder,''));
    for(const row of rows) select.add(new Option(row.name,row.id));
}
function localDate() {
    const now=new Date();
    return new Date(now.getTime()-now.getTimezoneOffset()*60000).toISOString().slice(0,10);
}
const fieldRules={
    firstName:{required:true,maxLength:100},lastName:{required:true,maxLength:100},
    email:{required:true,type:'email',maxLength:150},phone:{required:true,type:'tel',pattern:'[0-9]{10}',minLength:10,maxLength:10,inputMode:'numeric',title:'Enter exactly 10 digits'},
    petName:{required:true,minLength:2,maxLength:50,title:'Use letters and spaces only'},petSpecies:{required:true},petBreed:{},petGender:{required:true},petCustomSpecies:{maxLength:100},petCustomBreed:{maxLength:100},
    petWeight:{min:'0.01',max:'200',step:'0.01'},petColor:{maxLength:50,pattern:'[A-Za-z ]+',title:'Use letters and spaces only'},petDob:{max:localDate()},
    apptPet:{required:true},apptVet:{required:true},apptDate:{required:true,min:localDate()},apptTime:{required:true},
    apptReason:{required:true,minLength:3,maxLength:1000},
    groomPet:{required:true},groomGroomer:{required:true},groomDate:{required:true,min:localDate()},groomTime:{required:true},
    pName:{required:true,minLength:2,maxLength:100},pDesc:{maxLength:2000},pPrice:{required:true,min:'0.01',max:'99999999.99',step:'0.01'},
    pSale:{min:'0.01',max:'99999999.99',step:'0.01'},pStock:{required:true,min:'0',max:'1000000',step:'1'},
    pSku:{required:true,pattern:'[A-Za-z0-9_\\-]{3,50}',maxLength:50},pCatId:{required:true},pBrand:{maxLength:100},pReorder:{min:'0',max:'1000000',step:'1'},pWeight:{maxLength:50},
    recPetId:{required:true,min:'1',step:'1'},recApptId:{min:'1',step:'1'},
    recDiag:{required:true,minLength:3,maxLength:2000},recTreat:{required:true,minLength:3,maxLength:2000},
    recPresc:{maxLength:2000},recWeight:{min:'0.01',max:'999.99',step:'0.01'},recTemp:{min:'1',max:'50',step:'0.01'},
    recDate:{required:true,max:localDate()},recFollowup:{min:localDate()},
    contactName:{required:true,maxLength:100},contactEmail:{required:true,type:'email',maxLength:150},
    contactSubject:{required:true,minLength:3,maxLength:200},contactMessage:{required:true,minLength:10,maxLength:2000},
    searchInput:{maxLength:100},userSearch:{maxLength:100}
};
function configureField(el) {
    if (!el.matches('input,select,textarea')) return;
    if(el.type==='hidden'||el.dataset.suggestionControl) return;
    if(['checkbox','radio'].includes(el.type)&&!el.required)return;
    if(el.dataset.rulesReady) return;
    el.dataset.rulesReady='true';
    Object.assign(el,fieldRules[el.id]||{});
    if(el.required) {
        const wrapper=el.closest('div');
        const label=wrapper?.querySelector('label');
        if(label && !label.querySelector('.required-mark')) label.insertAdjacentHTML('beforeend',' <span class="required-mark text-danger" aria-hidden="true">*</span>');
    }
    const msg=document.createElement('div');msg.className='field-validation-message';msg.id=(el.id||'field-'+(++fieldFeedbackSequence))+'-feedback';
    msg.setAttribute('aria-live','polite');el.dataset.validationFeedback=msg.id;el.insertAdjacentElement('afterend',msg);
    el.setAttribute('aria-describedby',[el.getAttribute('aria-describedby'),msg.id].filter(Boolean).join(' '));
    if(el.maxLength>0 && ['TEXTAREA'].includes(el.tagName)) {
        const counter=document.createElement('small'); counter.className='field-counter text-muted';
        const update=()=>counter.textContent=`${el.value.length}/${el.maxLength}`; el.insertAdjacentElement('afterend',counter); el.addEventListener('input',update); update();
    }
    if(el.id && !el.getAttribute('aria-label')) {
        const label=el.closest('div')?.querySelector('label');
        if(label) {label.htmlFor=el.id;} else {el.setAttribute('aria-label',el.placeholder||el.id.replace(/([A-Z])/g,' $1'));}
    }
    if(el.type==='tel') {
        el.inputMode='numeric';el.maxLength=10;el.pattern='[0-9]{10}';el.title='Enter exactly 10 digits';
        el.addEventListener('input',()=>{el.value=el.value.replace(/[^0-9]/g,'').slice(0,10);});
    }
    const update=()=>{el.setCustomValidity('');if(el.classList.contains('is-invalid')||fieldFeedback(el)?.textContent)checkField(el);else el.classList.remove('is-valid');};
    el.addEventListener('input',update);el.addEventListener('change',update);
    el.addEventListener('blur',()=>{ if(el.value || el.required) checkField(el); });
    if(['pStock','pReorder'].includes(el.id)) el.addEventListener('input',()=>{ if(el.value.includes('.')) el.setCustomValidity('Enter a whole number only.'); });
}
let fieldFeedbackSequence=0;
function fieldFeedback(el) {return document.getElementById(el.dataset.validationFeedback||'');}
function checkField(el) {
    if (!el.matches('input,select,textarea')||el.dataset.suggestionControl) return true;
    if(['checkbox','radio'].includes(el.type)&&!el.required)return true;
    configureField(el);
    if(el.disabled || el.type==='hidden') {el.classList.remove('is-invalid','is-valid');el.removeAttribute('aria-invalid');if(fieldFeedback(el))fieldFeedback(el).textContent='';return true;}
    el.setCustomValidity('');
    if(['text','email','tel'].includes(el.type)||el.tagName==='TEXTAREA') el.value=el.value.trim();
    if(el.required && !el.value.trim()) el.setCustomValidity(({groomPet:'Choose your pet.',groomGroomer:'Choose a groomer.',groomDate:'Choose a date.',groomTime:'Choose an available time.',apptPet:'Choose your pet.',apptVet:'Choose a veterinarian.',apptDate:'Choose a date.',apptTime:'Choose an available time.'})[el.id]||'Please fill in this field.');
    if(el.value && el.minLength>0 && el.value.length<el.minLength) el.setCustomValidity('Enter at least '+el.minLength+' characters.');
    if(el.maxLength>0 && el.value.length>el.maxLength) el.setCustomValidity('Enter no more than '+el.maxLength+' characters.');
    if(el.type==='file' && el.files[0]) {
        const file=el.files[0];
        if(!['image/jpeg','image/png'].includes(file.type)) el.setCustomValidity('Choose a JPEG or PNG image.');
        else if(file.size>5*1024*1024) el.setCustomValidity('Image must be 5 MB or smaller.');
        else if(!file.size) el.setCustomValidity('The image file is empty.');
    }
    const valid=el.checkValidity();
    el.classList.toggle('is-invalid',!valid);
    el.classList.toggle('is-valid',valid && !!el.value && el.type!=='file');
    el.setAttribute('aria-invalid',String(!valid));
    const feedback=fieldFeedback(el);
    if(feedback) feedback.textContent=valid?'':(el.validationMessage||'Check this field.');
    return valid;
}
function validateFields(ids) {
    let first;
    for(const id of ids) {
        const el=document.getElementById(id);
        if(el && !checkField(el) && !first) first=el;
    }
    if(first) {first.focus({preventScroll:true});const box=first.getBoundingClientRect();if(box.top<95||box.bottom>innerHeight-20)first.scrollIntoView({block:'center',behavior:'instant'});return false;}return true;
}
function validateBooking(dateId,timeId) {
    const date=value(dateId), time=value(timeId);
    if(!date || !time) { showToast('Choose an available date and time.','warning'); return false; }
    const when=new Date(date+'T'+time);
    if(Number.isNaN(when.getTime())) { showToast('Choose a valid date and time.','error'); return false; }
    if(when<=new Date()) { showToast('Choose a date and time in the future.','error'); return false; }
    return true;
}
function renderPager(tableId,data,load) {
    const table=document.getElementById(tableId);
    let pager=document.getElementById(tableId+'Pager');
    if(!pager){pager=document.createElement('div');pager.id=tableId+'Pager';pager.className='d-flex gap-2 align-items-center mt-3';table.closest('.card-pawcare').after(pager);}
    pager.replaceChildren();
    if(data.totalPages<2)return;
    for(const [label,page,disabled] of [['Previous',data.number-1,data.first],['Next',data.number+1,data.last]]){
        const button=document.createElement('button');button.className='btn btn-sm btn-pawcare-outline';button.textContent=label;button.disabled=disabled;button.onclick=()=>load(page);pager.append(button);
    }
    const label=document.createElement('span');label.textContent='Page '+(data.number+1)+' of '+data.totalPages;pager.append(label);
}
function previewPhoto(input,id) {
    const preview=document.getElementById(id);
    if(preview.dataset.url) URL.revokeObjectURL(preview.dataset.url);
    preview.hidden=true;
    if(!checkField(input)) { input.reportValidity();return; }
    if(!input.files[0])return;
    const url=URL.createObjectURL(input.files[0]);preview.dataset.url=url;
    preview.onload=()=>{
        if(preview.naturalWidth*preview.naturalHeight>16000000) {
            input.setCustomValidity('Image must be at most 16 megapixels.'); input.value='';preview.hidden=true;input.reportValidity();
        } else preview.hidden=false;
    };
    preview.onerror=()=>{input.value='';preview.hidden=true;showToast('The image could not be read','error');};
    preview.src=url;
}
function configureForm(form) {
        if(form.dataset.validationReady)return;
        form.dataset.validationReady='true';form.noValidate=true;
        form.addEventListener('submit',event=>{
            if(!validateFields([...form.elements].map(e=>e.id).filter(Boolean))) {event.preventDefault();event.stopImmediatePropagation();}
        },true);
}
document.addEventListener('DOMContentLoaded',()=>{
    document.querySelectorAll('input,select,textarea').forEach(configureField);
    document.querySelectorAll('form').forEach(configureForm);
    new MutationObserver(records=>{
        for(const record of records)for(const node of record.addedNodes) {
            if(node.nodeType!==1)continue;
            if(node.matches('input,select,textarea'))configureField(node);
            if(node.matches('form'))configureForm(node);
            node.querySelectorAll('input,select,textarea').forEach(configureField);node.querySelectorAll('form').forEach(configureForm);
        }
    }).observe(document.body,{childList:true,subtree:true});
});
