/* PawCare QR encoder. Generates a standards-compliant Version 6-L QR code for short passport URLs. */
(function(root){
  'use strict';
  const VERSION=6, SIZE=41, DATA_CODEWORDS=136, BLOCK_DATA=68, ECC_PER_BLOCK=18;
  const GF_EXP=new Uint8Array(512),GF_LOG=new Uint8Array(256);
  (function(){let x=1;for(let i=0;i<255;i++){GF_EXP[i]=x;GF_LOG[x]=i;x<<=1;if(x&0x100)x^=0x11d;}for(let i=255;i<512;i++)GF_EXP[i]=GF_EXP[i-255];})();
  function mul(a,b){if(!a||!b)return 0;return GF_EXP[GF_LOG[a]+GF_LOG[b]];}
  function generator(degree){let g=[1];for(let i=0;i<degree;i++){const next=new Array(g.length+1).fill(0),a=GF_EXP[i];for(let j=0;j<g.length;j++){next[j]^=g[j];next[j+1]^=mul(g[j],a);}g=next;}return g;}
  const GEN=generator(ECC_PER_BLOCK);
  function ecc(data){const rem=new Array(ECC_PER_BLOCK).fill(0);for(const b of data){const factor=b^rem[0];rem.shift();rem.push(0);for(let j=0;j<ECC_PER_BLOCK;j++)rem[j]^=mul(GEN[j+1],factor);}return rem;}
  function appendBits(out,val,count){for(let i=count-1;i>=0;i--)out.push((val>>>i)&1);}
  function encodeBytes(text){const bytes=Array.from(new TextEncoder().encode(text));if(bytes.length>134)throw new Error('Passport URL is too long for the built-in QR code.');const bits=[];appendBits(bits,0b0100,4);appendBits(bits,bytes.length,8);bytes.forEach(b=>appendBits(bits,b,8));const cap=DATA_CODEWORDS*8;for(let i=0;i<4&&bits.length<cap;i++)bits.push(0);while(bits.length%8)bits.push(0);const data=[];for(let i=0;i<bits.length;i+=8){let b=0;for(let j=0;j<8;j++)b=(b<<1)|bits[i+j];data.push(b);}let pad=0;while(data.length<DATA_CODEWORDS)data.push((pad++%2===0)?0xec:0x11);const blocks=[data.slice(0,BLOCK_DATA),data.slice(BLOCK_DATA)];const parity=blocks.map(ecc),all=[];for(let i=0;i<BLOCK_DATA;i++)for(const b of blocks)all.push(b[i]);for(let i=0;i<ECC_PER_BLOCK;i++)for(const e of parity)all.push(e[i]);return all;}
  function formatBits(mask){const data=(1<<3)|mask;let rem=data<<10;for(let i=14;i>=10;i--)if((rem>>>i)&1)rem^=0x537<<(i-10);return ((data<<10)|rem)^0x5412;}
  function makeMatrix(text){const codewords=encodeBytes(text),m=Array.from({length:SIZE},()=>Array(SIZE).fill(false)),reserved=Array.from({length:SIZE},()=>Array(SIZE).fill(false));
    function fn(r,c,dark){if(r<0||c<0||r>=SIZE||c>=SIZE)return;m[r][c]=!!dark;reserved[r][c]=true;}
    function finder(top,left){for(let dr=-1;dr<=7;dr++)for(let dc=-1;dc<=7;dc++){const r=top+dr,c=left+dc;if(r<0||c<0||r>=SIZE||c>=SIZE)continue;const sep=dr===-1||dr===7||dc===-1||dc===7;const dark=!sep&&(dr===0||dr===6||dc===0||dc===6||(dr>=2&&dr<=4&&dc>=2&&dc<=4));fn(r,c,dark);}}
    finder(0,0);finder(0,SIZE-7);finder(SIZE-7,0);
    for(let i=8;i<=SIZE-9;i++){fn(6,i,i%2===0);fn(i,6,i%2===0);}
    const center=34;for(let dr=-2;dr<=2;dr++)for(let dc=-2;dc<=2;dc++){const d=Math.max(Math.abs(dr),Math.abs(dc));fn(center+dr,center+dc,d!==1);}
    fn(4*VERSION+9,8,true);
    const fmt=formatBits(0);for(let i=0;i<=5;i++)fn(i,8,(fmt>>>i)&1);fn(7,8,(fmt>>>6)&1);fn(8,8,(fmt>>>7)&1);fn(8,7,(fmt>>>8)&1);for(let i=9;i<15;i++)fn(8,14-i,(fmt>>>i)&1);for(let i=0;i<8;i++)fn(8,SIZE-1-i,(fmt>>>i)&1);for(let i=8;i<15;i++)fn(SIZE-15+i,8,(fmt>>>i)&1);
    const stream=[];for(const b of codewords)appendBits(stream,b,8);let k=0,up=true;for(let right=SIZE-1;right>0;right-=2){if(right===6)right--;for(let t=0;t<SIZE;t++){const r=up?SIZE-1-t:t;for(let j=0;j<2;j++){const c=right-j;if(reserved[r][c])continue;let bit=k<stream.length?stream[k++]:0;if((r+c)%2===0)bit^=1;m[r][c]=!!bit;}}up=!up;}return m;}
  function svg(text,options){const opt=options||{},matrix=makeMatrix(text),quiet=4,scale=opt.scale||8,total=SIZE+quiet*2,label=opt.label||'Digital Pet Passport QR code';let path='';for(let r=0;r<SIZE;r++)for(let c=0;c<SIZE;c++)if(matrix[r][c])path+=`M${c+quiet} ${r+quiet}h1v1h-1z`;return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${total} ${total}" width="${total*scale}" height="${total*scale}" role="img" aria-label="${String(label).replace(/&/g,'&amp;').replace(/"/g,'&quot;')}"><rect width="100%" height="100%" fill="white"/><path d="${path}" fill="#241b31" shape-rendering="crispEdges"/></svg>`;}
  function dataUrl(text,options){return 'data:image/svg+xml;charset=utf-8,'+encodeURIComponent(svg(text,options));}
  const api={makeMatrix,svg,dataUrl,version:VERSION,maxUtf8Bytes:134};if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.PawQr=api;
})(typeof window!=='undefined'?window:globalThis);
