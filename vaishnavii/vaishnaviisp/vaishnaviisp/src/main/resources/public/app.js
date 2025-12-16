const api = {
  async health(){
    const r = await fetch('/api/health');
    return r.json();
  },
  async customers(){
    const r = await fetch('/api/customers');
    return r.json();
  },
  async createCustomer(data){
    const r = await fetch('/api/customers',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams(data)});
    return r.json();
  },
  async recordUsage(data){
    const r = await fetch('/api/usage',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams(data)});
    return r.json();
  },
  async getUsage(customerId){
    const r = await fetch(`/api/usage/${encodeURIComponent(customerId)}`);
    return r.json();
  },
  async createTicket(data){
    const r = await fetch('/api/tickets',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams(data)});
    return r.json();
  },
  async ticketsByCustomer(customerId){
    const r = await fetch(`/api/tickets/customer/${encodeURIComponent(customerId)}`);
    return r.json();
  },
  async connectDevice(data){
    const r = await fetch('/api/hotspot/connect',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams(data)});
    return r.json();
  },
  async activeDevices(){
    const r = await fetch('/api/hotspot/active');
    return r.json();
  },
  async disconnectDevice(id, finalDataUsedGB){
    const r = await fetch(`/api/hotspot/${encodeURIComponent(id)}/disconnect`,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({finalDataUsedGB})});
    return r.json();
  },
  async billingSummary(customerId){
    const r = await fetch(`/api/billing/customer/${encodeURIComponent(customerId)}/summary`);
    return r.json();
  },
  async runBilling(){
    const r = await fetch('/api/billing/run',{method:'POST'});
    return r.json();
  }
};

function setHealth(ok){
  const dot=document.getElementById('health-dot');
  const text=document.getElementById('health-text');
  if(ok){dot.classList.remove('bg-secondary');dot.classList.add('bg-success');text.textContent='Healthy';}
  else{dot.classList.remove('bg-success');dot.classList.add('bg-danger');text.textContent='Down';}
}

async function loadHealth(){
  try{const h=await api.health();setHealth(h.status==='ok');}
  catch{setHealth(false)}
}

function qs(sel,root=document){return root.querySelector(sel)}
function qsa(sel,root=document){return [...root.querySelectorAll(sel)]}

// Customers UI
async function loadCustomers(){
  const tbody=qs('#table-customers tbody');
  tbody.innerHTML='<tr><td colspan="4" class="text-body-secondary">Loading…</td></tr>';
  try{
    const it=await api.customers();
    tbody.innerHTML='';
    for(const c of it){
      const tr=document.createElement('tr');
      tr.innerHTML=`<td>${escapeHtml(c.name)}</td><td>${escapeHtml(c.email)}</td><td><code>${c.id}</code></td>
        <td class="text-end d-flex justify-content-end gap-2">
          <button class="btn btn-sm btn-outline-secondary" data-action="copy" data-id="${c.id}"><i class="fa-regular fa-copy me-1"></i>Copy ID</button>
          <button class="btn btn-sm btn-primary" data-action="invoice" data-id="${c.id}"><i class="fa-solid fa-file-pdf me-1"></i>Invoice</button>
        </td>`;
      tbody.appendChild(tr);
    }
    if(!it.length){
      tbody.innerHTML='<tr><td colspan="4" class="text-body-secondary">No customers yet</td></tr>';
    }
  }catch(e){
    tbody.innerHTML=`<tr><td colspan="4" class="text-danger">Failed to load: ${e}</td></tr>`
  }
}

function escapeHtml(s){return String(s).replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#39;"}[m]))}

// Usage UI
async function onRecordUsage(ev){
  ev.preventDefault();
  const fd=new FormData(ev.target);
  const data=Object.fromEntries(fd.entries());
  const btn=ev.submitter;btn.disabled=true;btn.innerHTML='Saving…';
  try{await api.recordUsage(data);ev.target.reset();toast('Usage recorded');}
  catch{toast('Failed to record usage','danger')}finally{btn.disabled=false;btn.innerHTML='Submit'}
}

async function onLoadUsage(ev){
  ev.preventDefault();
  const id=new FormData(ev.target).get('customerId');
  const ul=qs('#list-usage');ul.innerHTML='Loading…';
  try{
    const list=await api.getUsage(id);
    ul.innerHTML='';
    if(!list.length){ul.innerHTML='<li class="list-group-item">No usage</li>';return}
    for(const u of list){
      const li=document.createElement('li');
      li.className='list-group-item';
      li.textContent=`${u.gigabytes} GB (id: ${u.id})`;
      ul.appendChild(li);
    }
  }catch{ul.innerHTML='<li class="list-group-item text-danger">Failed to load</li>'}
}

// Tickets UI
async function onCreateTicket(ev){
  ev.preventDefault();
  const data=Object.fromEntries(new FormData(ev.target).entries());
  const btn=ev.submitter;btn.disabled=true;btn.innerHTML='Creating…';
  try{await api.createTicket(data);ev.target.reset();toast('Ticket created');}
  catch{toast('Failed to create ticket','danger')}finally{btn.disabled=false;btn.innerHTML='Create'}
}

async function onLoadTickets(ev){
  ev.preventDefault();
  const id=new FormData(ev.target).get('customerId');
  const ul=qs('#list-tickets');ul.innerHTML='Loading…';
  try{const list=await api.ticketsByCustomer(id);ul.innerHTML='';
    if(!list.length){ul.innerHTML='<li class="list-group-item">No tickets</li>';return}
    for(const t of list){
      const li=document.createElement('li');li.className='list-group-item d-flex justify-content-between align-items-center';
      li.innerHTML=`<span><span class="badge rounded-pill ${t.status==='OPEN'?'text-bg-primary':'text-bg-success'} me-2">${t.status}</span> ${escapeHtml(t.description)}</span><code>${t.id}</code>`;
      ul.appendChild(li);
    }
  }catch{ul.innerHTML='<li class="list-group-item text-danger">Failed to load</li>'}
}

// Hotspot UI
async function loadActiveDevices(){
  const tbody=qs('#table-active-devices tbody');
  tbody.innerHTML='<tr><td colspan="6" class="text-body-secondary">Loading…</td></tr>';
  try{
    const list=await api.activeDevices();
    tbody.innerHTML='';
    for(const d of list){
      const tr=document.createElement('tr');
      tr.innerHTML=`<td>${escapeHtml(d.deviceName)}</td><td><code>${escapeHtml(d.macAddress)}</code></td>
        <td>${Number(d.dataUsedGB).toFixed(2)}</td><td>${d.connectedMinutes}</td><td><code>${d.id}</code></td>
        <td class="text-end d-flex justify-content-end gap-2">
          <input type="number" min="0" step="0.01" class="form-control form-control-sm w-auto" value="${Number(d.dataUsedGB).toFixed(2)}" placeholder="Final GB" />
          <button class="btn btn-sm btn-outline-danger" data-action="disconnect" data-id="${d.id}"><i class="fa-solid fa-link-slash me-1"></i>Disconnect</button>
        </td>`;
      tbody.appendChild(tr);
    }
    if(!list.length){
      tbody.innerHTML='<tr><td colspan="6" class="text-body-secondary">No active devices</td></tr>';
    }
  }catch(e){
    tbody.innerHTML=`<tr><td colspan="6" class="text-danger">Failed to load: ${e}</td></tr>`
  }
}

async function onConnectDevice(ev){
  ev.preventDefault();
  const data=Object.fromEntries(new FormData(ev.target).entries());
  const btn=ev.submitter;btn.disabled=true;btn.innerHTML='Connecting…';
  try{await api.connectDevice(data);ev.target.reset();toast('Device connected');await loadActiveDevices();}
  catch{toast('Failed to connect','danger')}finally{btn.disabled=false;btn.innerHTML='Connect'}
}

// Billing UI
async function onBillingSummary(ev){
  ev.preventDefault();
  const id=new FormData(ev.target).get('customerId');
  const pre=qs('#billing-summary');pre.textContent='Loading…';
  try{const res=await api.billingSummary(id);pre.textContent=typeof res.summary==='string'?res.summary:JSON.stringify(res,null,2)}
  catch{pre.textContent='Failed to load summary'}
}

async function onRunBilling(){
  const el=qs('#billing-run-result');el.textContent='Running…';
  try{const r=await api.runBilling();el.textContent=r.message||'Done'}catch{el.textContent='Failed'}
}

function toast(message, type='success'){
  const div=document.createElement('div');
  div.className=`toast align-items-center text-bg-${type} border-0 position-fixed bottom-0 end-0 m-3`;
  div.setAttribute('role','alert');div.innerHTML=`<div class="d-flex"><div class="toast-body">${escapeHtml(message)}</div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>`;
  document.body.appendChild(div);const t=new bootstrap.Toast(div,{delay:2000});t.show();div.addEventListener('hidden.bs.toast',()=>div.remove());
}

// Event wiring
window.addEventListener('DOMContentLoaded',()=>{
  loadHealth();
  loadCustomers();
  loadActiveDevices();

  qs('#btn-refresh-customers').addEventListener('click',loadCustomers);
  qs('#form-create-customer').addEventListener('submit',async ev=>{ev.preventDefault();
    const data=Object.fromEntries(new FormData(ev.target).entries());
    const btn=ev.submitter;btn.disabled=true;btn.innerHTML='Creating…';
    try{await api.createCustomer(data);ev.target.reset();toast('Customer created');await loadCustomers();}
    catch{toast('Failed to create','danger')}finally{btn.disabled=false;btn.innerHTML='Create'}
  });

  qs('#form-record-usage').addEventListener('submit',onRecordUsage);
  qs('#form-load-usage').addEventListener('submit',onLoadUsage);
  qs('#form-create-ticket').addEventListener('submit',onCreateTicket);
  qs('#form-load-tickets').addEventListener('submit',onLoadTickets);
  qs('#form-connect-device').addEventListener('submit',onConnectDevice);
  qs('#btn-refresh-active').addEventListener('click',loadActiveDevices);
  qs('#form-billing-summary').addEventListener('submit',onBillingSummary);
  qs('#btn-run-billing').addEventListener('click',onRunBilling);
  qs('#btn-download-invoice').addEventListener('click',()=>{
    const id=qs('#form-billing-summary input[name="customerId"]').value.trim();
    if(!id){toast('Enter a customer ID','danger');return}
    window.open(`/api/billing/customer/${encodeURIComponent(id)}/invoice`,'_blank');
  });

  // Delegate actions (copy ID, disconnect)
  document.body.addEventListener('click',async e=>{
    const btn=e.target.closest('button'); if(!btn) return;
    const action=btn.getAttribute('data-action');
    if(action==='copy'){
      const id=btn.getAttribute('data-id');
      await navigator.clipboard.writeText(id);toast('Copied ID');
    } else if(action==='invoice'){
      const id=btn.getAttribute('data-id');
      window.open(`/api/billing/customer/${encodeURIComponent(id)}/invoice`,'_blank');
    } else if(action==='disconnect'){
      const id=btn.getAttribute('data-id');
      const gbInput=btn.parentElement.querySelector('input[type="number"]');
      const finalGB=gbInput?gbInput.value:0;
      btn.disabled=true;btn.innerHTML='Disconnecting…';
      try{await api.disconnectDevice(id, finalGB);toast('Disconnected');await loadActiveDevices();}
      catch{toast('Failed to disconnect','danger')}finally{btn.disabled=false;btn.innerHTML='<i class="fa-solid fa-link-slash me-1"></i>Disconnect'}
    }
  });
});
