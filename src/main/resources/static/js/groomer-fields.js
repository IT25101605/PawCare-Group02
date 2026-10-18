/* Suggestions stay editable: a groomer may always enter their own details. */
window.GroomerFields=(()=>{
    const choices={
        organization:[],
        qualification:['Professional pet grooming certificate','Diploma in pet grooming','Dog grooming qualification','Cat grooming qualification','Advanced pet styling certificate','Salon grooming apprenticeship','Pet first aid training','Animal handling and welfare training','Breed-specific styling training','Hand-stripping training','Low-stress grooming and handling training','On-the-job grooming training'],
        specialty:['All-round pet groomer','Nail trimming specialist','Pet bathing and blow-drying','Pet stylist / haircuts','Cat grooming specialist','De-shedding and coat care','Ear cleaning and hygiene care','Sanitary and paw-pad trims','Breed-specific styling','Hand-stripping specialist','Puppy introductions','Senior pet grooming','Gentle grooming for nervous pets','Long-coat and matting care','Curly-coat grooming','Double-coat grooming','Basic teeth brushing','Flea and tick baths','Mobile pet groomer']
    };
    const hints={organization:'Choose an existing salon or type your own business name.',qualification:'Choose training you have completed, or type your own qualification.',specialty:'Choose a specialty or type your own, such as nail trimming and gentle baths.'};
    let directory;
    function refreshPicker(input,key){
        const picker=document.getElementById(input.id+'-suggestions');if(!picker)return;
        picker.replaceChildren(new Option('Choose from the list…',''),...[...new Set(choices[key].filter(Boolean))].map(value=>new Option(value,value)),new Option('Other — type your own','__custom__'));
        picker.value=choices[key].includes(input.value)?input.value:'';
    }
    function list(key){
        const id='groomer-options-'+key;let element=document.getElementById(id);
        if(!element){element=document.createElement('datalist');element.id=id;document.body.append(element);}
        element.replaceChildren(...[...new Set(choices[key].filter(Boolean))].map(value=>new Option(value,value)));
        document.querySelectorAll('input[data-groomer-choice="'+key+'"]').forEach(input=>refreshPicker(input,key));return id;
    }
    function remember(details){for(const key of Object.keys(choices)){if(details[key]&&!choices[key].includes(details[key]))choices[key].push(details[key]);list(key);}}
    function attach(ids){
        for(const [key,id] of Object.entries(ids)){
            const input=document.getElementById(id);if(!input||!choices[key])continue;
            input.dataset.groomerChoice=key;input.setAttribute('list',list(key));input.autocomplete='off';input.placeholder='Or type your own';
            if(!document.getElementById(id+'-suggestions')){
                const picker=document.createElement('select');picker.id=id+'-suggestions';picker.className='form-select groomer-suggestion-select';picker.dataset.suggestionControl='true';
                picker.setAttribute('aria-label','Choose '+({organization:'a salon / business',qualification:'a qualification',specialty:'a grooming specialty'}[key])+' from suggestions');
                input.before(picker);
                picker.addEventListener('change',()=>{
                    if(picker.value&&picker.value!=='__custom__'){
                        input.value=picker.value;input.dispatchEvent(new Event('input',{bubbles:true}));input.dispatchEvent(new Event('change',{bubbles:true}));
                    }else if(picker.value==='__custom__'){input.focus();input.select();}
                });
                input.addEventListener('input',()=>{picker.value=choices[key].includes(input.value)?input.value:'';});
            }
            refreshPicker(input,key);
            if(!input.parentElement.querySelector('.groomer-field-hint')){
                const hint=document.createElement('small');hint.className='groomer-field-hint';hint.textContent=hints[key];hint.id=id+'-hint';input.after(hint);
                input.setAttribute('aria-describedby',[input.getAttribute('aria-describedby'),hint.id].filter(Boolean).join(' '));
            }
            configureField(input);
        }
        if(!directory)directory=api.get('/groomers').then(rows=>{for(const row of rows)remember({organization:row.organization});}).catch(()=>{directory=null;});
    }
    function fields(prefix){
        return [['Organization','Salon / business','text',150,true],['Qualification','Qualification / grooming certificate','text',200,false],['Specialty','Grooming specialty','text',150,true],['Experience','Years of experience','number',60,true]].map(([key,label,type,max,required])=>`<div class="col-12"><label class="form-label" for="${prefix+key}">${label}${required?'':' (optional)'}</label><input class="form-control" id="${prefix+key}" type="${type}" ${required?'required':''} ${type==='number'?'min="0" max="60" step="1"':`maxlength="${max}"`}></div>`).join('');
    }
    const ids=prefix=>({organization:prefix+'Organization',qualification:prefix+'Qualification',specialty:prefix+'Specialty'});
    const read=prefix=>({organization:value(prefix+'Organization'),qualification:value(prefix+'Qualification')||null,specialty:value(prefix+'Specialty'),experienceYears:Number(value(prefix+'Experience'))});
    function fill(prefix,details){for(const [suffix,key] of [['Organization','organization'],['Qualification','qualification'],['Specialty','specialty'],['Experience','experienceYears']])document.getElementById(prefix+suffix).value=details[key]??'';remember(details);for(const [key,id] of Object.entries(ids(prefix)))refreshPicker(document.getElementById(id),key);}
    return {attach,fields,ids,read,fill,remember};
})();
