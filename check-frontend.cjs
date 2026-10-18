const fs=require('fs'),vm=require('vm');let count=0;
function walk(dir){for(const e of fs.readdirSync(dir,{withFileTypes:true})){
const p=dir+'/'+e.name;if(e.isDirectory())walk(p);
else if(p.endsWith('.js')){new vm.Script(fs.readFileSync(p,'utf8'),{filename:p});count++;}
else if(p.endsWith('.html')){const s=fs.readFileSync(p,'utf8');for(const m of s.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/gi)){
if(m[1].trim()){new vm.Script(m[1],{filename:p});count++;}}}
}}
walk('src/main/resources/static');console.log(count+' JavaScript blocks parsed');

