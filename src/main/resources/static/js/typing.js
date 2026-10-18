/* Shared heading typing, with full-size measurement and preserved text colours. */
(() => {
    const motion=matchMedia('(prefers-reduced-motion: reduce)');
    const animations=[];
    let ready=!document.querySelector('.paw-page-loader');
    const textOf=node=>node.nodeType===Node.TEXT_NODE?node.textContent:node.nodeName==='BR'?'\n':Array.from(node.childNodes,textOf).join('');
    document.querySelectorAll('[data-typing]').forEach(element=>{
        const original=Array.from(element.childNodes,node=>node.cloneNode(true));
        const originalText=original.map(textOf).join('');
        let phrases=null;try{phrases=JSON.parse(element.dataset.typingPhrases||'null');}catch{}
        if(!Array.isArray(phrases)||!phrases.length)phrases=[originalText];
        const heading=element.closest('h1,h2,h3');
        if(heading&&!heading.hasAttribute('aria-label')&&!heading.querySelector('.visually-hidden'))heading.setAttribute('aria-label',originalText.replace(/\s+/g,' ').trim());
        const measure=document.createElement('span');measure.className='paw-typing-measure';measure.setAttribute('aria-hidden','true');measure.append(...original.map(node=>node.cloneNode(true)));
        const visible=document.createElement('span');visible.className='paw-typing-copy';visible.setAttribute('aria-hidden','true');
        element.classList.add('paw-typing');element.replaceChildren(measure,visible);
        let topic=0,count=0,deleting=false,timer;
        const hold=Number(element.dataset.typingHold)||1700;
        const template=()=>phrases.length===1?original:[document.createTextNode(phrases[topic])];
        let renderedTopic=-1,words=[];
        const caret=document.createElement('span');caret.className='paw-typing-caret';
        function buildWords() {
            let position=0;words=[];
            function copy(node) {
                if(node.nodeType===Node.TEXT_NODE) {
                    const fragment=document.createDocumentFragment();
                    for(const part of node.textContent.split(/(\s+)/).filter(Boolean)) {
                        const chars=Array.from(part);
                        if(/^\s+$/.test(part))fragment.append(document.createTextNode(part));
                        else {
                            const word=document.createElement('span'),letters=document.createElement('span');
                            word.className='paw-typing-word';word.dataset.word=part;
                            letters.className='paw-typing-letters';word.append(letters);fragment.append(word);
                            words.push({letters,chars,start:position});
                        }
                        position+=chars.length;
                    }
                    return fragment;
                }
                if(node.nodeName==='BR'){position++;return node.cloneNode();}
                const clone=node.cloneNode(false);
                for(const child of node.childNodes)clone.append(copy(child));
                return clone;
            }
            visible.replaceChildren(...template().map(copy));renderedTopic=topic;
        }
        function render() {
            if(renderedTopic!==topic)buildWords();
            caret.remove();let current=words[0];
            for(const word of words) {
                const length=Math.max(0,Math.min(word.chars.length,count-word.start));
                word.letters.textContent=word.chars.slice(0,length).join('');
                if(count>=word.start)current=word;
            }
            current?.letters.append(caret);
        }
        function schedule(delay){clearTimeout(timer);if(ready&&!document.hidden&&!motion.matches)timer=setTimeout(tick,delay);}
        function tick() {
            count+=deleting?-1:1;render();
            if(!deleting&&count===Array.from(phrases[topic]).length){deleting=true;schedule(hold);}
            else if(deleting&&count===0){deleting=false;topic=(topic+1)%phrases.length;render();schedule(350);}
            else schedule(deleting?32:55);
        }
        function sync() {
            clearTimeout(timer);element.classList.toggle('is-paused',motion.matches);
            if(motion.matches){count=Array.from(phrases[topic]).length;deleting=true;render();}
            else {render();schedule(60);}
        }
        animations.push({sync,stop:()=>clearTimeout(timer)});sync();
    });
    window.addEventListener('pawcare:page-ready',()=>{if(!ready){ready=true;animations.forEach(animation=>animation.sync());}});
    motion.addEventListener('change',()=>animations.forEach(animation=>animation.sync()));
    document.addEventListener('visibilitychange',()=>animations.forEach(animation=>document.hidden?animation.stop():animation.sync()));
    window.addEventListener('pagehide',()=>animations.forEach(animation=>animation.stop()));
    window.addEventListener('pageshow',event=>{if(event.persisted){ready=true;animations.forEach(animation=>animation.sync());}});
})();
