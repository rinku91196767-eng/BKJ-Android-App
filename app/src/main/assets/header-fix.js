(function(){
  function ensureStyle(){
    if(document.getElementById('bkj-header-touch-fix')) return;
    var style=document.createElement('style');
    style.id='bkj-header-touch-fix';
    style.textContent=`
      .top{position:sticky!important;top:0!important;z-index:2147483000!important;isolation:isolate!important;pointer-events:auto!important}
      .top .nav,.top .brand,.top button,.top nav,.top nav a,#bkjMenuToggle,#bkjAccountTop{pointer-events:auto!important;touch-action:manipulation!important}
      #bkjMenuToggle,#bkjAccountTop{position:relative!important;z-index:2147483001!important}
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
    wrap.classList.toggle('nav-open',open);
    toggle.setAttribute('aria-expanded',open?'true':'false');
    toggle.textContent=open?'×':'☰';
    if(window.matchMedia('(max-width:1050px)').matches){
      menu.style.setProperty('display',open?'grid':'none','important');
      if(open){
        menu.style.setProperty('grid-template-columns','1fr 1fr','important');
        menu.style.setProperty('width','100%','important');
        menu.style.setProperty('order','4','important');
      }
    }else{
      menu.style.removeProperty('display');
      menu.style.removeProperty('grid-template-columns');
      menu.style.removeProperty('width');
      menu.style.removeProperty('order');
    }
  }

  function openAccount(){
    try{
      if(typeof window.bkjOpenAccount==='function'){
        window.bkjOpenAccount();
        return;
      }
    }catch(e){}
    var modal=document.getElementById('account');
    if(modal){
      modal.hidden=false;
      modal.removeAttribute('hidden');
      modal.style.setProperty('display','grid','important');
      document.body.style.overflow='hidden';
    }
  }

  function attachTap(node,handler){
    if(!node||node.dataset.bkjTouchFixed==='1') return;
    node.dataset.bkjTouchFixed='1';
    var last=0;
    function run(e){
      var now=Date.now();
      if(now-last<300) return;
      last=now;
      e.preventDefault();
      e.stopPropagation();
      if(typeof e.stopImmediatePropagation==='function') e.stopImmediatePropagation();
      handler();
    }
    node.addEventListener('touchend',run,true);
    node.addEventListener('click',run,true);
  }

  function install(){
    ensureStyle();
    var toggle=document.getElementById('bkjMenuToggle');
    attachTap(toggle,function(){
      var wrap=document.querySelector('.top .nav')||document.querySelector('.nav');
      setMenu(!(wrap&&wrap.classList.contains('nav-open')));
    });

    var account=document.getElementById('bkjAccountTop');
    attachTap(account,openAccount);

    document.querySelectorAll('#bkjMainNav a').forEach(function(link){
      if(link.dataset.bkjNavTouchFixed==='1') return;
      link.dataset.bkjNavTouchFixed='1';
      link.style.pointerEvents='auto';
      link.addEventListener('click',function(){setMenu(false);},false);
    });
  }

  install();
  setTimeout(install,500);
  setTimeout(install,1500);
})();
