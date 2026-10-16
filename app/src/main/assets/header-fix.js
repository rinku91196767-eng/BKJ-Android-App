(function(){
  function ensureStyle(){
    if(document.getElementById('bkj-header-touch-fix-v5')) return;
    var style=document.createElement('style');
    style.id='bkj-header-touch-fix-v5';
    style.textContent=`
      .top{position:sticky!important;top:0!important;z-index:2147483000!important;isolation:isolate!important;pointer-events:auto!important}
      .top .nav,.top .brand,.top button,.top nav,.top nav a,#bkjMenuToggle,#bkjAccountTop{pointer-events:auto!important;touch-action:manipulation!important}
      #bkjMenuToggle,#bkjAccountTop{position:relative!important;z-index:2147483002!important;cursor:pointer!important;-webkit-tap-highlight-color:rgba(255,107,53,.15)!important}
      .hero:before,.hero:after,.hero-bg{pointer-events:none!important}
      .bkj-account-modal:not([hidden]){pointer-events:auto!important;z-index:2147483600!important}
      @media(max-width:1050px){
        .top .nav{position:relative!important}
        #bkjMainNav{position:relative!important;z-index:2147482999!important}
        .nav.nav-open #bkjMainNav{display:grid!important;grid-template-columns:1fr 1fr!important;width:100%!important;order:4!important;gap:6px!important}
      }
    `;
    document.head.appendChild(style);
  }

  function setMenu(open){
    var wrap=document.querySelector('.top .nav')||document.querySelector('.nav');
    var menu=document.getElementById('bkjMainNav');
    var toggle=document.getElementById('bkjMenuToggle');
    if(!wrap||!menu||!toggle) return;
    wrap.classList.toggle('nav-open',!!open);
    toggle.setAttribute('aria-expanded',open?'true':'false');
    toggle.textContent=open?'×':'☰';
    if(window.matchMedia('(max-width:1050px)').matches){
      menu.style.setProperty('display',open?'grid':'none','important');
      if(open){
        menu.style.setProperty('grid-template-columns','1fr 1fr','important');
        menu.style.setProperty('width','100%','important');
        menu.style.setProperty('order','4','important');
        menu.style.setProperty('gap','6px','important');
      }
    }
  }

  function openAccount(){
    try{
      if(typeof window.bkjOpenAccount==='function'){
        window.bkjOpenAccount();
        return;
      }
    }catch(e){}
    var modal=document.querySelector('.bkj-account-modal') || document.getElementById('account');
    if(modal){
      modal.hidden=false;
      modal.removeAttribute('hidden');
      modal.style.setProperty('display','grid','important');
      document.body.style.overflow='hidden';
    }
  }

  function stopEvent(e){
    if(!e) return;
    try{e.preventDefault();}catch(_){}
    try{e.stopPropagation();}catch(_){}
    try{if(typeof e.stopImmediatePropagation==='function') e.stopImmediatePropagation();}catch(_){}
  }

  function makeFreshButton(id,handler){
    var old=document.getElementById(id);
    if(!old) return null;
    if(old.dataset.bkjV5Fixed==='1') return old;

    var node=old.cloneNode(true);
    node.dataset.bkjV5Fixed='1';
    node.removeAttribute('onclick');
    node.style.pointerEvents='auto';
    node.style.touchAction='manipulation';
    old.parentNode.replaceChild(node,old);

    var lastAction=0;
    function run(e){
      stopEvent(e);
      var now=Date.now();
      if(now-lastAction<450) return false;
      lastAction=now;
      handler();
      return false;
    }

    node.addEventListener('pointerup',run,true);
    node.addEventListener('touchend',run,true);
    node.addEventListener('click',run,true);
    node.onclick=run;
    return node;
  }

  function install(){
    ensureStyle();

    makeFreshButton('bkjMenuToggle',function(){
      var wrap=document.querySelector('.top .nav')||document.querySelector('.nav');
      setMenu(!(wrap&&wrap.classList.contains('nav-open')));
    });

    makeFreshButton('bkjAccountTop',openAccount);

    document.querySelectorAll('#bkjMainNav a').forEach(function(link){
      if(link.dataset.bkjV5NavFixed==='1') return;
      link.dataset.bkjV5NavFixed='1';
      link.style.pointerEvents='auto';
      link.style.touchAction='manipulation';
      link.addEventListener('click',function(){setMenu(false);},true);
    });
  }

  install();
  setTimeout(install,250);
  setTimeout(install,900);
  setTimeout(install,2000);

  try{
    var observer=new MutationObserver(function(){install();});
    observer.observe(document.documentElement,{childList:true,subtree:true});
    setTimeout(function(){try{observer.disconnect();}catch(_){}},10000);
  }catch(e){}
})();
