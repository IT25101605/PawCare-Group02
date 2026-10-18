/* Only decorative motion pauses out of view; content is always visible. */
(()=>{
 const scenes=document.querySelectorAll('[data-home-scene]');
 if(!('IntersectionObserver' in window)){scenes.forEach(scene=>scene.classList.add('home-in-view'));return;}
 const observer=new IntersectionObserver(entries=>entries.forEach(entry=>entry.target.classList.toggle('home-in-view',entry.isIntersecting)),{rootMargin:'120px 0px'});
 scenes.forEach(scene=>observer.observe(scene));
})();
