(()=>{var ln="0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ",Vr=(e=8)=>{let t="";for(let n=0;n<e;n+=1)t+=ln[Math.floor(Math.random()*ln.length)];return t},oe=()=>({schemaVersion:1,channels:[],keywords:[]}),st=e=>e.trim().replace(/^https?:\/\/(www\.|m\.)?youtube\.com\//i,"").replace(/^\/+/,"").replace(/^(c|channel|user)\//i,"").replace(/^\/+/,"").replace(/^@+/,"").replace(/\s+/g," ").toLocaleLowerCase(),zr=(e,t)=>{if(!e)return!1;let n=st(e);return n?mn(t).some(o=>n===o):!1},Se=(e,t)=>e.some(n=>zr(n,t)),cn=(e,t)=>{let n=st(e);return n?{id:typeof t?.id=="string"&&t.id?t.id:Vr(),value:n,enabled:typeof t?.enabled=="boolean"?t.enabled:!0,createdAt:typeof t?.createdAt=="number"?t.createdAt:Date.now()}:null},un=e=>{let t=new Set,n=[];for(let o of e||[]){let r=typeof o=="string"?cn(o):cn(o.value||"",o);!r||t.has(r.value)||(t.add(r.value),n.push(r))}return n},dn=e=>({schemaVersion:1,channels:un(e?.channels),keywords:un(e?.keywords)});var mn=e=>e.filter(t=>t.enabled).map(t=>t.value),at=(e,t)=>{if(!e)return!1;let n=st(e);return n?mn(t).some(o=>n.includes(o)):!1},ke=(e,t)=>e.some(n=>at(n,t));var jr=["adBreakHeartbeatParams","adPlacements","adSlots","playerAds"],ct=new RegExp("^/youtubei/v1/(browse|get_watch|next|player|reel|search)"),Ee=e=>e.blockAds!==!1;function ut(e,t={}){let n=JSON.parse(e);return Ee(t)&&dt(n[0].playerResponse),gn(n[0].playerResponse,t),se(n,t),JSON.stringify(n)}function dt(e){return jr.forEach(t=>delete e[t]),e}function mt(e,t={}){return Ee(t)&&dt(e),gn(e,t),se(e,t),e}function pt(e,t,n={}){return JSON.stringify(mt(JSON.parse(e),n))}function ft(e,t,n={}){let o=JSON.parse(e),r=n.hideShorts!==!1;se(o,n);let s=(o.contents?.sectionListRenderer||o.contents?.twoColumnSearchResultsRenderer?.primaryContents.sectionListRenderer)?.contents[0].itemSectionRenderer||o.onResponseReceivedCommands?.[0].appendContinuationItemsAction?.continuationItems?.[0].itemSectionRenderer;return s&&(s.contents=s.contents.map(a=>Wr(a,t,r)).filter(Boolean)),$(o,t,n),JSON.stringify(o)}function ie(e,t,n={}){let o=JSON.parse(e);return se(o,n),$(o,t,n),JSON.stringify(o)}function Te(e,t,n={}){let o=JSON.parse(e);return Ee(n)&&o?.playerResponse&&typeof o.playerResponse=="object"&&dt(o.playerResponse),se(o,n),$(o,t,n),JSON.stringify(o)}function Wr(e,t,n){let{videoWithContextRenderer:o,gridShelfViewModel:r}=e;if(!(n&&(r||o?.navigationEndpoint.commandMetadata.webCommandMetadata.url.startsWith("/shorts")))&&!fn(e,t))return e}function $(e,t,n={}){if(!e||typeof e!="object")return;let o=Ee(n);for(let r of Object.keys(e)){let i=e[r];Array.isArray(i)?(e[r]=i.filter(s=>!(o&&Qr(s))&&!fn(s,t)),e[r].forEach(s=>$(s,t,n))):i&&typeof i=="object"&&$(i,t,n)}}var Fr=["adSlotRenderer","adsEngagementPanelRenderer","bannerPromoRenderer","brandVideoShelfRenderer","brandVideoSingletonRenderer","carouselAdRenderer","compactPromotedItemRenderer","compactPromotedVideoRenderer","displayAdRenderer","inFeedAdLayoutRenderer","mealbarPromoRenderer","primetimePromoRenderer","promotedSparklesTextSearchRenderer","promotedSparklesWebRenderer","promotedVideoRenderer","searchPyvRenderer","statementBannerRenderer"];function Qr(e){return!e||typeof e!="object"?!1:e.command?.reelWatchEndpoint?.adClientParams?.isAd?!0:[e,e.richItemRenderer?.content,e.richSectionRenderer?.content,e.itemSectionRenderer?.contents?.length===1?e.itemSectionRenderer.contents[0]:void 0].filter(Boolean).some(n=>Fr.some(o=>n[o]))}function fn(e,t){let n=dn(t);return!n.channels.length&&!n.keywords.length?!1:yn(e).some(o=>Yr(o,n))}function yn(e){if(!e||typeof e!="object")return[];let t=[e.videoRenderer,e.compactVideoRenderer,e.gridVideoRenderer,e.videoWithContextRenderer,e.reelItemRenderer,e.lockupViewModel,e.richItemRenderer?.content?.videoRenderer,e.richItemRenderer?.content?.lockupViewModel].filter(Boolean);return t.length?t:[]}function Yr(e,t){let n=Kr(e),o=Gr(e);return ke(n,t.keywords)||Se(o,t.channels)}function Kr(e){return[H(e?.title),H(e?.headline),e?.metadata?.lockupMetadataViewModel?.title?.content].filter(Boolean)}function hn(e,t={ids:new Set,urls:new Set,names:new Set}){if(!e||typeof e!="object")return t;if(e.browseEndpoint){let{browseId:n,canonicalBaseUrl:o}=e.browseEndpoint;typeof n=="string"&&n&&t.ids.add(n),typeof o=="string"&&o&&t.urls.add(o)}typeof e.text=="string"&&e.text&&e.navigationEndpoint?.browseEndpoint&&t.names.add(e.text);for(let n of Object.keys(e))hn(e[n],t);return t}function Gr(e){let t=hn(e),n=[...t.names,...t.urls,...t.ids];return n.length===0?[H(e?.ownerText),H(e?.shortBylineText),H(e?.longBylineText),H(e?.owner?.videoOwnerRenderer?.title),e?.shortBylineText?.runs?.[0]?.navigationEndpoint?.browseEndpoint?.canonicalBaseUrl,e?.shortBylineText?.runs?.[0]?.navigationEndpoint?.browseEndpoint?.browseId,e?.ownerText?.runs?.[0]?.navigationEndpoint?.browseEndpoint?.canonicalBaseUrl,e?.ownerText?.runs?.[0]?.navigationEndpoint?.browseEndpoint?.browseId,e?.metadata?.lockupMetadataViewModel?.metadata?.content,e?.contentMetadata?.runs?.map(o=>o?.text).join(" ")].filter(Boolean):n}function H(e){return e?typeof e=="string"?e:typeof e.simpleText=="string"?e.simpleText:typeof e.content=="string"?e.content:Array.isArray(e.runs)?e.runs.map(t=>t?.text||"").join(""):"":""}function gn(e,t){if(!t.showOriginalVideoTitle)return;let n=H(e?.videoDetails?.title);n&&(re(e?.microformat?.playerMicroformatRenderer?.title,n),re(e?.videoDetails?.title,n))}function se(e,t){!t.showOriginalVideoTitle||!e||typeof e!="object"||lt(e,n=>{for(let o of yn(n))pn(o);pn(n)})}function lt(e,t){if(!(!e||typeof e!="object")){if(t(e),Array.isArray(e)){e.forEach(n=>lt(n,t));return}for(let n of Object.keys(e))lt(e[n],t)}}function pn(e){if(!e||typeof e!="object")return;let t=Jr(e);t&&(re(e.title,t),re(e.headline,t),re(e.metadata?.lockupMetadataViewModel?.title,t))}function Jr(e){return[e.originalTitle,e.untranslatedTitle,e.untranslatedVideoTitle,e.title?.originalTitle,e.title?.untranslatedTitle,e.headline?.originalTitle,e.metadata?.lockupMetadataViewModel?.originalTitle,e.metadata?.lockupMetadataViewModel?.title?.originalTitle].map(H).find(Boolean)||""}function re(e,t){!e||typeof e!="object"||(typeof e.simpleText=="string"&&(e.simpleText=t),typeof e.content=="string"&&(e.content=t),Array.isArray(e.runs)&&e.runs.length&&e.runs.forEach((n,o)=>{n&&typeof n=="object"&&typeof n.text=="string"&&(n.text=o===0?t:"")}))}var yt=()=>window.NouTube?.getSettings?.()?.blockAds!==!1,Xr=()=>({showOriginalVideoTitle:!!window.NouTube?.getSettings?.()?.showOriginalVideoTitle,blockAds:yt()});function bn(){let e=window.ytInitialData;Object.defineProperty(window,"ytInitialData",{get(){return e},set(s){try{let a=window.NouTube?.getBlocklist?.();$(s,a,{blockAds:yt()})}catch(a){console.error("NouScript initialData:",a)}e=s},configurable:!0});let t=s=>{try{s&&typeof s=="object"&&mt(s,Xr())}catch(a){console.error("NouScript initialPlayerResponse:",a)}return s},n=t(window.ytInitialPlayerResponse);Object.defineProperty(window,"ytInitialPlayerResponse",{get(){return n},set(s){n=t(s)},configurable:!0});let o=window.ytInitialReelWatchSequenceResponse;Object.defineProperty(window,"ytInitialReelWatchSequenceResponse",{get(){return o},set(s){try{let a=window.NouTube?.getBlocklist?.(),l={blockAds:yt()};typeof s=="string"?s=Te(s,a,l):$(s,a,l)}catch(a){console.error("NouScript initialReelSequence:",a)}o=s},configurable:!0});let r=fetch;window.fetch=async(...s)=>{let a=s[0],l=a instanceof Request?a.url:a.toString(),c=await r(...s),m=new URL(l,location.origin).pathname.match(ct),f=window.NouTube?.getBlocklist?.(),d=window.NouTube?.getSettings?.(),u={showOriginalVideoTitle:!!d?.showOriginalVideoTitle,blockAds:d?.blockAds!==!1};if(c.status>200||!m)return c;let p=await c.text(),y={status:c.status,headers:c.headers};try{let g={browse:(h,x)=>ie(h,x,u),get_watch:h=>ut(h,u),next:(h,x)=>ie(h,x,u),reel:(h,x)=>Te(h,x,u),search:(h,x)=>ft(h,f,{hideShorts:window.NouTube.shortsHidden,...u})}[m[1]]||((h,x)=>pt(h,x,u));return new Response(g(p,f),y)}catch(g){console.error("NouScript:",g)}return new Response(p,y)};let i=XMLHttpRequest.prototype.open;XMLHttpRequest.prototype.open=function(s,a){return a=a.toString(),this.addEventListener("readystatechange",function(){if(this.readyState!==4)return;let l=new URL(a,location.origin).pathname.match(ct);if(!l)return;let c=window.NouTube?.getBlocklist?.(),m=window.NouTube?.getSettings?.(),f={showOriginalVideoTitle:!!m?.showOriginalVideoTitle,blockAds:m?.blockAds!==!1};try{let u=({browse:(p,y)=>ie(p,y,f),get_watch:p=>ut(p,f),next:(p,y)=>ie(p,y,f),reel:(p,y)=>Te(p,y,f),search:(p,y)=>ft(p,c,{hideShorts:window.NouTube.shortsHidden,...f})}[l[1]]||((p,y)=>pt(p,y,f)))(this.responseText,c);Object.defineProperty(this,"response",{writable:!0}),Object.defineProperty(this,"responseText",{writable:!0}),this.response=this.responseText=u}catch(d){console.error("NouScript:",d)}}),i.apply(this,[s,a])}}var Zr=["webm","vp8","vp9","av01"];function vn(e){return function(t){if(t===void 0)return"";for(let n of Zr)if(t.indexOf(n)!==-1)return"";return e(t)}}function wn(){try{let e=document.createElement("video"),t=e.canPlayType.bind(e);HTMLVideoElement.prototype.canPlayType=vn(t)}catch{}try{let e=window.MediaSource;if(e&&typeof e.isTypeSupported=="function"){let t=e.isTypeSupported.bind(e);e.isTypeSupported=vn(t)}}catch{}}var ei=/^(https?:\/\/i\d?\.ytimg\.com\/(?:vi|vi_webp)\/[^/]+\/)(default|mqdefault|hqdefault|sddefault|hq720|hq1|hq2|hq3|maxresdefault)(_custom_\d+)?(\.[a-zA-Z0-9]+)(.*)$/;function ht(e,t){let n=ei.exec(e);return!n||n[2]===t?e:`${n[1]}${t}${n[4]}${n[5]}`}function ti(e,t){return e.replace(/(https?:\/\/i\d?\.ytimg\.com\/(?:vi|vi_webp)\/[^/]+\/)(default|mqdefault|hqdefault|sddefault|hq720|hq1|hq2|hq3|maxresdefault)(_custom_\d+)?(\.[a-zA-Z0-9]+)/g,`$1${t}$4`)}function _n(e){if(!e||e==="default")return;try{let n=Object.getOwnPropertyDescriptor(HTMLImageElement.prototype,"src");if(n?.set&&n.get){let o=n.set,r=n.get;Object.defineProperty(HTMLImageElement.prototype,"src",{configurable:!0,enumerable:!0,get(){return r.call(this)},set(i){o.call(this,typeof i=="string"?ht(i,e):i)}})}}catch{}try{let n=Element.prototype.setAttribute;Element.prototype.setAttribute=function(o,r){return this instanceof HTMLImageElement&&o==="src"&&typeof r=="string"?n.call(this,o,ht(r,e)):n.call(this,o,r)}}catch{}let t=()=>{document.querySelectorAll("img").forEach(n=>{let o=n.getAttribute("src");if(o){let r=ht(o,e);r!==o&&n.setAttribute("src",r)}}),document.querySelectorAll('[style*="ytimg.com"]').forEach(n=>{let o=n.getAttribute("style");if(o){let r=ti(o,e);r!==o&&n.setAttribute("style",r)}})};document.body&&t(),document.addEventListener("DOMContentLoaded",t),setTimeout(t,1e3),setTimeout(t,3e3)}var j=(e,...t)=>String.raw({raw:e},...t);var xn=[{id:"hide-mix-playlist",labelKey:"settings.userStyles.builtin.hideMixPlaylist.label",css:j`
      ytm-compact-radio-renderer:has(yt-collections-stack),
      ytm-compact-playlist-renderer:has(yt-collections-stack),
      ytm-rich-item-renderer:has(yt-collections-stack) {
        display: none !important;
      }
    `},{id:"hide-shorts-navbar",labelKey:"settings.userStyles.builtin.hideShortsInNavbar.label",css:j`
      yt-tab-shape[tab-title='Shorts'],
      ytd-guide-entry-renderer:has(a[href^='/shorts']),
      ytd-mini-guide-entry-renderer:has(a[href^='/shorts']),
      ytm-pivot-bar-item-renderer:has(.pivot-shorts) {
        display: none !important;
      }
    `},{id:"hide-community-posts",labelKey:"settings.userStyles.builtin.hideCommunityPosts.label",css:j`
      ytd-rich-section-renderer:has(ytd-post-renderer),
      ytd-post-renderer,
      ytd-backstage-post-thread-renderer,
      ytm-rich-section-renderer:has(ytm-post-renderer),
      ytm-post-renderer,
      ytm-backstage-post-thread-renderer {
        display: none !important;
      }
    `},{id:"hide-ask-button",labelKey:"settings.userStyles.builtin.hideAskButton.label",css:j`
      .you-chat-entrypoint-button,
      yt-button-view-model:has(> .you-chat-entrypoint-button) {
        display: none !important;
      }
    `},{id:"hide-home-feed",labelKey:"settings.userStyles.builtin.hideHomeFeed.label",css:j`
      ytd-browse[page-subtype='home'] #contents,
      ytm-browse [tab-identifier='FEwhat_to_watch'] ytm-rich-grid-renderer {
        display: none !important;
      }
    `},{id:"hide-related-videos",labelKey:"settings.userStyles.builtin.hideRelatedVideos.label",css:j`
      #related,
      ytd-watch-next-secondary-results-renderer,
      ytm-watch-next-secondary-results-renderer,
      ytm-item-section-renderer[section-identifier='related-items'] {
        display: none !important;
      }
    `},{id:"hide-end-screens",labelKey:"settings.userStyles.builtin.hideEndScreens.label",css:j`
      .ytp-ce-element,
      .ytp-endscreen-content,
      .ytp-suggested-video-overlay,
      .ytp-pause-overlay,
      .ytp-pause-overlay-container {
        display: none !important;
      }
    `}],xa=xn.reduce((e,t)=>(e[t.id]=t,e),{}),ni=()=>({"hide-mix-playlist":{enabled:!1},"hide-shorts-navbar":{enabled:!1},"hide-community-posts":{enabled:!1},"hide-ask-button":{enabled:!1},"hide-home-feed":{enabled:!1},"hide-related-videos":{enabled:!1},"hide-end-screens":{enabled:!1}}),Ce=()=>({schemaVersion:5,builtins:ni(),customStyles:[],customScripts:[]}),Sn=(e,t)=>{let n=t||Ce(),o=xn.filter(i=>n.builtins[i.id]?.enabled!==!1).map(i=>i.css.trim()).filter(Boolean),r=(n.customStyles||[]).filter(i=>i.enabled).filter(i=>i.css.trim()).map(i=>i.css.trim()).filter(Boolean);return[...o,...r].join(`

`)};var oi="(window.__nouUserScripts || (window.__nouUserScripts = { ran: {}, gen: 0 }))",Sa=`;${oi}.gen++;`;var Le=e=>{let t=e>>>24&255,n=e>>>16&255,o=e>>>8&255,r=e&255;return`rgba(${n}, ${o}, ${r}, ${(t/255).toFixed(3)})`},ri=(e,t)=>{switch(e){case 1:return`-1px -1px 0 ${t}, 1px -1px 0 ${t}, -1px 1px 0 ${t}, 1px 1px 0 ${t}`;case 2:return`2px 2px 2px ${t}`;case 3:return`1px 1px 0 ${t}`;case 4:return`-1px -1px 0 ${t}`;default:return"none"}},kn=e=>e<=.87?-1:e<=1.25?0:e<=1.75?1:e<=2.5?2:3,En=e=>{if(!e?.enabled)return"";let t=[],n=[],o=[];e.foregroundColor!=null&&t.push(`color: ${Le(e.foregroundColor)} !important;`),e.backgroundColor!=null&&n.push(`background-color: ${Le(e.backgroundColor)} !important;`),e.edgeType!=null&&t.push(`text-shadow: ${ri(e.edgeType,Le(e.edgeColor??4278190080))} !important;`),e.windowColor!=null&&o.push(`background-color: ${Le(e.windowColor)} !important;`);let r=[];return t.length&&r.push(`.ytp-caption-segment,
.captions-text,
.captions-text span {
  ${t.join(`
  `)}
}`),n.length&&r.push(`.ytp-caption-segment,
.captions-text span {
  ${n.join(`
  `)}
}`),o.length&&r.push(`.caption-window {
  ${o.join(`
  `)}
}`),r.join(`

`)};function b(e,t){window.NouTubeI?window.NouTubeI.onMessage(JSON.stringify({type:e,data:t})):window.electron&&window.electron.ipcRenderer.sendToHost(e,t)}function O(...e){console.log(...e),b("[content]",e.length>1?{data:[...e]}:e[0])}function ae(e,t){if(!e)return t;try{return JSON.parse(e)}catch{return t}}var _=window.trustedTypes?window.trustedTypes.createPolicy("nouPolicy",{createHTML:e=>e}):{createHTML:e=>e},D=document.location.host=="music.youtube.com",Tn=window!==window.top&&document.location.pathname.startsWith("/embed/"),gt=new Map,w={getItem(e){try{return localStorage.getItem(e)}catch{return gt.get(e)??null}},setItem(e,t){try{localStorage.setItem(e,t)}catch{gt.set(e,t)}},removeItem(e){try{localStorage.removeItem(e)}catch{gt.delete(e)}}};var bt="_nou_pip",vt="_nou_pip_ancestor",Cn=0,Me;function _t(){if(!window.NouTubePip){Me=new Map;for(let e of["outerWidth","outerHeight"]){Me.set(e,Object.getOwnPropertyDescriptor(window,e));let t=Math.max(window[e],e==="outerWidth"?screen.width:screen.height);Object.defineProperty(window,e,{configurable:!0,get:()=>t})}window.NouTubePip=!0}}async function Mn(e){let t=++Cn;if(!e){let r=!document.querySelector("video")?.paused;if(window.NouTubePip=!1,document.getElementById(bt)?.remove(),document.querySelectorAll("."+vt).forEach(i=>i.classList.remove(vt)),await new Promise(i=>setTimeout(i,500)),t!==Cn)return;for(let[i,s]of Me||[])s?Object.defineProperty(window,i,s):Reflect.deleteProperty(window,i);Me=void 0,window.dispatchEvent(new Event("resize")),r&&window.NouTube.play();return}if(_t(),document.fullscreenElement&&await document.exitFullscreen(),!window.NouTubePip)return;document.getElementById(bt)?.remove();let n=document.querySelector("video");for(let r=n?.parentElement;r;r=r.parentElement)r.classList.add(vt);let o=document.createElement("style");o.id=bt,o.textContent=`
    body *:not(._nou_pip_ancestor):not(video) { display: none !important; }
    ._nou_pip_ancestor {
      position: fixed !important;
      inset: 0 !important;
      width: 100% !important;
      height: 100% !important;
      margin: 0 !important;
      padding: 0 !important;
      transform: none !important;
      overflow: visible !important;
    }
    html, body { background: black !important; }
    video {
      visibility: visible !important;
      position: fixed !important;
      inset: 0 !important;
      width: 100vw !important;
      height: 100vh !important;
      object-fit: contain !important;
      transform: none !important;
      z-index: 2147483647 !important;
    }
  `,document.head.appendChild(o),window.dispatchEvent(new Event("resize")),window.NouTube.play()}var ii=()=>window.NouTubeToken||"",si="nou:settings",Ln="";function ai(){try{return JSON.parse(w.getItem(si)||"{}").pictureInPicture===!0}catch{return!1}}function wt(){let e=document.querySelector("video"),t=!!document.fullscreenElement||document.location.pathname=="/watch",o=ai()&&!D&&t&&e&&!e.paused&&!e.ended&&e.videoWidth>0?[e.videoWidth,e.videoHeight]:[0,0],r=o.join("x");r!=Ln&&(Ln=r,window.NouTubeI?.setPictureInPictureVideo?.(ii(),o[0],o[1]))}function Pn(){if(!(!window.isAndroid||!window.NouTubeI?.setPictureInPictureVideo)){for(let e of["play","playing","pause","ended","emptied","loadedmetadata","resize"])document.addEventListener(e,wt,!0);for(let e of["yt-navigate-finish","fullscreenchange","popstate","noutube:settings"])window.addEventListener(e,wt);wt()}}var xt=typeof globalThis=="object"&&globalThis||typeof window=="object"&&window||typeof self=="object"&&self||typeof global=="object"&&global||(function(){return this})();var Rn=typeof xt.DOMException<"u"?xt.DOMException:Error;var In=class extends Rn{constructor(e="The operation was aborted"){super(e)}};function Bn(e,t,{signal:n,edges:o}={}){let r,i=null,s=o!=null&&o.includes("leading"),a=o==null||o.includes("trailing"),l=()=>{i!==null&&(e.apply(r,i),r=void 0,i=null)},c=()=>{a&&l(),u()},m=null,f=()=>{m!=null&&clearTimeout(m),m=setTimeout(()=>{m=null,c()},t)},d=()=>{m!==null&&(clearTimeout(m),m=null)},u=()=>{d(),r=void 0,i=null},p=()=>{l()},y=function(...g){if(n?.aborted)return;r=this,i=g;let h=m==null;f(),s&&h&&l()};return y.schedule=f,y.cancel=u,y.flush=p,n?.addEventListener("abort",u,{once:!0}),y}function An(e,{signal:t}={}){return new Promise((n,o)=>{let r=()=>{o(new In)},i=()=>{clearTimeout(s),r()};if(t?.aborted)return r();let s=setTimeout(()=>{t?.removeEventListener("abort",i),n()},e);t?.addEventListener("abort",i,{once:!0})})}var Nn=0,li=Number.POSITIVE_INFINITY,Hn=()=>!0;async function q(e,t){let n,o,r,i;typeof t=="number"?(n=Nn,o=t,r=void 0,i=Hn):(n=t?.delay??Nn,o=t?.retries??li,r=t?.signal,i=t?.shouldRetry??Hn);let s;for(let a=0;a<=o;a++){if(r?.aborted)throw s??new Error("The retry operation was aborted due to an abort signal.");try{return await e()}catch(l){if(s=l,!i(l,a))throw l;if(a<o){let c=typeof n=="function"?n(a,s):n;await An(c)}}}throw s}function le(e,t,{signal:n,edges:o=["leading","trailing"]}={}){let r=null,i=Bn(function(...a){r=Date.now(),e.apply(this,a)},t,{signal:n,edges:o}),s=function(...a){if(r==null&&(r=Date.now()),Date.now()-r>=t){r=Date.now(),e.apply(this,a),i.cancel(),i.schedule();return}i.apply(this,a)};return s.cancel=i.cancel,s.flush=i.flush,s}var ci=["liveChatTextMessageRenderer","liveChatPaidMessageRenderer","liveChatPaidStickerRenderer","liveChatMembershipItemRenderer","liveChatViewerEngagementMessageRenderer","liveChatSponsorshipsGiftRedemptionAnnouncementRenderer"];function Re(e){if(typeof e!="string")return;let t=e.startsWith("//")?`https:${e}`:e;return t.startsWith("https://")?t:void 0}function $n(e=[]){return e.some(t=>t.replayChatItemAction?.actions?.length)?1e3:5e3}function On(e,t,n,o){return t-e<=Math.max(o/1e3,n*2)}var Pe=class{constructor(){this.pending=[];this.messages=new Map;this.bufferedUntil=0}clear(){this.pending=[],this.messages.clear(),this.bufferedUntil=0}append(t=[]){for(let n of t){let o=n.replayChatItemAction;if(o){let r=Number(o.videoOffsetTimeMsec)/1e3;if(!Number.isFinite(r))continue;this.bufferedUntil=Math.max(this.bufferedUntil,r),this.pending.push({time:r,actions:o.actions||[]})}}this.pending.sort((n,o)=>n.time-o.time)}advance(t){let n=!1;for(;this.pending.length&&this.pending[0].time<=t;){for(let o of this.pending.shift().actions){let r=o.addChatItemAction?.item||o.replaceChatItemAction?.replacementItem;if(r){let a=ci.map(c=>r[c]).find(c=>c?.id),l=o.replaceChatItemAction?.targetItemId;if(a){if(l&&this.messages.has(l)&&l!==a.id){let c=Array.from(this.messages,([m,f])=>m===l?[a.id,a]:[m,f]);this.messages.clear();for(let[m,f]of c)this.messages.set(m,f)}else this.messages.set(a.id,a);n=!0}else l&&(n=this.messages.delete(l)||n)}let i=o.markChatItemAsDeletedAction?.targetItemId||o.removeChatItemAction?.targetItemId;i&&(n=this.messages.delete(i)||n);let s=o.markChatItemsByAuthorAsDeletedAction?.externalChannelId||o.removeChatItemByAuthorAction?.externalChannelId;if(s)for(let[a,l]of this.messages)l.authorExternalChannelId===s&&(n=this.messages.delete(a)||n)}for(;this.messages.size>100;)this.messages.delete(this.messages.keys().next().value)}return n}};function Dn(e){let t=e?.contents?.twoColumnWatchNextResults?.conversationBar?.liveChatRenderer,n=t?.continuations?.[0]?.reloadContinuationData?.continuation;return t?.isReplay&&typeof n=="string"?n:void 0}async function qn(e,t,n=Date.now()){let o=new Map(t.split(";").map(l=>{let c=l.indexOf("=");return[l.slice(0,c).trim(),l.slice(c+1)]})),r=o.get("__Secure-3PAPISID"),i=[["SAPISIDHASH",o.get("SAPISID")||r],["SAPISID1PHASH",o.get("__Secure-1PAPISID")],["SAPISID3PHASH",r]],s=Math.floor(n/1e3),a=await Promise.all(i.filter(([,l])=>l).map(async([l,c])=>{let m=await crypto.subtle.digest("SHA-1",new TextEncoder().encode(`${s} ${c} ${e}`)),f=Array.from(new Uint8Array(m),d=>d.toString(16).padStart(2,"0")).join("");return`${l} ${s}_${f}`}));return a.length?a.join(" "):void 0}async function Un(e,t,n){let o=window.ytcfg,r=o?.get("INNERTUBE_CONTEXT");if(!r?.client)throw new Error("YouTube context is not ready");let i={"Content-Type":"application/json"},s=o.get("LOGGED_IN")===!1?void 0:await qn(location.origin,document.cookie);if(s){i.Authorization=s,i["X-Goog-AuthUser"]=String(o.get("SESSION_INDEX")??0),i["X-Origin"]=location.origin;let l=o.get("DELEGATED_SESSION_ID");l&&(i["X-Goog-PageId"]=l)}r.client.visitorData&&(i["X-Goog-Visitor-Id"]=r.client.visitorData);let a=await fetch(`${location.origin}/youtubei/v1/${e}?prettyPrint=false`,{method:"POST",headers:i,signal:n,body:JSON.stringify({context:{...r,client:{...r.client,clientName:"WEB",platform:"DESKTOP",clientFormFactor:"UNKNOWN_FORM_FACTOR"}},...t})});if(!a.ok)throw new Error(`Chat request failed: ${a.status}`);return a.json()}async function Vn(e,t){return Dn(await Un("next",{videoId:e},t))}function ce(e,t){t?.simpleText&&e.append(document.createTextNode(t.simpleText));for(let n of t?.runs||[]){n.text&&e.append(document.createTextNode(n.text));let o=n.emoji;if(o){let r=document.createElement("img");r.className="_nou_replay_emoji",r.alt=o.image?.accessibility?.accessibilityData?.label||o.shortcuts?.[0]||o.emojiId||"";let i=Re(o.image?.thumbnails?.[0]?.url);i?(r.src=i,e.append(r)):e.append(document.createTextNode(r.alt))}}}function ui(e){let t=document.createElement("article");t.className="_nou_replay_message";let n=Re(e.authorPhoto?.thumbnails?.[0]?.url);if(n){let a=document.createElement("img");a.className="_nou_replay_avatar",a.src=n,a.alt="",t.append(a)}let o=document.createElement("span"),r=document.createElement("small");ce(r,e.timestampText),o.append(r);let i=document.createElement("strong");ce(i,e.authorName),o.append(i),ce(o,e.purchaseAmountText),ce(o,e.headerSubtext),ce(o,e.message);let s=Re(e.sticker?.thumbnails?.at(-1)?.url);if(s){let a=document.createElement("img");a.className="_nou_replay_sticker",a.src=s,a.alt=e.sticker?.accessibility?.accessibilityData?.label||"Paid sticker",o.append(a)}return t.append(o),t}function zn(e,t,n){let o=document.createElement("section");o.className="_nou_replay_panel";let r=document.createElement("header"),i=document.createElement("span");i.textContent="Chat replay",r.append(i);let s=e.querySelector("._nou_livechat_close");s&&r.append(s);let a=document.createElement("p");a.textContent="Loading...";let l=document.createElement("button");l.className="_nou_replay_retry",l.textContent="Retry",l.hidden=!0;let c=document.createElement("section");c.className="_nou_replay_messages",c.setAttribute("aria-label","Chat replay messages"),o.append(r,a,l,c),e.prepend(o);let m=new Pe,f=n,d=n,u=new AbortController,p=0,y=!1,g=!1,h=!1,x,_e=5e3,B=0,C=!1,T=new Map;function on(){let z=c.scrollHeight-c.scrollTop-c.clientHeight<40,L=c.getBoundingClientRect().top,Q=z?void 0:Array.from(T.entries()).find(([S,M])=>m.messages.has(S)&&M.element.getBoundingClientRect().bottom>L),Y=Q?.[1].element.getBoundingClientRect().top;for(let[S,M]of T)m.messages.has(S)||(M.element.remove(),T.delete(S));let ne;for(let[S,M]of m.messages){let P=T.get(S);if(!P||P.message!==M){let an=ui(M);P?.element.replaceWith(an),P={message:M,element:an},T.set(S,P)}let K=ne?ne.nextElementSibling:c.firstElementChild;P.element!==K&&c.insertBefore(P.element,K),ne=P.element}if(a.hidden=!h&&m.messages.size>0,z)c.scrollTop=c.scrollHeight;else if(Q&&Y!==void 0){let S=T.get(Q[0])?.element;S&&(c.scrollTop+=S.getBoundingClientRect().top-Y)}}function it(){u.abort(),u=new AbortController,p++,g=!1,h=!1,B=0,C=!1,f=d,m.clear(),c.replaceChildren(),T.clear(),a.hidden=!1,a.textContent="Loading...",l.hidden=!0}async function xe(){let z=document.querySelector("#movie_player");if(y||z?.getPlayerResponse?.()?.videoDetails?.videoId!==t)return;let L=z.getCurrentTime?.();if(typeof L!="number"||!Number.isFinite(L))return;let Q=document.querySelector("#movie_player video")?.playbackRate||1;if(x!==void 0&&(L<x-1||L>x+Math.max(5,Q*2))&&it(),x=L,m.advance(L)&&on(),g||h||!f||performance.now()<B||C&&(z.getPlayerState?.()!==1||!On(L,m.bufferedUntil,Q,_e)))return;g=!0;let Y=p;try{let ne=await Un("live_chat/get_live_chat_replay",{continuation:f,currentPlayerState:{playerOffsetMs:String(Math.floor(L*1e3))}},u.signal);if(y||Y!==p)return;let S=ne?.continuationContents?.liveChatContinuation;if(!S)throw new Error("Chat replay is unavailable");m.append(S.actions);let M=S.continuations?.find(K=>K.liveChatReplayContinuationData)?.liveChatReplayContinuationData,P=S.continuations?.find(K=>K.playerSeekContinuationData)?.playerSeekContinuationData;f=M?.continuation||"",d=P?.continuation||n,_e=Number(M?.timeUntilLastMessageMsec??5e3),B=performance.now()+$n(S.actions),C=!0,m.advance(z.getCurrentTime())&&on(),a.hidden=m.messages.size>0,a.textContent="No messages at this point in the video."}catch{if(y||Y!==p)return;h=!0,a.hidden=!1,a.textContent="Unable to load chat replay.",l.hidden=!1}finally{Y===p&&(g=!1)}}l.onclick=()=>{it(),xe()};let rn=document.querySelector("#movie_player video"),sn=()=>{x=void 0,it(),xe()};rn?.addEventListener("seeked",sn);let Ur=setInterval(()=>{xe()},500);return xe(),()=>{y=!0,u.abort(),clearInterval(Ur),rn?.removeEventListener("seeked",sn)}}var St,Ie;function kt(){St?.(),St=void 0,document.querySelector("div#_nou_livechat")?.remove()}var di='<svg height="24" viewBox="0 0 24 24" width="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="m6 6 12 12M6 18 18 6"></path></svg>',mi='<svg height="20" viewBox="0 0 24 24" width="20" fill="currentColor"><path d="M16 3v11H7.59L5 16.59V3h11m1-1H4v17l4-4h9V2zM8 18h8l4 4V6h-1v13.59L16.41 17H8v1z"></path></svg>';function pi(e,t){kt();let n=document.createElement("div");if(n.id="_nou_livechat",n.innerHTML=_.createHTML(`
    <div id="_nou_livechat_loading">Loading...</div>
    <button class="_nou_livechat_close" aria-label="Close chat" title="Close chat">${di}</button>
  `),n.querySelector("button").onclick=kt,document.body.append(n),t)n.querySelector("#_nou_livechat_loading")?.remove(),St=zn(n,e,t);else{let o=document.createElement("iframe");o.src=`https://www.youtube.com/live_chat?v=${encodeURIComponent(e)}&embed_domain=${document.location.hostname}`,n.querySelector("#_nou_livechat_loading").after(o)}window.innerWidth>1e3&&window.innerWidth>window.innerHeight?n.classList.add("right"):n.classList.remove("right")}function Be(){Ie?.abort(),Ie=void 0,kt(),document.querySelector("button#_nou_livechat_btn")?.remove()}function Et(e,t){let n=document.querySelector("button#_nou_livechat_btn"),o=!!n;n||(n=document.createElement("button"),n.id="_nou_livechat_btn"),n.innerHTML=_.createHTML(`${mi} ${t?"Chat replay":"Live chat"}`),n.onclick=()=>pi(e,t),o||document.body.append(n)}async function jn(e){Ie?.abort();let t=new AbortController;Ie=t;try{let n=await Vn(e,t.signal);n&&!t.signal.aborted&&Et(e,n)}catch{}}var fi={en:"original",es:"original",fr:"originale",de:"original",it:"originale",pt:"original",ru:"\u043E\u0440\u0438\u0433\u0438\u043D\u0430\u043B",ja:"\u30AA\u30EA\u30B8\u30CA\u30EB",ko:"\uC6D0\uBCF8",zh:"\u539F\u59CB",ar:"\u0623\u0635\u0644\u064A",hi:"\u092E\u0942\u0932",bn:"\u09AE\u09C2\u09B2",id:"asli",tr:"orijinal",vi:"nguy\xEAn b\u1EA3n",th:"\u0E15\u0E49\u0E19\u0E09\u0E1A\u0E31\u0E1A",nl:"origineel",pl:"oryginalny",sv:"original",fi:"alkuper\xE4inen",no:"original",da:"original",cs:"origin\xE1l",el:"\u03C0\u03C1\u03C9\u03C4\u03CC\u03C4\u03C5\u03C0\u03BF",he:"\u05DE\u05E7\u05D5\u05E8\u05D9",ro:"original",hu:"eredeti",uk:"\u043E\u0440\u0438\u0433\u0456\u043D\u0430\u043B",fa:"\u0627\u0635\u0644\u06CC",ms:"asal",tl:"orihinal",ta:"\u0B85\u0B9A\u0BB2\u0BCD",ur:"\u0627\u0635\u0644",hr:"izvorni",bg:"\u043E\u0440\u0438\u0433\u0438\u043D\u0430\u043B\u0435\u043D",sk:"origin\xE1l",sl:"izvirnik",sr:"\u043E\u0440\u0438\u0433\u0438\u043D\u0430\u043B",lt:"originalus",lv:"ori\u0123in\u0101ls",et:"originaal",ka:"\u10DD\u10E0\u10D8\u10D2\u10D8\u10DC\u10D0\u10DA\u10D8",az:"orijinal",uz:"asl",kk:"\u0442\u04AF\u043F\u043D\u04B1\u0441\u049B\u0430",hy:"\u0562\u0576\u0585\u0580\u056B\u0576\u0561\u056F",km:"\u178A\u17BE\u1798",my:"\u1019\u1030\u101B\u103A\u1038",si:"\u0DB8\u0DD4\u0DBD\u0DCA",am:"\u12E8\u1218\u1300\u1218\u122A\u12EB\u12CD",ne:"\u092E\u094C\u0932\u093F\u0915",lo:"\u0E95\u0EBB\u0EC9\u0E99\u0EAA\u0EB0\u0E9A\u0EB1\u0E9A",mn:"\u044D\u0445",bo:"\u0F51\u0F44\u0F7C\u0F66\u0F0B\u0F42\u0F5E\u0F72\u0F0D",cy:"gwreiddiol",eo:"originala",eu:"jatorrizko",gl:"orixinal",gu:"\u0AAE\u0AC2\u0AB3",ha:"asali",ig:"izugbe",is:"upprunalegt",jv:"asli",kn:"\u0CAE\u0CC2\u0CB2",ku:"or\xEEj\xEEnal",ky:"\u0442\u04AF\u043F \u043D\u0443\u0441\u043A\u0430",lb:"original",mi:"taketake",ml:"\u0D2F\u0D25\u0D3E\u0D7C\u0D24\u0D4D\u0D25",mr:"\u092E\u0942\u0933",mt:"ori\u0121inali",or:"\u0B2E\u0B4C\u0B33\u0B3F\u0B15",pa:"\u0A05\u0A38\u0A32",ps:"\u0627\u0635\u0644\u064A",sd:"\u0627\u0635\u0644",sm:"muamua",sn:"chaicho",so:"asalka ah",sq:"origjinal",su:"asli",sw:"asili",te:"\u0C05\u0C38\u0C32\u0C41",tk:"asyl",tt:"\u043E\u0440\u0438\u0433\u0438\u043D\u0430\u043B\u044C",ug:"\u0626\u06D5\u0633\u0644\u0649",yi:"\u05D0\u05E8\u05D9\u05D2\u05D9\u05E0\u05E2\u05DC",yo:"atil\u1EB9ba",zu:"okwangempela",af:"oorspronklik",bs:"originalni",ca:"original",ceb:"orihinal",co:"uriginale",fy:"oarspronklik",ga:"bunaidh",gd:"t\xF9sail",haw:"kumu",hmn:"thawj",ht:"orijinal",ia:"original",ie:"original",io:"originala",kw:"gwredhek",la:"originalis",ln:"ya liboso",mg:"tany am-boalohany",mk:"\u043E\u0440\u0438\u0433\u0438\u043D\u0430\u043B\u0435\u043D",ny:"yoyamba",oc:"original",qu:"qallariy",rm:"original",rw:"cy'umwimerere",sa:"\u092E\u0942\u0932",sc:"originale",st:"ea pele",tg:"\u0430\u0441\u043B\u04E3",to:"\u02BBuluaki",wo:"nj\xEBkk",xh:"yokuqala","zh-tw":"\u539F\u59CB"},Wn=[...new Set(Object.values(fi))];function Ae(){let e=w.getItem("nou:settings");if(!e)return!1;try{return JSON.parse(e).sponsorBlock}catch{return!1}}var Fn={sponsor:"#00d400",selfpromo:"#ffff00",interaction:"#cc00ff",intro:"#00ffff",outro:"#0202ed",preview:"#008fd6",music_offtopic:"#ff9900",poi_highlight:"#ff1684",filler:"#7300ff"},Tt="_nou_sb_segments",yi="_nou_sb_segment",hi=[".ytChapteredProgressBarHost",".ytp-progress-list",".progress-bar-line",".progress-bar-background"],gi=[".ytChapteredProgressBarChapteredPlayerBarChapter",".ytp-chapter-hover-container"];function bi(){let e=[];for(let t of hi)for(let n of document.querySelectorAll(t))n.offsetWidth>0&&e.push(n);return e}function vi(e){let t=[];for(let i of gi)if(t=Array.from(e.querySelectorAll(i)),t.length)break;if(!t.length)return[{start:0,end:1,left:0,width:e.offsetWidth}];let n=t.reduce((i,s)=>i+s.offsetWidth,0),o=[],r=0;for(let i of t){let s=parseFloat(i.style.width),a=i.style.width.endsWith("%")&&Number.isFinite(s)?s/100:i.offsetWidth/(n||1);o.push({start:r,end:r+a,left:i.offsetLeft,width:i.offsetWidth}),r+=a}return o[o.length-1].end=1,o}function Ct(){for(let e of document.querySelectorAll(`.${Tt}`))e.parentElement?.removeAttribute("data-nou-sb"),e.remove()}function Ne(e,t,n){if(!e||!n||!Number.isFinite(n))return;let o=t.map(({actionType:r,category:i,segment:s})=>`${r}:${i}:${s[0]}:${s[1]}`).join(",");for(let r of bi()){let i=r.querySelector(`.${Tt}`),s=vi(r),a=s.map(({start:c,end:m,left:f,width:d})=>`${c}:${m}:${f}:${d}`).join(","),l=`${e}:${n}:${o}:${r.offsetWidth}:${a}`;i&&r.dataset.nouSb===l||(i?.remove(),getComputedStyle(r).position=="static"&&(r.style.position="relative"),r.append(wi(t,n,s)),r.dataset.nouSb=l)}}function wi(e,t,n){let o=document.createElement("div");o.className=Tt;for(let r of e){if(r.actionType=="full")continue;let i=Fn[r.category]||Fn.sponsor,s=Math.max(0,Math.min(1,r.segment[0]/t)),a=Math.max(s,Math.min(1,r.segment[1]/t));for(let l of n){let c=Math.max(s,l.start),m=Math.min(a,l.end);if(s==a?m<c:m<=c)continue;let f=l.end-l.start;if(f<=0)continue;let d=(m-c)/f*l.width;if(s!=a&&d<.5)continue;let u=document.createElement("div");u.className=yi,u.style.left=`${l.left+(c-l.start)/f*l.width}px`,u.style.width=`${Math.max(s==a?2:1,d)}px`,u.style.background=i,o.append(u)}}return o}async function Qn(e){let t=await fetch(`https://sponsor.ajay.app/api/skipSegments?videoID=${e}`),n=[];return t.status==200&&(n=await t.json()),{videoId:e,segments:n}}var ue=[.25,.5,.75,1,1.25,1.5,1.75,2,2.5,3,3.5,4];function Yn(e){return`${e.toFixed(2).replace(/\.?0+$/,"")}x`}function Kn(e){return!Number.isFinite(e)||ue.includes(e)?ue:[...ue,e].sort((t,n)=>t-n)}var v,R="",A={videoId:"",segments:[]},Gn=!1,Jn="_nou_fullscreen_title",$e={playing:"nou:playing",settings:"nou:settings"};function to(){return ae(w.getItem($e.settings),{})}function _i(){let{playbackRate:e}=to();return typeof e=="number"&&Number.isFinite(e)?e:1}function xi(){let{playbackQuality:e}=to();return typeof e=="string"?e:"auto"}function He(e){let t=document.querySelector("#movie_player video")||e?.querySelector?.("video")||document.querySelector("video");return t instanceof HTMLVideoElement?t:null}function Si(e){if(!window.isAndroid)return;let t=document.querySelector("#player-control-overlay .player-controls-content");if(!t||t.querySelector("player-fullscreen-top-controls"))return;let n=t.querySelector(`#${Jn}`);n||(n=document.createElement("div"),n.id=Jn,t.append(n)),n.textContent=e}function ki(e){if(!(!e||e.__nouPlaybackRatesExtended))try{let t=e.getAvailablePlaybackRates?.bind(e),n=e.setPlaybackRate?.bind(e);if(!t||!n)return;e.getAvailablePlaybackRates=()=>ue,e.setPlaybackRate=o=>{let r=He(e);r&&(r.playbackRate=o),o<=2&&n(o),b("playback-rate",{playbackRate:o})},e.__nouPlaybackRatesExtended=!0}catch(t){O("failed to extend playback rates",t)}}function Ei(e){let t=_i();try{e.setPlaybackRate?.(t)}catch(n){O("failed to apply playback rate",n)}}function Ti(e){if(!(!e||e.__nouPlaybackQualityExtended))try{let t=e.setPlaybackQuality?.bind(e),n=e.setPlaybackQualityRange?.bind(e);t&&(e.setPlaybackQuality=o=>{t(o),b("playback-quality",{playbackQuality:o})}),n&&(e.setPlaybackQualityRange=(o,r)=>{n(o,r),b("playback-quality",{playbackQuality:o})}),e.__nouPlaybackQualityExtended=!0}catch(t){O("failed to extend playback quality",t)}}function Ci(e){let t=xi();try{let n=e.getAvailableQualityLevels?.();if(t==="auto")return;Array.isArray(n)&&n.includes(t),e.setPlaybackQualityRange?e.setPlaybackQualityRange(t,t):e.setPlaybackQuality&&e.setPlaybackQuality(t)}catch(n){O("failed to apply playback quality",n)}}function no(e){for(let t of e)for(let n of t.addedNodes.values()){let o=n;o.id=="movie_player"&&Oe(o)}if(!v?.__nouPlayerBound||!v.isConnected){let t=document.querySelector("#movie_player");t&&Oe(t)}}function Oe(e){if(v=e,e.__nouPlayerBound)return;e.__nouPlayerBound=!0,ki(v),Ti(v),e.addEventListener("onApiChange",()=>de(e)),de(e);let t="",n=0,o=u=>{let{videoDetails:p}=e.getPlayerResponse?.()||{},y=p?.videoId,g=v.getVideoUrl?.()||"";if(!(!y||!g))return{url:g,videoId:y,title:p.title||"",author:p.author||"",thumbnail:p.thumbnail?.thumbnails?.at(-1)?.url||"",current:u,duration:Number(p.lengthSeconds)||0}},r=le(u=>{w.setItem($e.playing,JSON.stringify({url:u.url,current:u.current})),b("progress",u)},5e3),i=()=>{let u=He(v);return u?!u.paused:e.getPlayerState()==1},s=le(()=>{if(window.NouTubeI)return;let u=o(e.getCurrentTime?.()??0);u&&b("playback",{...u,playing:i(),rate:He(v)?.playbackRate??1})},5e3),a=le(()=>{if(!e.getCurrentTime){Be();return}let u=e.getCurrentTime();window.NouTubeI?.notifyProgress(i(),u),s();let p=o(u);if(p&&r(p),Ae()&&R==A.videoId&&A.segments.length){Ne(R,A.segments,n||e.getDuration?.()||0);for(let y of A.segments){let[g,h]=y.segment;if(u>g&&u<h){v.seekTo(h);return}}}else Ct()},1e3),l=()=>{window.NouTubeI?.notifyProgress(i(),e.getCurrentTime?.()??0),s(),s.flush()},c=()=>{Ae()&&R==A.videoId&&A.segments.length&&Ne(R,A.segments,n||e.getDuration?.()||0)};Gn||(Gn=!0,window.addEventListener("resize",c),document.addEventListener("fullscreenchange",()=>{c(),setTimeout(c,400)}));let m=!1,f=async u=>{b("play-state",{playing:u==1});let{playabilityStatus:p,videoDetails:y}=e.getPlayerResponse()||{};if(!y){Be();return}if(u==0&&!D&&(w.removeItem($e.playing),b("playback-end")),document.location.host=="m.youtube.com"&&document.location.pathname=="/"){e.pauseVideo();return}if(!m){let C=He(v);C&&(C.addEventListener("timeupdate",a),["play","pause"].forEach(T=>{C.addEventListener(T,()=>{l(),a()})}),C.addEventListener("ratechange",()=>{b("playback-rate",{playbackRate:C.playbackRate}),s(),s.flush()}),m=!0)}let{title:g,author:h,thumbnail:x,lengthSeconds:_e,videoId:B}=y;if(Si(g),R!=B){r.cancel(),Tn||v.unMute();let C=x.thumbnails.at(-1);if(n=+_e,t=g,window.NouTubeI?.notify(t,h,n,C?.url||""),R=B,window.NouTubeI&&(Li(),Be(),p?.liveStreamability?Et(R):y.isLiveContent&&jn(R)),Ct(),A={videoId:"",segments:[]},Ae()){let T=await Qn(B);if(R!==B)return;A=T,Ne(B,T.segments,n)}Ei(v),Ci(v)}};e.addEventListener("onStateChange",f);let d=e.getPlayerState?.();(d===1||d===2||d===3)&&f(d)}document.addEventListener("click",e=>{e.target?.closest?.(".fullscreen-icon, .ytp-fullscreen-button")&&(window.NouTubeFsIntent=Date.now())},!0);screen.orientation.addEventListener("change",e=>{if(window.NouTubePip||window.NouTubeBackground||document.location.pathname!="/watch"||document.visibilityState!="visible")return;e.target.type.includes("landscape")?!document.fullscreenElement&&screen.availWidth<1e3&&document.querySelector("#player-control-container .fullscreen-icon")?.click():document.fullscreenElement&&document.exitFullscreen()});async function oo(){await q(()=>{if(!v)throw"player not ready";return v},{retries:30,delay:100}),v?.playVideo();let e=await q(()=>{let s=v.getAvailableAudioTracks();if(!s.length)throw"tracks not ready";return s},{retries:30,delay:100}),t="",n,o=0;for(let s of e){for(let a of Object.values(s)){let l=a;l&&typeof l=="object"&&"isDefault"in l&&l.name&&(Wn.some(c=>l.name.includes(c))&&(n=o.toString(),v.setAudioTrack(s)),t+=`<option value="${o}">${l.name}</option>`)}o++}let r=document.querySelector("div#_nou_audio_picker");r||(r=document.createElement("div"),r.id="_nou_audio_picker",document.body.append(r)),r.innerHTML=_.createHTML(`
    <select>
      ${t}
    </select>
  `);let i=r.querySelector("select");i&&(n&&(i.value=n),i.onchange=s=>{let a=s.target.value;a!=null&&v.setAudioTrack(e[+a])})}async function Li(){if(document.location.pathname!="/watch"||D)return;let e=await q(async()=>{let n=document.querySelector("ytm-slim-video-information-renderer ytm-badge-supported-renderer");if(!n)throw"badge not ready";return n},{retries:30,delay:100});if(!e)return;let t=document.createElement("div");t.id="_nou_audio_btn",t.innerHTML=_.createHTML(`
    Play original audio \u{1F9A6}
  `),t.onclick=n=>{n.stopPropagation(),v.pauseVideo(),b("embed",R)},e.append(t)}var Xn=5;function Zn(e){try{return new URL(e,document.location.href).searchParams.get("v")||""}catch{return""}}function eo(e){try{return new URL(e,document.location.href).searchParams.has("t")}catch{return!1}}async function Mi(e){try{(await q(async()=>{let n=document.getElementById("movie_player");if(!n?.seekTo)throw"player not ready";return n},{retries:30,delay:100}))?.seekTo(e)}catch(t){O("failed to seek to the saved position",t)}}function ro(){let e=ae(w.getItem($e.playing),{});if(!e.url)return;let t=Number(e.current)||0,n=Zn(e.url);if(n&&n==Zn(document.location.href)){t>=Xn&&!eo(document.location.href)&&Mi(t);return}document.location.href=t>=Xn&&!eo(e.url)?Pi(e.url,t):e.url}function Pi(e,t){try{let n=new URL(e,document.location.href);return n.searchParams.set("t",`${Math.floor(t)}s`),n.href}catch{return e}}var Ri=()=>document.getElementById("movie_player"),Ii=e=>({itag:e?.itag,mimeType:e?.mimeType,qualityLabel:e?.qualityLabel,height:e?.height,fps:e?.fps,bitrate:e?.bitrate,averageBitrate:e?.averageBitrate,contentLength:e?.contentLength}),so=e=>{let t=e?.streamingData,n=[...t?.formats??[],...t?.adaptiveFormats??[]];return n.length?{videoId:e?.videoDetails?.videoId??"",title:e?.videoDetails?.title??"",formats:n.map(Ii)}:null},Bi=e=>{try{let t=Ri()?.getPlayerResponse?.();return!t||t?.videoDetails?.videoId!==e?null:so(t)}catch{return null}},io=e=>{let t=window.ytcfg;try{return t?.get?.(e)??t?.data_?.[e]}catch{return t?.data_?.[e]}},Ai=async e=>{let t=io("INNERTUBE_CONTEXT");if(!t)return null;let n=io("INNERTUBE_API_KEY"),o=await fetch(`/youtubei/v1/player?prettyPrint=false${n?`&key=${n}`:""}`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({context:t,videoId:e,contentCheckOk:!0,racyCheckOk:!0})});if(!o.ok)return null;let r=await o.json(),i=r?.playabilityStatus?.status;return i&&i!=="OK"?null:so(r)},ao=async e=>Bi(e)??Ai(e).catch(()=>null);function Lt(e){try{let t=new URL(e);return["http:","https:"].includes(t.protocol)&&["youtube.com","www.youtube.com","m.youtube.com"].includes(t.host)&&t.pathname==="/watch"}catch{return!1}}function lo(e=window){Object.defineProperties(e,{outerWidth:{configurable:!0,get:()=>e.screen.width},outerHeight:{configurable:!0,get:()=>e.screen.height}})}function co(e){let t=typeof e.composedPath=="function"?e.composedPath():[],n=e.target,o=[...t,n?.closest?.("a[href], ytm-home-logo")];for(let r of o){let i=r;if(i?.tagName==="A"){let s=i,a=s.getAttribute("href");if(a)return{href:a,target:s.target}}if(i?.tagName==="YTM-HOME-LOGO")return{href:"/"}}return null}var Ni=["m.youtube.com","www.youtube.com","youtube.com","music.youtube.com"],Hi=400,me=e=>Lt(e.href);function $i(){let e=window.NouTubeRole;return e==="browse"||e==="player"?e:null}function po(){let e=window;return e.NouTubePlayerWarmup&&Lt(location.href)&&(e.NouTubePlayerWarmup=!1,pe(!1)),!!e.NouTubePlayerWarmup}function qe(e){if(!e)return null;try{let t=new URL(e,location.href);return Ni.includes(t.host)?t:null}catch{return null}}function fo(e,t){return t==="browse"?me(e):!me(e)}function yo(e,t){b(t==="browse"?"open-watch":"open-page",{url:e.href})}function Oi(e){document.addEventListener("click",t=>{if(t.defaultPrevented||t.button!==0||t.metaKey||t.ctrlKey||t.shiftKey)return;let n=co(t);if(!n||n.target==="_blank")return;let o=qe(n.href);!o||!fo(o,e)||(t.preventDefault(),t.stopImmediatePropagation(),yo(o,e))},!0)}function Di(){let e=history.pushState;history.pushState=function(...t){let n=qe(typeof t[2]=="string"?t[2]:location.href),o=qe(location.href);return n&&o&&me(n)&&me(o)?history.replaceState.apply(this,t):e.apply(this,t)}}function qi(e){let t="",n=()=>{if(e==="player"&&po())return;let i=qe(location.href);if(!i||!fo(i,e)){t="";return}if(i.href!==t){t=i.href,yo(i,e);try{history.back()}catch{}}},o,r=()=>{clearTimeout(o);let i=location.href;o=setTimeout(()=>{location.href===i&&n()},Hi)};window.addEventListener("popstate",r),window.addEventListener("yt-navigate-finish",r),document.addEventListener("yt-navigate-finish",r);for(let i of["pushState","replaceState"]){let s=history[i];history[i]=function(...a){let l=s.apply(this,a);return r(),l}}}function Ue(e){window.NouTubePlayerWarmup&&(window.NouTubePlayerWarmup=!1,pe(!1));try{let t=new URL(e,location.href);me(t)&&(t.host=location.host);let n=document.createElement("a");n.href=t.href,n.style.display="none",document.body.appendChild(n),n.click(),n.remove()}catch{location.href=e}return!0}var k="_nou_native_mini",uo="_nou_native_mini_style",Ui=`
  html.${k}, html.${k} body {
    overflow: hidden !important;
    background: #000 !important;
  }
  /* The mobile player's parent has its own stacking context. A body backdrop
     covers that entire context, regardless of the video's z-index. Hide the
     surrounding page instead, keeping the live player in its original DOM. */
  html.${k} body * {
    visibility: hidden !important;
  }
  html.${k} #movie_player,
  html.${k} #movie_player * {
    visibility: visible !important;
  }
  /* Ancestors must not clip or offset the fixed player in the small viewport. */
  html.${k} body :has(#movie_player) {
    position: static !important;
    transform: none !important;
    contain: none !important;
    overflow: visible !important;
  }
  html.${k} #movie_player {
    position: fixed !important;
    inset: 0 !important;
    width: 100vw !important;
    height: 100vh !important;
    max-width: none !important;
    max-height: none !important;
    margin: 0 !important;
    z-index: 2147483647 !important;
    background: #000 !important;
  }
  /* The player sizes its video in inline pixels, which the rule above does not
     touch, so the video has to be told to fill the box as well. */
  html.${k} #movie_player .html5-video-container,
  html.${k} #movie_player video {
    position: absolute !important;
    inset: 0 !important;
    width: 100% !important;
    height: 100% !important;
    left: 0 !important;
    top: 0 !important;
    object-fit: contain !important;
  }
  /* Controls are the app's job in the mini player: the box is too small to hit
     anything, and a tap on it means "give me the video back". */
  html.${k} #movie_player .ytp-chrome-top,
  html.${k} #movie_player .ytp-chrome-bottom,
  html.${k} #movie_player #player-control-overlay,
  html.${k} #movie_player .ytp-gradient-top,
  html.${k} #movie_player .ytp-gradient-bottom {
    display: none !important;
  }
`;function It(e){let t=document.documentElement;if(t){if(e&&!document.getElementById(uo)){let n=document.createElement("style");n.id=uo,n.textContent=Ui,(document.head||t).appendChild(n)}t.classList.toggle(k,!!e);try{window.dispatchEvent(new Event("resize"))}catch{}}}var De=!1,G=null,Mt=new WeakSet;function mo(){for(let e of Array.from(document.querySelectorAll("video, audio"))){let t=e;De?t.muted||(t.muted=!0,Mt.add(t)):Mt.has(t)&&(Mt.delete(t),t.muted=!1)}}function pe(e){De=!!e,mo(),De&&!G?(G=new MutationObserver(()=>mo()),G.observe(document.documentElement,{childList:!0,subtree:!0})):!De&&G&&(G.disconnect(),G=null)}var Bt=e=>e.tagName.indexOf("MINIPLAYER")!==-1||(e.getAttribute("class")||"").indexOf("iniplayer")!==-1;function Pt(e){let t=e;if(!t||t.nodeType!==1||!Bt(t)||!t.querySelector("video"))return!1;let n=t.getBoundingClientRect();return n.width>0&&n.height>0}var ho=new WeakSet;function Rt(e){let t=e;return!t||t.nodeType!==1||!ho.has(t)?!1:Array.from(t.querySelectorAll("video")).some(n=>!n.paused)}function Vi(e){let t=Array.from(e.querySelectorAll('[class*="lose" i]')).filter(n=>!n.closest("#movie_player, .html5-video-player"));return t.find(n=>Bt(n))||t[0]}function zi(e){let t=e,n=()=>Pt(e)||Rt(e);try{t.setActive?.(!1)}catch{}if(n()&&e.hasAttribute("active"))try{t.active=!1}catch{}if(n())try{(e.closest("ytd-app")||document.documentElement).dispatchEvent(new CustomEvent("yt-close-miniplayer",{bubbles:!0,composed:!0}))}catch{}if(n()&&Vi(e)?.click(),n()){for(let o of Array.from(e.querySelectorAll("video")))try{o.pause()}catch{}try{e.style?.setProperty("display","none","important"),ho.add(e)}catch{}}}function ji(){let e=new WeakSet,t=new Set,n=i=>{if(!i)return null;for(let s of t)if(!s.isConnected)t.delete(s);else if(s===i||s.contains(i))return s;return null},o=i=>{i&&i.nodeType===1&&Bt(i)&&t.add(i),!(!Pt(i)&&!Rt(i)||e.has(i))&&(e.add(i),requestAnimationFrame(()=>{e.delete(i),(Pt(i)||Rt(i))&&zi(i)}))},r=()=>{for(let i of Array.from(document.querySelectorAll("*")))o(i);new MutationObserver(i=>{for(let s of i)if(s.type==="childList"){for(let a of Array.from(s.addedNodes))o(a);o(n(s.target))}else o(s.target)}).observe(document.documentElement,{subtree:!0,childList:!0,attributes:!0,attributeFilter:["class","active","hidden","style"]}),document.addEventListener("play",i=>{o(n(i.target))},!0)};document.documentElement?r():document.addEventListener("DOMContentLoaded",r,{once:!0})}function go(){let e=$i();if(!e||location.host==="music.youtube.com")return;let t=window;if(!t.__nouSplitViewInstalled){if(t.__nouSplitViewInstalled=!0,Oi(e),qi(e),e==="browse"&&ji(),e==="player"){lo(),Di();let n=()=>{po()&&pe(!0)};document.documentElement?n():document.addEventListener("DOMContentLoaded",n,{once:!0})}window.NouTubeNativeMini&&It(!0)}}var Nt=e=>e?.playlistPanelVideoRenderer?.videoId,bo=e=>Nt(e?.contents?.find?.(t=>t?.playlistPanelVideoRenderer?.selected)),Ve=(e,t)=>!!t&&bo(e)==t;function vo(e){let t=e?.contents;return[t?.singleColumnWatchNextResults,t?.twoColumnWatchNextResults].map(n=>n?.playlist?.playlist).filter(n=>Array.isArray(n?.contents))}function wo(e,t){return vo(e).find(n=>Ve(n,t))}function _o(e,t,n){if(!t||!n)return;let o=e.flatMap(vo).filter(r=>r.playlistId==n);return o.find(r=>Ve(r,t))||o.find(r=>r.contents.some(i=>Nt(i)==t))}function At(e,t){let n=e?.commandMetadata?.webCommandMetadata?.url;if(typeof n=="string"&&n.includes("v="))return n;let o=e?.watchEndpoint?.videoId;if(!o)return;let r=e.watchEndpoint.playlistId||t;return`/watch?v=${o}${r?`&list=${r}`:""}`}function xo(e,t,n){let o=Array.isArray(e?.contents)?e.contents:[],r=bo(e),i=n||r,s=!!r&&r==i,a=o.findIndex(f=>s?f?.playlistPanelVideoRenderer?.selected:Nt(f)==i);if(!i||a<0)return;let c=(t=="next"?e.nextButtonVideo:e.previousButtonVideo)?.buttonRenderer?.command;if(s&&c?.watchEndpoint?.videoId!=r){let f=At(c,e.playlistId);if(f)return f}let m=o[a+(t=="next"?1:-1)]?.playlistPanelVideoRenderer;if(m?.videoId)return At(m.navigationEndpoint,e.playlistId)||At({watchEndpoint:{videoId:m.videoId}},e.playlistId)}var N="noutube:settings",$t="noutube:user-styles",Ot="noutube:blocklist",fe={},ze=Ce(),je=oe(),E=()=>document.getElementById("movie_player"),So=!1;function Wi(){So||(So=!0,window.addEventListener("keyup",e=>{b("keyup",{key:e.key,metaKey:e.metaKey,ctrlKey:e.ctrlKey})}),window.addEventListener("paste",e=>{if(ko(e.target)||ko(document.activeElement))return;let t=e.clipboardData?.getData("text");t&&b("paste",t)}))}function ko(e){if(!e)return!1;let t=e.tagName?.toUpperCase?.();return t==="INPUT"||t==="TEXTAREA"||t==="SELECT"||e.isContentEditable===!0}function Fi(){return fe}function Eo(e={}){return fe={...fe,...e},window.dispatchEvent(new CustomEvent(N,{detail:fe})),fe}function Qi(){return ze}function To(e){return ze=e||Ce(),window.dispatchEvent(new CustomEvent($t,{detail:ze})),ze}function Yi(){return je}function Co(e){return je=e||oe(),window.dispatchEvent(new CustomEvent(Ot,{detail:je})),je}function Lo(e){if(D)return!1;let t=E(),n=t?.getVideoData?.()?.video_id,o=t?.getWatchNextResponse?.(),r=wo(o,n)||_o([o,window.ytInitialData],n,new URLSearchParams(location.search).get("list"));if(!r)return!1;let i=xo(r,e,n);return i&&(Ve(r,n)?Ue(i):location.assign(i)),!0}function Mo(){if(Lo("previous"))return;let e=E();e?.seekTo?.(0),e?.previousVideo()}function Po(){let e=ye;if(ye=void 0,e?.isConnected){Ht=!0;try{e.click()}finally{Ht=!1}return}Lo("next")||E()?.nextVideo()}function Ro(){if(window.NouTubeI){let e=E()?.getVideoUrl?.()||"";b("playback-next",{url:e.includes("v=")?e:document.location.href})}else Po()}var ye,Ht=!1;function Ki(e){let t=e?.closest?.(".player-middle-controls-prev-next-button"),n=t?.closest(".player-controls-middle")?.querySelectorAll(".player-middle-controls-prev-next-button");if(t&&n&&n.length>1&&n[n.length-1]==t)return t}function Gi(){document.addEventListener("click",e=>{if(Ht)return;let t=Ki(e.target);!t||t.getAttribute("aria-disabled")=="true"||(e.preventDefault(),e.stopImmediatePropagation(),ye=t,setTimeout(()=>{ye==t&&(ye=void 0)},2e3),Ro())},!0)}function Ji(){document.addEventListener("click",e=>{e.target.closest?.(".previous-button")?.closest("ytmusic-player-bar,ytmusic-player-controls")&&(e.preventDefault(),e.stopImmediatePropagation(),Mo())},!0)}function Io(){return window.NouTubeInitialSettings&&Eo(window.NouTubeInitialSettings),window.NouTubeBlocklist&&Co(window.NouTubeBlocklist),window.NouTubeUserStyles&&To(window.NouTubeUserStyles),Pn(),D?Ji():window.NouTubeI&&Gi(),{getSettings:Fi,setSettings:Eo,getUserStyles:Qi,setUserStyles:To,getBlocklist:Yi,setBlocklist:Co,preparePictureInPicture:_t,setPictureInPicture:Mn,shortsHidden:!0,play:()=>E()?.playVideo(),pause:()=>E()?.pauseVideo(),prev:Mo,next:Ro,skipToNext:Po,seekBy:e=>E()?.seekBy(e),seekTo:e=>E()?.seekTo(e),getVideoUrl:()=>E()?.getVideoUrl?.()||"",getPlaybackRate:()=>E()?.getPlaybackRate?.(),setPlaybackRate:e=>E()?.setPlaybackRate?.(e),getPlaybackQuality:()=>E()?.getPlaybackQuality?.(),setPlaybackQuality:e=>{let t=E();t&&(t.setPlaybackQualityRange?t.setPlaybackQualityRange(e,e):t.setPlaybackQuality&&t.setPlaybackQuality(e))},hideShorts(){Bo(),this.shortsHidden=!0},showShorts(){Ao(),this.shortsHidden=!1},playDefaultAudio:oo,restoreLastPlaying:ro,bridgeShortcuts:Wi,getPlayerFormats:ao}}var No=null,Dt="nou:caption-font-size",Xi=()=>{try{let e=w.getItem(Dt);if(e==null)return null;let t=Number(e);return Number.isFinite(t)?t:null}catch{return null}},Ho=()=>window.NouTube?.getSettings?.()?.captionStyle||null,$o=()=>En(Ho());function de(e){e&&(No=e);let t=No||document.getElementById("movie_player");if(!t||typeof t.setOption!="function")return;let n=t.getOptions?.();if(!Array.isArray(n)||!n.includes("captions"))return;let o=Ho(),r=Xi();if(!o?.enabled){if(r!=null)try{t.setOption("captions","fontSize",r),w.removeItem(Dt)}catch{}return}if(r==null)try{let i=t.getOption?.("captions","fontSize");w.setItem(Dt,String(typeof i=="number"?i:0))}catch{}try{t.setOption("captions","fontSize",kn(o.fontScale))}catch{}}function qt(){window.addEventListener(N,()=>de()),de()}var Oo="_nou_injected_css",he=(e,...t)=>String.raw({raw:e},...t),Zi=he`
  /*
   * Text zoom (webView textZoom) scales fonts but not the fixed pixel heights
   * YouTube hardcodes on its text containers, so zoomed titles/headlines get
   * clipped. Drop those height clamps so the boxes grow with the text;
   * -webkit-line-clamp still truncates long titles to the intended line count.
   *
   * The player is excluded: its own class list carries title-related flags
   * (ytp-hide-fullscreen-title), and forcing height:auto on #movie_player
   * collapses it to 0, clipping the video away.
   */
  :is(
      [class*='headline' i],
      [class*='title' i],
      [class*='subhead' i],
      [class*='channel-name' i]
    ):not(#movie_player, #movie_player *) {
    height: auto !important;
    max-height: none !important;
  }

  /*
   * YouTube's control scrim fades to fully transparent at the bottom, but the
   * progress bar sits ~80% down, so the track (white at 35% alpha) disappears
   * over a bright frame. Darken the bottom and outline the bar to keep it and
   * the SponsorBlock segments readable.
   */
  #player-control-overlay .player-controls-background::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(to top, rgba(0, 0, 0, 0.7), rgba(0, 0, 0, 0) 35%);
    pointer-events: none;
  }

  #player-control-overlay .ytPlayerProgressBarHost {
    filter: drop-shadow(0 1px 1px rgba(0, 0, 0, 0.6));
  }

  /*
   * YouTube Music mobile: when minimized to the bottom player bar,
   * YouTube Music's collapse selector typo leaves ytmusic-app-layout fixed and
   * leaves full player page backgrounds and overlays active over browsing content.
   */
  ytmusic-app-layout[player-ui-state='PLAYER_BAR_ONLY'] {
    position: static !important;
    animation: none !important;
    transform: none !important;
  }

  ytmusic-player-page[player-ui-state='PLAYER_BAR_ONLY'] {
    visibility: hidden !important;
    background: transparent !important;
    pointer-events: none !important;
  }

  ytmusic-player-page[player-ui-state='PLAYER_BAR_ONLY'] ytmusic-player {
    visibility: visible !important;
    pointer-events: auto !important;
  }

  /*
   * YouTube Music mobile fullscreen sizes the player to 56.25vw (16:9 of the
   * width), which overflows the screen vertically on phones wider than 16:9.
   */
  ytmusic-player[player-ui-state='FULLSCREEN'][is-mweb-modernization-enabled][video-mode] {
    height: min(56.25vw, 100vh) !important;
  }

  #_nou_fullscreen_title {
    display: none;
  }

  #player-container-id:fullscreen #_nou_fullscreen_title {
    display: block;
    position: absolute;
    top: 14px;
    left: 24px;
    right: 192px;
    z-index: 2;
    overflow: hidden;
    color: white;
    font-size: 18px;
    font-weight: 500;
    line-height: 24px;
    text-overflow: ellipsis;
    white-space: nowrap;
    pointer-events: none;
  }

  /*
   * Newer YouTube ships its own fullscreen title inside
   * player-fullscreen-top-controls, but keeps the whole host visibility:hidden
   * until the "More videos" panel opens. Unhide just the title (not the close
   * button next to it) and drop ours, otherwise both stack in the same corner.
   * Scoped to .fadein because the controls hide by flipping visibility, and a
   * title forced visible would stay burned over the video.
   */
  #player-container-id:fullscreen
    #player-control-overlay.fadein
    .ytwPlayerFullscreenTopControlsFullscreenControlsVideoTitle {
    visibility: visible;
    /*
     * The title box spans the full width, under the top-right control cluster.
     * YouTube gets away with that because the whole host is hidden; once we
     * make the title visible it starts hit-testing and swallows taps on the
     * captions and settings buttons, so it has to stay inert like ours.
     */
    pointer-events: none;
    /*
     * Its flex parent only reserves room for the close button (40px) next to
     * the host's 12px padding, so the title still runs under the controls.
     * Back both out of the 192px inset ours uses to land on the same edge.
     */
    margin-right: 140px;
  }

  #player-container-id:fullscreen
    #player-control-overlay:has(player-fullscreen-top-controls)
    #_nou_fullscreen_title {
    display: none;
  }

  /*
   * Fullscreen controls button: only in fullscreen, and only while the controls
   * are showing, so it never burns over the video. Left edge keeps it clear of
   * the centered play/seek buttons and the top-right control cluster. Hidden
   * once the panel or the lock overlay takes over.
   *
   * --_nou_cutout_left is published by the native view (NouTubeView.kt): the
   * fullscreen window draws under the display cutout, and in landscape the
   * camera sits on a side edge at the same height as the button.
   */
  #_nou_fs_btn {
    display: none;
  }

  #player-container-id:fullscreen:has(#player-control-overlay.fadein):not(
      :has(#_nou_lock_overlay, #_nou_fs_panel)
    )
    > #_nou_fs_btn {
    display: flex;
    position: fixed;
    top: 50%;
    left: calc(16px + var(--_nou_cutout_left, 0px));
    z-index: 2147483646;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    padding: 0;
    transform: translateY(-50%);
    border: none;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.5);
    color: white;
  }

  /* Same chain as above so it outweighs the left-edge rule. */
  #player-container-id:fullscreen:has(#player-control-overlay.fadein):not(
      :has(#_nou_lock_overlay, #_nou_fs_panel)
    )
    > #_nou_fs_btn.right {
    left: auto;
    right: calc(16px + var(--_nou_cutout_right, 0px));
  }

  #_nou_lock_overlay {
    position: fixed;
    inset: 0;
    z-index: 2147483647;
    touch-action: none;
    -webkit-user-select: none;
    user-select: none;
  }

  #_nou_lock_overlay ._nou_lock_unlock {
    position: absolute;
    top: 50%;
    left: calc(16px + var(--_nou_cutout_left, 0px));
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    padding: 0;
    transform: translateY(-50%);
    border: none;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.6);
    color: white;
    opacity: 0;
    transition: opacity 0.2s;
    pointer-events: none;
  }

  #_nou_lock_overlay.right ._nou_lock_unlock {
    left: auto;
    right: calc(16px + var(--_nou_cutout_right, 0px));
  }

  #_nou_lock_overlay.reveal ._nou_lock_unlock {
    opacity: 1;
    pointer-events: auto;
  }
`,es=he`
  /*
   * The scrim only exists to give taps outside the panel something to land on;
   * the panel's own event blocking still runs, this just keeps the hit area from
   * falling through to YouTube's own layers visually.
   */
  #_nou_fs_scrim {
    position: fixed;
    inset: 0;
    z-index: 2147483646;
  }

  #_nou_fs_panel {
    position: fixed;
    top: 50%;
    left: calc(16px + var(--_nou_cutout_left, 0px));
    z-index: 2147483647;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    gap: 12px;
    max-width: min(420px, calc(100vw - 32px - var(--_nou_cutout_left, 0px) - var(--_nou_cutout_right, 0px)));
    max-height: 80vh;
    overflow-y: auto;
    padding: 14px 16px;
    transform: translateY(-50%);
    border-radius: 14px;
    background: rgba(0, 0, 0, 0.82);
    color: white;
    font-size: 13px;
    touch-action: pan-x pan-y;
    -webkit-user-select: none;
    user-select: none;
  }

  #_nou_fs_panel.right {
    left: auto;
    right: calc(16px + var(--_nou_cutout_right, 0px));
  }

  #_nou_fs_panel ._nou_fs_row {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  #_nou_fs_panel ._nou_fs_label {
    flex: none;
    width: 52px;
    color: rgba(255, 255, 255, 0.7);
  }

  #_nou_fs_panel ._nou_fs_chips {
    display: flex;
    gap: 6px;
    overflow-x: auto;
    scrollbar-width: none;
  }

  #_nou_fs_panel ._nou_fs_chips::-webkit-scrollbar {
    display: none;
  }

  #_nou_fs_panel ._nou_fs_chip {
    flex: none;
    padding: 5px 10px;
    border: none;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.12);
    color: white;
    font-size: 13px;
    line-height: 1;
  }

  #_nou_fs_panel ._nou_fs_chip.active {
    background: #4f46e5;
  }

  #_nou_fs_panel ._nou_fs_value {
    flex: none;
    width: 40px;
    color: rgba(255, 255, 255, 0.7);
    text-align: right;
    font-variant-numeric: tabular-nums;
  }

  #_nou_fs_panel #_nou_fs_lock {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    padding: 8px 10px;
    border: none;
    border-radius: 10px;
    background: rgba(255, 255, 255, 0.12);
    color: white;
    font-size: 14px;
  }

  #_nou_fs_panel #_nou_fs_side {
    display: flex;
    flex: none;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    padding: 0;
    border: none;
    border-radius: 10px;
    background: rgba(255, 255, 255, 0.12);
    color: white;
  }

  /*
   * The WebView's native range widget centres its thumb on its own track
   * metrics, which do not match the 4px track we want, so draw both parts
   * ourselves. --_nou_fill is written by the panel on every input so the filled
   * portion still tracks the value without accent-color.
   */
  #_nou_fs_panel ._nou_fs_slider {
    flex: 1;
    height: 16px;
    margin: 0;
    padding: 0;
    border: none;
    outline: none;
    box-shadow: none;
    background: transparent;
    -webkit-appearance: none;
    appearance: none;
  }

  #_nou_fs_panel ._nou_fs_slider::-webkit-slider-runnable-track {
    height: 4px;
    border-radius: 2px;
    background: linear-gradient(
      to right,
      #4f46e5 var(--_nou_fill, 0%),
      rgba(255, 255, 255, 0.3) var(--_nou_fill, 0%)
    );
  }

  #_nou_fs_panel ._nou_fs_slider::-webkit-slider-thumb {
    width: 14px;
    height: 14px;
    margin-top: -5px;
    border: none;
    border-radius: 50%;
    background: white;
    -webkit-appearance: none;
    appearance: none;
  }
`,ts=he`
  /*
   * Fullscreen controls button, web player. The Android rule keys off
   * #player-container-id and the mobile control overlay, neither of which the
   * desktop site has; here the button is a direct child of whatever element
   * YouTube put into fullscreen, and fullscreen-controls.ts toggles .hidden to
   * follow the player's own autohide.
   */
  #_nou_fs_btn {
    display: none;
  }

  :fullscreen > #_nou_fs_btn {
    position: fixed;
    top: 50%;
    left: 16px;
    z-index: 2147483646;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    padding: 0;
    transform: translateY(-50%);
    border: none;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.5);
    color: white;
    cursor: pointer;
    opacity: 1;
    transition: opacity 0.2s;
  }

  :fullscreen > #_nou_fs_btn:hover {
    background: rgba(0, 0, 0, 0.75);
  }

  :fullscreen > #_nou_fs_btn.right {
    left: auto;
    right: 16px;
  }

  /* Faded rather than display:none so it can still be hovered back into view. */
  :fullscreen > #_nou_fs_btn.hidden {
    opacity: 0;
  }

  /*
   * A mouse cannot swipe a chip strip, and shift+wheel is not something to make
   * the only way to reach the last few chips, so wrap them instead of scrolling
   * on desktop.
   */
  #_nou_fs_panel ._nou_fs_chips {
    flex-wrap: wrap;
    overflow-x: visible;
  }

  /* Once the chips wrap, the label belongs beside the first row of them, not
     centred against the whole block. 23px is a chip's height: 13px of text plus
     its 5px padding. */
  #_nou_fs_panel ._nou_fs_row:has(._nou_fs_chips) {
    align-items: flex-start;
  }

  #_nou_fs_panel ._nou_fs_row:has(._nou_fs_chips) ._nou_fs_label {
    line-height: 23px;
  }

  #_nou_fs_panel ._nou_fs_chip,
  #_nou_fs_panel #_nou_fs_side {
    cursor: pointer;
  }

  #_nou_fs_panel ._nou_fs_chip:hover:not(.active) {
    background: rgba(255, 255, 255, 0.24);
  }

  #_nou_fs_panel #_nou_fs_side:hover {
    background: rgba(255, 255, 255, 0.24);
  }
`,ns=he`
  ytd-page-top-ad-layout-renderer,
  ytd-in-feed-ad-layout-renderer,
  ad-slot-renderer,
  yt-mealbar-promo-renderer,
  ytm-promoted-sparkles-web-renderer,
  .ytd-player-legacy-desktop-watch-ads-renderer {
    display: none !important;
  }

  /* Ads are normally dropped from the feed data, but if one still renders, hide
   * its grid cell too, otherwise the hidden ad leaves a blank tile behind. */
  ytd-rich-item-renderer:has(ytd-ad-slot-renderer),
  ytd-rich-item-renderer:has(ytd-in-feed-ad-layout-renderer),
  ytd-rich-section-renderer:has(ytd-ad-slot-renderer),
  ytd-rich-section-renderer:has(ytd-statement-banner-renderer),
  ytd-item-section-renderer:has(ytd-ad-slot-renderer),
  ytm-rich-item-renderer:has(ad-slot-renderer),
  ytm-item-section-renderer:has(ad-slot-renderer),
  ytd-rich-item-renderer:has(ad-slot-renderer),
  ytd-rich-item-renderer:has(.ytwFeedAdMetadataViewModelHostMetadata) {
    display: none !important;
  }
`,os=he`
  a.app-install-link,
  a.yt-spec-button-shape-next {
    display: none !important;
  }

  #_nou_livechat {
    width: 100%;
    height: 50vh;
    position: fixed;
    bottom: 0;
    display: flex;
    flex-direction: column;
    border-top: 1px solid #e5e5e5;
    background: white;
    z-index: 10;
  }
  #_nou_livechat.right {
    width: 36vw;
    height: 100%;
    top: 0;
    bottom: 0;
    right: 0;
    border-top: none;
    border-left: 1px solid #e5e5e5;
  }

  #_nou_livechat > button {
    position: absolute;
    top: 1.25rem;
    left: 50%;
    transform: translateX(-50%);
  }

  #_nou_livechat_loading {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
  }

  #_nou_livechat iframe {
    position: relative;
    flex: 1;
    border: none;
  }

  ._nou_replay_panel {
    display: flex;
    flex-direction: column;
    min-height: 0;
    height: 100%;
    color: #0f0f0f;
    font-size: 14px;
  }
  ._nou_replay_panel header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-shrink: 0;
    min-height: 56px;
    box-sizing: border-box;
    padding: 6px 12px 6px 16px;
    border-bottom: 1px solid #e5e5e5;
    font-size: 16px;
    font-weight: 600;
  }
  ._nou_replay_panel header ._nou_livechat_close {
    display: grid;
    place-items: center;
    width: 44px;
    height: 44px;
    padding: 0;
    border: none;
    border-radius: 50%;
    background: transparent;
    color: inherit;
    cursor: pointer;
  }
  ._nou_replay_panel header ._nou_livechat_close:is(:hover, :active) {
    background: #f2f2f2;
  }
  ._nou_replay_panel header ._nou_livechat_close:focus-visible {
    outline: 2px solid #606060;
    outline-offset: -4px;
  }
  ._nou_replay_panel header ._nou_livechat_close svg {
    width: 20px;
    height: 20px;
  }
  ._nou_replay_panel p {
    padding: 0.5rem 1rem;
  }
  ._nou_replay_messages {
    flex: 1;
    overflow-y: auto;
    min-height: 0;
    padding: 0.5rem 1rem;
  }
  ._nou_replay_message {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    padding: 6px 0;
    overflow-wrap: anywhere;
  }
  ._nou_replay_message small,
  ._nou_replay_message strong {
    color: #606060;
    margin-right: 6px;
  }
  ._nou_replay_avatar {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    flex-shrink: 0;
  }
  ._nou_replay_emoji {
    width: 20px;
    height: 20px;
    vertical-align: middle;
  }
  ._nou_replay_sticker {
    display: block;
    width: 64px;
    height: 64px;
    object-fit: contain;
    margin-top: 4px;
  }
  ._nou_replay_retry {
    align-self: flex-start;
    margin: 0 1rem 0.5rem;
    padding: 0.5rem 1rem;
  }

  #_nou_livechat_btn {
    padding: 0.75rem 1rem;
    background: #e1002d;
    color: white;
    position: fixed;
    bottom: 1rem;
    right: 1rem;
    border-radius: 18px;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 16px;
  }

  .quick-actions-wrapper.enable-rtl-mirroring {
    display: none !important;
  }

  #_nou_audio_btn {
    display: flex;
    align-items: center;
    background: #34d399;
    padding: 0 4px;
    color: #44403c;
    border-radius: 4px;
    margin-left: 8px;
  }
  #_nou_audio_picker {
    position: absolute;
    top: 1rem;
    left: 1rem;
  }
  #_nou_audio_picker select {
    border: none;
    background: #a7f3d0;
    color: #44403c;
    padding: 2px;
  }

  ._nou_sb_segments {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 1;
  }
  ._nou_sb_segment {
    position: absolute;
    top: 0;
    height: 100%;
    opacity: 0.85;
  }
`,rs=()=>{let e=!!window.NouTubeI;return(window.NouTube?.getSettings?.()?.blockAds!==!1?ns:"")+os+(e?Zi:ts)+es},is=e=>[rs(),$o(),Sn(document.location.host,e)].filter(Boolean).join(`

`);function Ut(){let e=document.querySelector(`#${Oo}`)||document.createElement("style"),t=()=>{let n=window.NouTube?.getUserStyles?.();e.textContent=is(n)};e.id=Oo,e.type="text/css",t(),(document.head||document.documentElement).appendChild(e),window.addEventListener(N,t),window.addEventListener($t,t)}function Bo(){let e=document.createElement("style");e.id="noutube-shorts",e.type="text/css",e.textContent=`
ytm-reel-shelf-renderer,
ytd-reel-shelf-renderer,
.ytGridShelfViewModelHost,
grid-shelf-view-model,
ytd-rich-section-renderer:has(ytd-rich-shelf-renderer[is-shorts]),
ytm-shorts-lockup-view-model,
yt-lockup-view-model:has(a[href^='/shorts']),
ytd-video-renderer:has(a[href^='/shorts']),
ytm-video-with-context-renderer:has(a[href^='/shorts']) {
  display: none !important;
}
`,document.head.appendChild(e)}function Ao(){document.querySelector("style#noutube-shorts")?.remove()}var Do=!1;function qo(){if(Do)return;let e=document.querySelector("tp-yt-paper-dialog");if(!e)return;new MutationObserver(n=>{let o=e.querySelector("ytmusic-mealbar-promo-renderer .dismiss-button");o&&o.click()}).observe(e,{attributes:!0}),Do=!0}var Uo='<svg height="16" viewBox="0 0 24 24" width="16" focusable="false" aria-hidden="true" style="pointer-events: none; display: inherit; width: 100%; height: 100%;"><path d="M21 16h-7v-1h7v1zm0-5H9v1h12v-1zm0-4H3v1h18V7zm-11 8-7-4v8l7-4z"></path></svg>',Vo='<svg height="24" viewBox="0 -960 960 960" width="24" style="transform:scale(1.25)"><path d="m354-287 126-76 126 77-33-144 111-96-146-13-58-136-58 135-146 13 111 97-33 143Zm-61 83.92 49.62-212.54-164.93-142.84 217.23-18.85L480-777.69l85.08 200.38 217.23 18.85-164.93 142.84L667-203.08 480-315.92 293-203.08ZM480-470Z"/></svg>',zo='<svg height="24" viewBox="0 0 24 24" width="24" style="transform:scale(1.1)"><path d="M5 20h14v-2H5v2Zm7-2 5.5-5.5-1.42-1.42L13 14.16V4h-2v10.16L7.92 11.08 6.5 12.5 12 18Z"/></svg>';var ss=e=>{try{let t=new URL(e);return["list","index","start_radio","pp"].forEach(n=>t.searchParams.delete(n)),t.href}catch{return e}},as=({icon:e,label:t})=>_.createHTML(`
    <button class="menu-item-button" style="display: flex; align-items: center; width: 100%; padding: 0 16px; min-height: 48px; background: none; border: none; font-family: inherit; font-size: 14px; text-align: left; cursor: pointer;">
      <c3-icon fill-icon="false" style="margin-right: 16px; flex-shrink: 0; width: 24px; height: 24px;">
        <span class="yt-icon-shape yt-spec-icon-shape" style="width: 24px; height: 24px; display: block;">
          <div style="width: 24px; height: 24px; display: block; fill: currentcolor;">${e}</div>
        </span>
      </c3-icon>
      <span class="yt-core-attributed-string" role="text" style="flex: 1; font-size: 14px;">${t} \u{1F9A6}</span>
    </button>
  `),ls=({icon:e,label:t})=>_.createHTML(`
    <tp-yt-paper-item
      class="style-scope ytd-menu-service-item-renderer"
      role="option"
      style="display: flex; align-items: center; width: 100%; padding: 0 16px; min-height: 40px; cursor: pointer;"
    >
      <yt-icon class="style-scope ytd-menu-service-item-renderer" style="margin-right: 16px; flex-shrink: 0; width: 24px; height: 24px;">
        <span class="yt-icon-shape style-scope yt-icon ytSpecIconShapeHost" style="width: 24px; height: 24px; display: block;">
          <div style="width: 24px; height: 24px; display: block; fill: currentcolor;">${e}</div>
        </span>
      </yt-icon>
      <yt-formatted-string class="style-scope ytd-menu-service-item-renderer" style="flex: 1; font-size: 14px;">${t} \u{1F9A6}</yt-formatted-string>
    </tp-yt-paper-item>
  `),jo=(e,{icon:t,label:n})=>{e.querySelectorAll("[id]").forEach(i=>i.removeAttribute("id"));let o=e.querySelector(".ytListItemViewModelImage, .yt-list-item-view-model__image, c3-icon, yt-icon");if(o){let i=document.createElement("span");i.className=o.className,i.setAttribute("aria-hidden","true"),i.style.display="block",i.style.width="24px",i.style.height="24px",i.style.flexShrink="0",e.tagName.toLowerCase()==="ytm-menu-item"&&(i.style.marginRight="16px"),i.innerHTML=_.createHTML(`
      <span class="yt-icon-shape ytSpecIconShapeHost" style="width: 24px; height: 24px; display: block;">
        <div style="width: 24px; height: 24px; display: block; fill: currentcolor;">${t}</div>
      </span>
    `),o.replaceWith(i)}let r=e.querySelector('.ytListItemViewModelTitle, .yt-list-item-view-model__title, .ytAttributedStringHost, .yt-core-attributed-string[role="text"], yt-formatted-string');return r&&(r.textContent=`${n} \u{1F9A6}`,r.removeAttribute("is-empty")),e},Wo=e=>Array.from(e.children).find(t=>t.tagName.toLowerCase()==="yt-list-item-view-model"&&!t.className.includes("_nou_")),Vt=(e,t,n,o)=>{let r;switch(e.tagName.toLowerCase()){case"yt-list-view-model":{let i=o||Wo(e);if(!i)throw new Error("native list menu item is unavailable");r=jo(i.cloneNode(!0),t);break}case"tp-yt-paper-listbox":r=document.createElement("ytd-menu-service-item-renderer"),r.innerHTML=ls(t);break;default:{let i=Array.from(e.children).find(s=>s.tagName.toLowerCase()==="ytm-menu-item"&&!s.className.includes("_nou_"));r=i?jo(i.cloneNode(!0),t):document.createElement("ytm-menu-item"),i||(r.innerHTML=as(t))}}return r.classList.add(n),r};function Fo(){document.addEventListener("click",async e=>{let n=e.target.closest("ytm-media-item,ytm-compact-video-renderer,ytm-video-card-renderer,ytm-playlist-video-renderer,yt-lockup-metadata-view-model,ytd-video-renderer,ytd-grid-video-renderer");if(n){let o=await q(async()=>{let d=document.querySelectorAll("ytm-menu-service-item-renderer,ytm-menu-navigation-item-renderer,yt-list-view-model,tp-yt-paper-listbox"),u=Array.from(d).find(p=>p.offsetHeight>0);if(u){if(u.tagName.toLowerCase()==="yt-list-view-model"){let p=Wo(u);if(!p)throw"menu items not ready";return{menu:u,listItemTemplate:p.cloneNode(!0)}}return{menu:u}}throw"menu not ready"},{retries:50,delay:100}),{menu:r,listItemTemplate:i}=o,s=n.querySelector("h3")?.textContent?.trim()||n.querySelector("h4")?.textContent?.trim(),a=n.querySelector("a")?.href,l=a&&ss(a),c;if(window.NouTubeI){let d="_nou_queue_";r.querySelectorAll(`.${d}`).forEach(u=>u.remove()),c=Vt(r,{icon:Uo,label:"Add to queue"},d,i),c.onclick=()=>{l&&b("add-queue",{title:s,url:l}),c.remove()},r.prepend(c)}let m="_nou_menu_";r.querySelectorAll(`.${m}`).forEach(d=>d.remove());let f={icon:Vo,label:"Star"};if(c=Vt(r,f,m,i),c.onclick=()=>{l&&b("star",{title:s,url:l})},r.prepend(c),window.NouTubeDownloads!==!1){let d="_nou_download_";r.querySelectorAll(`.${d}`).forEach(y=>y.remove());let u={icon:zo,label:"Download"},p=Vt(r,u,d,i);if(p.onclick=()=>{l&&b("download",{url:l})},r.prepend(p),r.tagName.toLowerCase()=="tp-yt-paper-listbox"){let y=p.querySelector("yt-formatted-string");y?.hasAttribute("is-empty")&&(y.textContent=u.label+" \u{1F9A6}",y.removeAttribute("is-empty"));let g=p.querySelector("yt-icon");g?.hasAttribute("hidden")&&(g.innerHTML=_.createHTML(`
              <span class="yt-icon-shape style-scope yt-icon ytSpecIconShapeHost">
                <div style="width: 100%; height: 100%; display: block; fill: currentcolor;">${u.icon}</div>
              </span>
            `),g.removeAttribute("hidden"))}}if(r.tagName.toLowerCase()=="tp-yt-paper-listbox"){let d=c.querySelector("yt-formatted-string");d?.hasAttribute("is-empty")&&(d.textContent=f.label+" \u{1F9A6}",d.removeAttribute("is-empty"));let u=c.querySelector("yt-icon");u?.hasAttribute("hidden")&&(u.innerHTML=_.createHTML(`
            <span class="yt-icon-shape style-scope yt-icon ytSpecIconShapeHost">
              <div style="width: 100%; height: 100%; display: block; fill: currentcolor;">${f.icon}</div>
            </span>
          `),u.removeAttribute("hidden"))}}})}function Qo(){let e=document.querySelector("meta[name=viewport]");if(e){let t=e.getAttribute("content");if(t){let n=t.split(","),o=!1;["maximum-scale","user-scalable"].forEach(r=>{t.includes(r)&&(n=n.filter(i=>!i.includes(r)),o=!0)}),o&&e.setAttribute("content",n.join(","))}}}var We="_nou_blocked",Yo="_nou_blocklist_css",jt=["ytm-media-item","ytm-compact-video-renderer","ytm-video-with-context-renderer","ytd-video-renderer","ytd-compact-video-renderer","ytd-grid-video-renderer","ytd-rich-item-renderer","yt-lockup-view-model","yt-lockup-metadata-view-model"].join(",");function cs(){if(document.getElementById(Yo))return;let e=document.createElement("style");e.id=Yo,e.textContent=`.${We}{display:none!important;}`,(document.head||document.documentElement).appendChild(e)}function us(){return window.NouTube?.getBlocklist?.()||oe()}function I(e){return e?.textContent?.trim()||""}function ds(e){return[I(e.querySelector("h3")),I(e.querySelector("h4")),I(e.querySelector("#video-title")),I(e.querySelector("a#video-title")),I(e.querySelector(".yt-lockup-metadata-view-model-wiz__title")),I(e.querySelector('.yt-core-attributed-string[role="text"]'))].filter(Boolean)}function ms(e){return[...Array.from(e.querySelectorAll('a[href^="/@"],a[href^="/channel/"],a[href^="/c/"],a[href^="/user/"],a[href*="youtube.com/@"],a[href*="youtube.com/channel/"]')).flatMap(n=>[n.textContent?.trim(),n.getAttribute("href")||"",n.href||""]),I(e.querySelector(".yt-lockup-metadata-view-model-wiz__metadata")),I(e.querySelector("ytm-badge-and-byline-renderer")),I(e.querySelector("ytd-channel-name")),I(e.querySelector("#channel-name"))].filter(Boolean)}function Wt(e){let t=us();return ke(ds(e),t.keywords)||Se(ms(e),t.channels)||at(e.getAttribute("aria-label")||"",t.keywords)}function zt(e=document){cs(),e.querySelectorAll?.(jt).forEach(t=>{t.classList.toggle(We,Wt(t))})}function Ko(){zt(),window.addEventListener(Ot,()=>zt()),new MutationObserver(t=>{for(let n of t)n.addedNodes.forEach(o=>{if(o instanceof Element){o.matches(jt)&&o.classList.toggle(We,Wt(o));let r=o.closest?.(jt);r&&r.classList.toggle(We,Wt(r)),zt(o)}})}).observe(document.documentElement,{childList:!0,subtree:!0})}var W="_nou_dislike_count",Yt="_nou_dislike_loaded",Go="_nou_dislike_css",ps="https://returnyoutubedislikeapi.com/votes?videoId=",Fe=new Map,Ft=new Map,J="",Jo=0,fs=["dislike-button-view-model","#segmented-dislike-button"].join(","),ys=["dislike-button-view-model button","#segmented-dislike-button button"].join(","),hs=[".ytSpecButtonShapeNextButtonTextContent",".ytSpecButtonShapeNextButtonTextContent span",".yt-core-attributed-string",".button-renderer-text"].join(",");function gs(){return!!window.NouTube?.getSettings?.()?.showDislikes}function Xo(){let e=new URL(location.href);return e.hostname==="youtu.be"?e.pathname.split("/").filter(Boolean)[0]||"":e.pathname==="/shorts"?"":e.pathname.startsWith("/shorts/")?e.pathname.split("/")[2]||"":e.searchParams.get("v")||""}function bs(e){try{return new Intl.NumberFormat(void 0,{notation:"compact",maximumFractionDigits:1}).format(e)}catch{return String(e)}}function vs(){if(document.getElementById(Go))return;let e=document.createElement("style");e.id=Go,e.textContent=`
    .${W} {
      display: inline-flex !important;
      align-items: center !important;
      margin-left: 6px !important;
      font: inherit !important;
      font-size: 0.8em !important;
      font-weight: 400 !important;
      white-space: nowrap !important;
    }
  `,(document.head||document.documentElement).appendChild(e)}function Qt(){document.querySelectorAll(`.${W}`).forEach(e=>{let t=e.closest("button");t?.dataset.nouDislikeIconButton==="true"&&(t.classList.add("ytSpecButtonShapeNextIconButton"),t.classList.remove("ytSpecButtonShapeNextIconLeading"),delete t.dataset.nouDislikeIconButton);let n=e.dataset.nouOriginalText;if(n!=null&&e.dataset.nouReused==="true"){e.textContent=n,e.classList.remove(W),delete e.dataset.nouOriginalText,delete e.dataset.nouReused;return}e.remove()}),document.querySelectorAll(`.${Yt}`).forEach(e=>e.classList.remove(Yt))}function ws(e){let t=new Set(e);document.querySelectorAll(`.${W}`).forEach(n=>{let o=n.closest("button");(!o||!t.has(o))&&n.remove()})}function Zo(e){let t=e.getBoundingClientRect();return t.width>0&&t.height>0}function _s(){let e=[...Array.from(document.querySelectorAll(ys)),...Array.from(document.querySelectorAll(['button[aria-label*="Dislike"]','button[aria-label*="dislike"]','[role="button"][aria-label*="Dislike"]','[role="button"][aria-label*="dislike"]'].join(",")))];return[...new Set(e)].filter(t=>{if(!Zo(t))return!1;if(t.closest(fs))return!0;let n=`${t.getAttribute("aria-label")||""} ${t.getAttribute("title")||""}`;return/dislike/i.test(n)?!!t.closest(["ytd-segmented-like-dislike-button-renderer","segmented-like-dislike-button-view-model","ytm-slim-video-action-bar-renderer","ytm-like-button-renderer"].join(",")):!1})}function er(e){vs();let t=_s();ws(t);for(let n of t){let o=n.querySelector(`.${W}`);if(o&&!o.classList.contains("ytSpecButtonShapeNextButtonTextContent")&&(o.remove(),o=null),!o)if(o=Array.from(n.querySelectorAll(hs)).find(Zo)||null,o)o.dataset.nouOriginalText=o.textContent||"",o.dataset.nouReused="true",o.classList.add(W);else{o=document.createElement("div"),o.className=`ytSpecButtonShapeNextButtonTextContent ${W}`;let r=n.querySelector("yt-touch-feedback-shape");n.insertBefore(o,r||null),n.classList.contains("ytSpecButtonShapeNextIconButton")&&(n.dataset.nouDislikeIconButton="true",n.classList.remove("ytSpecButtonShapeNextIconButton"),n.classList.add("ytSpecButtonShapeNextIconLeading"))}o.textContent!==e&&(o.textContent=e),n.classList.add(Yt)}}async function xs(e){let t=Fe.get(e);if(t)return t;let n=Ft.get(e);if(n)return n;let o=fetch(`${ps}${encodeURIComponent(e)}`).then(async r=>{if(!r.ok)throw new Error(`Return YouTube Dislike responded with ${r.status}`);let i=await r.json(),s=typeof i?.dislikes=="number"?bs(i.dislikes):"";return s&&Fe.set(e,s),s}).finally(()=>Ft.delete(e));return Ft.set(e,o),o}async function ge(){if(!gs()){J="",Qt();return}let e=Xo();if(!e){J="",Qt();return}let t=++Jo;e!==J&&(J=e,Qt());try{let n=await xs(e);t===Jo&&e===J&&n&&er(n)}catch(n){console.warn("NouScript dislikes:",n)}}function tr(){ge(),window.addEventListener(N,ge);for(let t of["yt-navigate-finish","state-navigateend","popstate"])window.addEventListener(t,ge),document.addEventListener(t,ge);new MutationObserver(()=>{let t=Xo();if(t&&t===J&&Fe.has(t)){er(Fe.get(t)||"");return}ge()}).observe(document.documentElement,{childList:!0,subtree:!0})}var Qe="_nou_translate_btn",nr="_nou_translate_css",Kt="ytm-comment-renderer, ytm-comment-thread-renderer, ytd-comment-thread-renderer, ytd-comment-view-model",rr="#content-text, .comment-text, .comment-content, .YtmCommentRendererText, yt-attributed-string#content-text, yt-formatted-string#content-text",Ss=".YtmCommentRendererDetails, ytm-comment-actions, #toolbar, #action-menu",ks='<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true"><path d="M12.87 15.07l-2.54-2.51.03-.03c1.74-1.94 2.98-4.17 3.71-6.53H17V4h-7V2H8v2H1v1.99h11.17C11.5 7.92 10.44 9.75 9 11.35 8.07 10.32 7.3 9.19 6.69 8h-2c.73 1.63 1.73 3.17 2.98 4.56l-5.09 5.02L4 19l5-5 3.11 3.11.76-2.04zM18.5 10h-2L12 22h2l1.12-3h4.75L21 22h2l-4.5-12zm-2.62 7l1.62-4.33L19.12 17h-3.24z"/></svg>';function ir(){return!!window.NouTube?.getSettings?.()?.translateComments}function Es(){if(document.getElementById(nr))return;let e=document.createElement("style");e.id=nr,e.textContent=`
    .${Qe} {
      display: inline-flex !important;
      align-items: center !important;
      justify-content: center !important;
      padding: 4px 8px !important;
      margin: 0 !important;
      border: none !important;
      background: transparent !important;
      color: inherit !important;
      opacity: 0.7;
      cursor: pointer;
    }
  `,(document.head||document.documentElement).appendChild(e)}function Ts(e){let t=[],n=()=>{t.length&&!t.at(-1)?.endsWith(`
`)&&t.push(`
`)},o=r=>{if(r.nodeType===Node.TEXT_NODE){let i=r.textContent?.replace(/\s+/g," ").trim();i&&t.push(i," ");return}if(r instanceof Element){if(r.tagName==="BR"){n();return}r.childNodes.forEach(o)}};return o(e),t.join("").replace(/[ \t]+\n/g,`
`).replace(/\n[ \t]+/g,`
`).replace(/[ \t]{2,}/g," ").replace(/\n{3,}/g,`

`).trim()}function Cs(e){e.preventDefault(),e.stopPropagation();let o=e.currentTarget.closest(Kt)?.querySelector(rr);if(!o)return;let r=Ts(o);if(r.length<2||r.length>12e3)return;let i=o.getBoundingClientRect();b("translate-block",{id:`${Date.now()}-${Math.random()}`,text:r,x:i.left,y:i.bottom})}function Ls(){document.querySelectorAll(`.${Qe}`).forEach(e=>e.remove())}function sr(){Es(),document.querySelectorAll(Kt).forEach(e=>{let t=e.querySelector(Ss);if(!t||t.closest(Kt)!==e||t.querySelector(`.${Qe}`)||!e.querySelector(rr))return;let n=document.createElement("button");n.className=Qe,n.type="button",n.setAttribute("aria-label","Translate"),n.innerHTML=_.createHTML(ks),n.addEventListener("click",Cs);let o=t.querySelector(".YtmCommentRendererMenu");t.insertBefore(n,o)})}function or(){ir()?sr():Ls()}function ar(){if(!window.isAndroid)return;or(),window.addEventListener(N,or);let e=!1;new MutationObserver(()=>{e||!ir()||(e=!0,requestAnimationFrame(()=>{e=!1,sr()}))}).observe(document.documentElement,{childList:!0,subtree:!0})}var Ms=["pp","si"];function Gt(e){try{let t=new URL(e);return Ms.forEach(n=>t.searchParams.delete(n)),t.href}catch{return e}}function Ps(){let e=document.activeElement;return e instanceof HTMLInputElement||e instanceof HTMLTextAreaElement?e.selectionStart!==null&&e.selectionEnd!==null&&e.selectionStart!==e.selectionEnd?e.value.slice(e.selectionStart,e.selectionEnd):e.value:window.getSelection()?.toString()??""}function lr(){let e=navigator.clipboard?.writeText;e&&(navigator.clipboard.writeText=async function(t){let n=Gt(t);return e.call(this,n||t)}),document.addEventListener("copy",t=>{let n=t.clipboardData?.getData("text/plain")||Ps(),o=Gt(n);!n||o===n||!t.clipboardData||(t.preventDefault(),t.clipboardData.setData("text/plain",o))},{capture:!0})}function cr(e){try{return new URL(e,location.href).pathname.startsWith("/watch")}catch{return!1}}function Rs(){return!!window.NouTube?.getSettings?.()?.replaceWatchNavigation}function Is(e){if(typeof e=="string"&&e)try{return new URL(e,location.href).href}catch{return location.href}return location.href}function ur(){let e=window;if(e.__noutubeWatchNavInit)return;e.__noutubeWatchNavInit=!0;let t=history.pushState;history.pushState=function(...n){return Rs()&&cr(location.href)&&cr(Is(n[2]))?history.replaceState.apply(this,n):t.apply(this,n)}}var Jt=[{value:"auto",label:"Auto"},{value:"hd2160",label:"2160p (4K)"},{value:"hd1440",label:"1440p"},{value:"hd1080",label:"1080p"},{value:"hd720",label:"720p"},{value:"large",label:"480p"},{value:"medium",label:"360p"},{value:"small",label:"240p"},{value:"tiny",label:"144p"}];function dr(e){let t=Jt.find(n=>n.value===e);return t?t.label:"Auto"}var Ke="_nou_fs_btn",V="_nou_fs_panel",vr="_nou_fs_scrim",ee="_nou_lock_overlay",Xt="_nou_lock_unlock",be="_nou_fs_row",Zt="_nou_fs_chip",mr="_nou_fs_slider",wr="active",pr="reveal",Bs="hidden",As=3e3,_r="nou:brightness",xr="nou:fsControlsSide",Ns="right",Hs=`<svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
  <path
    d="M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z"
  ></path>
</svg>`,$s=`<svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
  <path
    d="M17 9V7a5 5 0 0 0-10 0v2H5v12h14V9h-2zM9 7a3 3 0 0 1 6 0v2H9V7zm8 12H7v-8h10v8z"
  ></path>
</svg>`,Os=`<svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
  <path
    d="M17 9H9V7a3 3 0 0 1 5.83-1l1.9-.62A5 5 0 0 0 7 7v2H5v12h14V9zm0 10H7v-8h10v8z"
  ></path>
</svg>`,Ds=`<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
  <path d="M6.99 11 3 15l3.99 4v-3H14v-2H6.99v-3zM21 9l-3.99-4v3H10v2h7.01v3L21 9z"></path>
</svg>`,qs=`<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
  <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3A4.5 4.5 0 0 0 14 7.97v8.06A4.5 4.5 0 0 0 16.5 12z"></path>
</svg>`,Us=`<svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
  <path
    d="M12 7a5 5 0 1 0 0 10 5 5 0 0 0 0-10zm0 8a3 3 0 1 1 0-6 3 3 0 0 1 0 6zM11 1h2v3h-2V1zm0 19h2v3h-2v-3zM1 11h3v2H1v-2zm19 0h3v2h-3v-2zM4.2 5.6l1.4-1.4 2.1 2.1-1.4 1.4-2.1-2.1zm12.1 12.1 1.4-1.4 2.1 2.1-1.4 1.4-2.1-2.1zM5.6 19.8l-1.4-1.4 2.1-2.1 1.4 1.4-2.1 2.1zM18.4 4.2l1.4 1.4-2.1 2.1-1.4-1.4 2.1-2.1z"
  ></path>
</svg>`,Ge,Sr=()=>w.getItem(xr)==="right"?"right":"left";function Xe(e){e?.classList.toggle(Ns,Sr()==="right")}function Vs(){w.setItem(xr,Sr()==="right"?"left":"right");for(let e of[Ke,V,ee])Xe(document.getElementById(e))}var Ze=()=>document.fullscreenElement||document.webkitFullscreenElement,U=()=>document.getElementById("movie_player"),et=()=>!window.isAndroid,te=()=>window.NouTubeToken||"",zs=()=>typeof window.NouTubeI?.setBrightness=="function",kr=()=>typeof window.NouTubeI?.setVolumeIndex=="function",Er=["touchstart","touchmove","touchend","touchcancel","pointerdown","pointermove","pointerup","mousedown","mouseup","click","dblclick","contextmenu","wheel"];function Tr(e){e.classList.add(pr),clearTimeout(Ge),Ge=setTimeout(()=>e.classList.remove(pr),As)}function fr(e){let t=document.getElementById(ee),n=document.getElementById(V);if(!t&&!n)return;let o=e.target;o instanceof Element&&o.closest(`#${V}, .${Xt}`)||(e.stopImmediatePropagation(),e.cancelable&&e.preventDefault(),(e.type==="touchend"||e.type==="click")&&(n?tn():t&&Tr(t)))}function en(e){for(let t of Er)e.addEventListener(t,n=>n.stopPropagation())}function tt(e){for(let t of Er)e?document.addEventListener(t,fr,{capture:!0,passive:!1}):document.removeEventListener(t,fr,{capture:!0})}function Cr(){clearTimeout(Ge),Ge=void 0,document.getElementById(ee)?.remove(),document.getElementById(V)||tt(!1)}function js(){let e=Ze();if(!e||document.getElementById(ee))return;let t=document.createElement("div");t.id=ee,Xe(t),t.innerHTML=_.createHTML(`
    <button type="button" class="${Xt}" aria-label="Unlock">${Os}</button>
  `);let n=t.querySelector(`.${Xt}`);n.addEventListener("click",()=>Cr()),en(n),e.append(t),tt(!0),Tr(t)}function Lr(){let e=Number(w.getItem(_r));return Number.isFinite(e)&&e>0&&e<=100?e:void 0}function Ws(){let e=Lr();if(e!==void 0)return e;let t=window.NouTubeI?.getBrightness?.(te());return typeof t=="number"&&t>0?Math.round(Qs(t)*100):100}var Mr=.17883277,Pr=.28466892,Rr=.55991073;function Fs(e){let t=e<=.5?(e/.5)**2:Math.exp((e-Rr)/Mr)+Pr;return Math.min(Math.max(t/12,0),1)}function Qs(e){let t=Math.min(Math.max(e,0),1)*12,n=t<=1?.5*Math.sqrt(t):Mr*Math.log(t-Pr)+Rr;return Math.min(Math.max(n,0),1)}function Ir(e){window.NouTubeI?.setBrightness?.(te(),e>=100?-1:Fs(e/100))}function Ys(){let e=Lr();e!==void 0&&Ir(e)}function Ks(){window.NouTubeI?.setBrightness?.(te(),-1)}function Gs(){let e=window.NouTubeI?.getVolumeSteps?.(te());return typeof e=="number"&&e>0?e:0}function Js(){let e=window.NouTubeI?.getVolumeIndex?.(te());return typeof e=="number"&&Number.isFinite(e)?e:0}var Xs=()=>typeof U()?.setVolume=="function";function Zs(){let e=U();if(e?.isMuted?.())return 0;let t=e?.getVolume?.();return typeof t=="number"&&Number.isFinite(t)?Math.round(t):100}function ea(e){let t=U();t?.setVolume?.(e),e>0&&t?.isMuted?.()&&t.unMute?.()}var Br=()=>{let e=document.querySelector("#movie_player video")||document.querySelector("video");return e instanceof HTMLVideoElement?e:null},ta=2,ve,X,yr,Je=1;function na(){let e=Br();if(e){if(X&&yr===e)return X;try{ve||=new AudioContext;let t=ve.createMediaElementSource(e);X=ve.createGain(),t.connect(X).connect(ve.destination),yr=e}catch{return}return X}}function Ar(e){if(Je=e,e===1&&!X)return;let t=na();t&&(t.gain.value=e,ve?.resume?.())}function oa(){if(kr()){let e=Gs();return e?{native:!0,max:e,value:Js()}:void 0}if(et()&&Xs()){let e=Zs();return{native:!1,max:100*ta,value:e<100?e:Math.round(Je*100)}}}function ra(e){ea(Math.min(e,100)),Ar(e<=100?1:e/100)}var hr=(e,t)=>`${Math.round(kr()?e/t*100:e)}%`;function ia(){let e=U()?.getAvailableQualityLevels?.(),t=Array.isArray(e)?e:[];return Jt.filter(n=>n.value==="auto"||t.includes(n.value)).map(n=>n.value)}function sa(){let e=ae(w.getItem("nou:settings"),{});return typeof e.playbackQuality=="string"?e.playbackQuality:"auto"}function Ye(e){if(!e)return;let t=Number(e.min),n=Number(e.max),o=n>t?(Number(e.value)-t)/(n-t):0;e.style.setProperty("--_nou_fill",`${Math.round(o*100)}%`)}var gr=(e,t,n,o)=>`
  <button
    type="button"
    class="${Zt}${o?` ${wr}`:""}"
    data-group="${e}"
    data-value="${t}"
  >
    ${n}
  </button>
`;function aa(e){let t=Br(),n=t?t.playbackRate:U()?.getPlaybackRate?.(),o=typeof n=="number"&&Number.isFinite(n)?n:1,r=sa(),i=Ws(),s=oa(),a=Kn(o).map(d=>gr("rate",String(d),Yn(d),d===o)).join(""),l=ia().map(d=>gr("quality",d,dr(d),d===r)).join("");e.innerHTML=_.createHTML(`
    <div class="${be}">
      ${window.isAndroid?`<button type="button" id="_nou_fs_lock">${$s}<span>Lock</span></button>`:""}
      <button type="button" id="_nou_fs_side" aria-label="Switch side">${Ds}</button>
    </div>
    <div class="${be}">
      <span class="_nou_fs_label">Speed</span>
      <div class="_nou_fs_chips">${a}</div>
    </div>
    <div class="${be}">
      <span class="_nou_fs_label">Quality</span>
      <div class="_nou_fs_chips">${l}</div>
    </div>
    ${s?`<div class="${be}">
          ${qs}
          <input
            class="${mr}"
            id="_nou_fs_volume"
            type="range"
            min="0"
            max="${s.max}"
            step="1"
            value="${s.value}"
          />
          <span class="_nou_fs_value" id="_nou_fs_volume_value"
            >${hr(s.value,s.max)}</span
          >
        </div>`:""}
    ${zs()?`<div class="${be}">
          ${Us}
          <input
            class="${mr}"
            id="_nou_fs_brightness"
            type="range"
            min="1"
            max="100"
            step="1"
            value="${i}"
          />
        </div>`:""}
  `),e.querySelector("#_nou_fs_lock")?.addEventListener("click",()=>{tn(),js()}),e.querySelector("#_nou_fs_side").addEventListener("click",()=>Vs()),e.addEventListener("click",d=>{let u=d.target;if(!(u instanceof Element))return;let p=u.closest(`.${Zt}`);if(!p)return;let{group:y,value:g}=p.dataset;if(y==="rate")U()?.setPlaybackRate?.(Number(g));else if(y==="quality"){let h=U();h?.setPlaybackQualityRange?h.setPlaybackQualityRange(g,g):h?.setPlaybackQuality?.(g)}else return;for(let h of p.parentElement.querySelectorAll(`.${Zt}`))h.classList.toggle(wr,h===p)});let c=e.querySelector("#_nou_fs_volume"),m=e.querySelector("#_nou_fs_volume_value");c?.addEventListener("input",()=>{let d=Number(c.value);s?.native?window.NouTubeI?.setVolumeIndex?.(te(),d):ra(d),Ye(c),m&&(m.textContent=hr(d,s.max))}),Ye(c);let f=e.querySelector("#_nou_fs_brightness");f?.addEventListener("input",()=>{let d=Number(f.value);w.setItem(_r,String(d)),Ir(d),Ye(f)}),Ye(f)}function tn(){document.getElementById(V)?.remove(),document.getElementById(vr)?.remove(),document.getElementById(ee)||tt(!1),Z()}function la(){let e=Ze();if(!e||document.getElementById(V))return;Je>1&&Ar(Je);let t=document.createElement("div");t.id=vr;let n=document.createElement("div");n.id=V,Xe(n),aa(n),en(n),e.append(t,n),tt(!0),Z()}function Z(){if(!et())return;let e=document.getElementById(Ke);if(!e)return;let t=!e.matches(":hover")&&(!!document.getElementById(V)||!!U()?.classList.contains("ytp-autohide"));e.classList.toggle(Bs,t)}function br(){let e=Ze();if(!e||e.querySelector(`:scope > #${Ke}`))return;let t=document.createElement("button");t.id=Ke,t.type="button",t.setAttribute("aria-label","Player controls"),t.innerHTML=_.createHTML(Hs),Xe(t),t.onclick=()=>la(),en(t),et()&&(t.addEventListener("mouseenter",()=>Z()),t.addEventListener("mouseleave",()=>Z())),e.append(t),Z()}function Nr(){let e=new MutationObserver(()=>{br(),Z()}),t=()=>{let n=Ze();n?(br(),Ys(),e.observe(n,{childList:!0,subtree:!0,...et()?{attributes:!0,attributeFilter:["class"]}:{}})):(e.disconnect(),tn(),Cr(),Ks())};document.addEventListener("fullscreenchange",t),document.addEventListener("webkitfullscreenchange",t)}var Hr=()=>{if(location.pathname!=="/watch")return;let t=document.querySelector("#movie_player")?.getPlayerResponse?.()?.videoDetails?.author;t&&document.querySelectorAll("ytm-slim-video-information-renderer .slim-video-information-channel-name .ytAttributedStringHost, ytm-slim-owner-renderer .ytAttributedStringHost").forEach(n=>{if(!/%[0-9a-f]{2}/i.test(n.textContent||""))return;let o=n.querySelectorAll(":scope > span"),r=o.length>1&&o[0].textContent==="@"?o[o.length-1]:n,i=r===n?"@"+t:t;r.textContent!==i&&(r.textContent=i)})};function $r(){location.host==="m.youtube.com"&&(new MutationObserver(Hr).observe(document.documentElement,{childList:!0,subtree:!0}),Hr())}var ca=1,ua=2,da=()=>window.NouTubeToken||"",F=!1,nt=!1,ot=!1,rt=0,we=0,Or=()=>window.NouTubeBackground===!0;function ma(){for(let e of["ytmusic-you-there-renderer","ytm-confirm-dialog-renderer"]){let t=document.querySelector(e);if(!t)continue;let n=t.querySelectorAll("button");if(n.length===1)return n[0].click(),!0}return!1}function nn(){let e=navigator.mediaSession;if(!e?.setActionHandler||e.__nouGuardedHandlers)return;window.__nouMediaSession=e;let t=e.setActionHandler.bind(e);e.setActionHandler=(n,o)=>!o||n!=="play"&&n!=="pause"?t(n,o):t(n,r=>(F=n==="pause",o(r))),e.__nouGuardedHandlers=!0}function Dr(){nn();let e=window.NouTube;if(e&&!e.__nouGuardedPause){let t=e.pause.bind(e),n=e.play.bind(e);e.pause=()=>(F=!0,t()),e.play=()=>(F=!1,n()),e.__nouGuardedPause=!0}document.addEventListener("pause",t=>{let n=document.getElementById("movie_player");!Or()&&t.target instanceof Node&&n?.contains(t.target)&&(F=!0)},!0);for(let t of["play","playing"])document.addEventListener(t,n=>{let o=document.getElementById("movie_player");n.target instanceof Node&&o?.contains(n.target)&&(F=!1,nt=!0)},!0);setInterval(()=>{let t=document.getElementById("movie_player");if(!t?.getPlayerState)return;let n=t.getPlayerState();if(n===ca){F=!1,nt=!0,ot=!1,rt=0,we=0;return}if(!Or()){nt=!1,ot=!1,rt=0,we=0;return}if(n!==ua||!nt||F||ot||(rt++,we>=3))return;if(window.NouTubeI?.canAutoResume?.(da())===!1){rt>=6&&(ot=!0);return}we++;let o=ma();O(`background guard: resuming (attempt ${we}, dialog: ${o})`),t.playVideo?.()},5e3)}try{window.NouTubePreferH264&&wn();let e=window.NouTubeClickbaitThumbnail;e&&e!=="default"&&_n(e),window.isAndroid&&nn(),window.NouTube=Io(),window.NouTube.setMuted=pe,window.NouTube.setNativeMini=It,window.NouTube.navigateWatch=Ue,lr(),ur(),go(),window.electron||bn(),document.documentElement?(Ut(),qt(),b("onload"),qr()):document.addEventListener("DOMContentLoaded",()=>{Ut(),qt(),b("onload"),qr()}),setInterval(()=>window._lact=Date.now(),60*1e3)}catch(e){console.error("NouScript: ",e)}async function qr(){let e=document.querySelector("#movie_player");e&&Oe(e),new MutationObserver(n=>{no(n),qo()}).observe(document.documentElement,{childList:!0,subtree:!0}),Fo(),Ko(),tr(),pa(),Nr(),ar(),$r(),window.isAndroid&&Dr(),Qo()}function pa(){if(!window.isAndroid)return;let e=window;if(e.__noutubeDoubleTapGesturesInit)return;e.__noutubeDoubleTapGesturesInit=!0;let t=0,n=0,o=0,r=!1,i=a=>!!a.closest('input, textarea, select, button, a, video, audio, [contenteditable="true"], [role="button"],#movie_player, .html5-video-player, #player, #player-container-id, ytm-player, ytd-player, #shorts-player'),s=()=>!!(document.fullscreenElement||document.webkitFullscreenElement);document.addEventListener("touchstart",a=>{a.touches.length>1&&(r=!0)},{passive:!0,capture:!0}),document.addEventListener("touchend",a=>{if(r){a.touches.length===0&&(r=!1);return}if(a.changedTouches.length!==1)return;if(s()){t=0;return}let l=a.changedTouches[0],c=Date.now(),m=l.clientX-n,f=l.clientY-o,d=c-t<=300&&m*m+f*f<=2304;if(t=c,n=l.clientX,o=l.clientY,!d)return;t=0;let p=(a.target instanceof Element?a.target:null)||document.elementFromPoint(l.clientX,l.clientY);if(!p||i(p))return;window.NouTube?.getSettings?.()?.doubleTapToToggleHeader&&(a.preventDefault(),b("header-double-tap"))},{passive:!1,capture:!0})}})();
