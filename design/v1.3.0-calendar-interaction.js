(() => {
  'use strict';
  const HOUR = 52, STEP = 30, LONG_PRESS = 450;
  const seed = [
    {id:'d1-1',day:1,order:1,name:'西湖天地',start:570,duration:60,travel:15,mode:'步行'},
    {id:'d1-2',day:1,order:2,name:'知味观',start:690,duration:60,travel:25,mode:'打车'},
    {id:'d1-3',day:1,order:3,name:'龙井村',start:810,duration:60},
    {id:'d2-1',day:2,order:1,name:'灵隐寺',start:540,duration:120,travel:20,mode:'步行'},
    {id:'d2-2',day:2,order:2,name:'素食午餐',start:720,duration:60,travel:35,mode:'打车'},
    {id:'d2-3',day:2,order:3,name:'西溪湿地',start:840,duration:120},
    {id:'d3-1',day:3,order:1,name:'河坊街',start:600,duration:120},
    {id:'d3-2',day:3,order:2,name:'杭州东站',start:840,duration:60}
  ];
  const colors={1:['#E1ECF6','#2766AA'],2:['#E5EEE3','#4F7853'],3:['#F3EDDF','#8B6A37']};
  const edgeSeed=[
    {id:'edge-1',day:1,order:1,name:'西湖天地',start:570,duration:60},
    {id:'edge-2',day:1,order:2,name:'游船',start:600,duration:60},
    {id:'edge-3',day:1,order:3,name:'知味观',start:750,duration:null},
    {id:'edge-4',day:1,order:4,name:'龙井村',start:870,duration:60},
    {id:'edge-5',day:1,order:5,name:'湖边散步',start:null,duration:90},
    {id:'edge-6',day:1,order:6,name:'湖滨夜景',start:null,duration:null}
  ];
  let edgeItems=edgeSeed.map(x=>({...x}));
  let items=seed.map(x=>({...x})), scene='list', selected=null, gesture=null, undo=null;
  let host=null, scroller=null, grid=null, bubble=null, panel=null, ghost=null, pool=null, poolHeight=0;
  let pending=null, lastPointer=null, edgeFrame=null, edgeSince=0, edgeDirection=0, edgeTime=0, revision=0, status='';
  let listDay=1;
  let listWhole=false;
  const scrollPositions={};
  const phone=document.querySelector('#phone');
  const fmt=n=>n==null?'未设':`${String(Math.floor(n/60)).padStart(2,'0')}:${String(n%60).padStart(2,'0')}`;
  const px=n=>n*HOUR/60;
  const allItems=()=>scene==='edge'?edgeItems:items;
  const entry=id=>items.find(x=>x.id===id)||edgeItems.find(x=>x.id===id);
  const displayDuration=item=>item.duration??60;
  const complete=item=>item.start!=null&&item.duration!=null&&item.duration>0;
  const timing=item=>item.duration==null?`${fmt(item.start)} 到达 · 停留待设`:`${fmt(item.start)} – ${fmt(item.start+item.duration)}`;
  const stay=item=>item.duration==null?'停留未设':`停留 ${item.duration} 分钟`;
  const isWhole=()=>scene==='whole'||scene==='tail';
  const dayForScene=()=>scene==='day2'?2:scene==='day3'?3:scene==='half'?listDay:1;
  const dayItems=d=>allItems().filter(x=>x.day===d).sort((a,b)=>a.order-b.order);
  const visibleItems=()=>isWhole()?items.filter(x=>scene==='tail'?x.day===3:x.day<3):dayItems(dayForScene());
  const draftItem=id=>gesture?.id===id?{...entry(id),...gesture.draft,...(gesture.mode==='place'&&!gesture.dropValid?{start:null}:{})}:entry(id);
  const clamp=(n,a,b)=>Math.max(a,Math.min(b,n));
  function eventBox(item){
    if(!isWhole()){
      const peers=dayItems(item.day).map(x=>draftItem(x.id)).filter(x=>x.start!=null).sort((a,b)=>a.start-b.start||a.order-b.order);
      const group=[];let end=-1;
      for(const p of peers){
        if(p.start>=end&&group.some(x=>x.id===item.id))break;
        if(p.start>=end)group.length=0;
        group.push(p);end=Math.max(end,p.start+displayDuration(p));
      }
      if(group.length>1&&group.some(x=>x.id===item.id)){
        const lane=group.findIndex(x=>x.id===item.id),width=(238-4*(group.length-1))/group.length;
        return {left:(40+lane*(width+4))/282*100,width:width/282*100};
      }
      return {left:40/282*100,width:238/282*100};
    }
    return {left:((scene==='tail'||item.day===1)?40:163)/282*100,width:115/282*100};
  }
  function overlaps(item){return complete(item)&&dayItems(item.day).map(x=>draftItem(x.id)).some(other=>complete(other)&&other.id!==item.id&&item.start<other.start+other.duration&&other.start<item.start+item.duration)}
  function warning(item){
    if(!complete(item))return '';
    if(overlaps(item))return '与其他日程时间重叠';
    const day=dayItems(item.day), i=day.findIndex(x=>x.id===item.id), before=day[i-1], after=day[i+1];
    if((before&&complete(before)&&before.start+before.duration+(before.travel||0)>item.start)||(after&&after.start!=null&&item.start+item.duration+(item.travel||0)>after.start))return '相邻交通可能来不及，未移动其他地点';
    return '';
  }
  function announce(text){status=text;const el=document.querySelector('#drag-status');if(el)el.textContent=text;}
  function makePanel(){
    panel=document.createElement('section');panel.className='drag-inspector';panel.id='drag-inspector';
    panel.innerHTML='<h3>拖动联动 · 可操作样例</h3><p id="drag-instruction"></p><output id="timing-readout" aria-live="polite"></output><p id="drag-warning" class="drag-warning"></p><p id="drag-status" class="drag-status" role="status"></p><div class="demo-actions"><button id="undo-timing" disabled>撤销</button><button id="cancel-timing" disabled>取消拖动</button><button id="reset-timing">重置样例</button></div><ul class="linked-list" id="linked-list" aria-label="同步后的行程清单"></ul><p style="margin-top:8px">本地演示数据，刷新后重置。</p>';
    document.querySelector('nav').before(panel);
    panel.querySelector('#undo-timing').onclick=undoChange;
    panel.querySelector('#cancel-timing').onclick=()=>cancel('已取消，时间未改变');
    panel.querySelector('#reset-timing').onclick=()=>{cancel();if(scene==='edge')edgeItems=edgeSeed.map(x=>({...x}));else items=seed.map(x=>({...x}));undo=null;revision++;selected=scene==='edge'?'edge-3':null;announce('样例已重置');render();};
  }
  function inspect(){
    if(!panel)return;
    panel.querySelector('#drag-instruction').textContent=isWhole()?'全程只读。点地点进入单日，再长按移动或拖上下沿。':`${scene==='edge'?'虚线表示时间待设；暖色实线表示重叠。长按待安排卡片拖入时间轴，或点卡片设到达。':''}长按日程移动，上下沿任意位置可拖。键盘：Space拾取，↑↓调整，Enter保存，Esc取消。`;
    const item=selected&&draftItem(selected);
    panel.querySelector('#timing-readout').textContent=item?`${item.name} · 第 ${item.day} 天\n到达 ${fmt(item.start)} → 结束 ${complete(item)?fmt(item.start+item.duration):'未设'}\n${stay(item)}${gesture?' · 拖动预览':' · 当前值'}${item.start==null?'\n未放入时间轴；长按拖入或点卡片设时间。':item.duration==null?'\n按 1 小时高度展示；实际拖动后保存时长。':''}`:'请选择一个日程，查看联动后的到达时间与停留时长。';
    panel.querySelector('#drag-warning').textContent=item?warning(item):'';
    panel.querySelector('#drag-status').textContent=status;
    panel.querySelector('#undo-timing').disabled=!undo||!!gesture||isWhole()||!allItems().some(x=>x.id===undo.id);
    panel.querySelector('#cancel-timing').disabled=!gesture;
    panel.querySelector('#reset-timing').disabled=!!gesture;
    const list=panel.querySelector('#linked-list');list.replaceChildren();
    for(const item of visibleItems()){
      const li=document.createElement('li'),label=document.createElement('strong'),value=document.createElement('span');
      label.textContent=`${isWhole()?`第${item.day}天 · `:''}${item.order} ${item.name}`;
      value.textContent=`到达 ${fmt(item.start)} · ${stay(item)}`;
      li.append(label,value);list.append(li);
    }
  }
  function positionCard(card,item){
    const box=eventBox(item);card.style.left=box.left+'%';card.style.width=box.width+'%';
    card.style.top=(12+px(item.start))+'px';card.style.height=px(displayDuration(item))+'px';
    card.classList.toggle('short',displayDuration(item)<60);card.classList.toggle('selected',selected===item.id&&!isWhole());
    card.classList.toggle('time-unset',item.duration==null);
    card.classList.toggle('dragging',gesture?.id===item.id);card.classList.toggle('conflicting',overlaps(item));
    card.setAttribute('aria-label',`${item.name}，${timing(item)}${item.duration==null?'，按1小时高度展示':`，${stay(item)}`}${isWhole()?'，查看单日':'，长按移动'}`);
    card.querySelector('.event-time').textContent=timing(item);
  }
  function renderRoutes(){
    grid.querySelectorAll('.calendar-route,.overlap-note').forEach(x=>x.remove());
    for(const item0 of visibleItems()){
      const item=draftItem(item0.id);if(!item.travel||!complete(item))continue;
      const route=document.createElement('div');route.className='calendar-route';
      const box=eventBox(item);route.style.left=box.left+'%';route.style.width=box.width+'%';
      route.style.top=(12+px(item.start+item.duration))+'px';route.style.height=px(item.travel)+'px';
      route.textContent=`${item.mode}约 ${item.travel} 分${isWhole()?'':'钟'}`;grid.append(route);
    }
    if(scene==='edge'){
      const peers=visibleItems().map(x=>draftItem(x.id)).filter(complete).sort((a,b)=>a.start-b.start),groups=[];
      for(const item of peers){
        const group=groups.at(-1);
        if(group&&item.start<group.end){group.items.push(item);group.end=Math.max(group.end,item.start+item.duration);}
        else groups.push({items:[item],end:item.start+item.duration});
      }
      for(const group of groups.filter(x=>x.items.length>1)){
        const start=Math.max(...group.items.map(x=>x.start)),end=Math.min(...group.items.map(x=>x.start+x.duration));
        const note=document.createElement('span');note.className='overlap-note';note.textContent=`${group.items.length} 项时间重叠${end>start?` · ${fmt(start)}–${fmt(end)}`:''}`;
        note.style.top=(21+px(group.end))+'px';grid.append(note);
      }
    }
  }
  function paint(){
    if(gesture?.mode==='place'&&!grid.querySelector(`[data-item-id="${gesture.id}"]`))grid.append(createCard(draftItem(gesture.id)));
    for(const card of grid.querySelectorAll('.calendar-event'))positionCard(card,draftItem(card.dataset.itemId));
    if(gesture?.mode==='place')grid.querySelector(`[data-item-id="${gesture.id}"]`).hidden=!gesture.dropValid;
    pool?.querySelectorAll('.pending-card').forEach(x=>x.classList.toggle('picked-up',gesture?.id===x.dataset.itemId));
    if(gesture){
      const item=draftItem(gesture.id);bubble.hidden=false;
      bubble.textContent=gesture.mode==='place'&&!gesture.dropValid?'拖到时间轴内，松手设置到达':`${fmt(item.start)} — ${fmt(item.start+item.duration)} · 停留 ${item.duration} 分钟`;
      bubble.style.top=(poolHeight+clamp(12+px(item.start)-34-scroller.scrollTop,2,scroller.clientHeight-32))+'px';
    }else bubble.hidden=true;
    renderRoutes();inspect();
  }
  function select(id){selected=id;paint();}
  function renderPool(){
    if(scene!=='edge')return;
    if(!pool){pool=document.createElement('section');pool.className='pending-area';pool.setAttribute('aria-label','待安排地点');host.prepend(pool);}
    const untimed=visibleItems().filter(x=>x.start==null),oldHeight=poolHeight;
    pool.replaceChildren();
    const heading=document.createElement('div');heading.className='pending-heading';heading.innerHTML=`<strong>待安排 · ${untimed.length}</strong><span>${untimed.length?'长按拖入时间轴':'已全部安排'}</span>`;pool.append(heading);
    for(const item of untimed){
      const card=document.createElement('button');card.className='pending-card';card.dataset.itemId=item.id;
      card.setAttribute('aria-label',`${item.name}，到达未设，${stay(item)}，长按拖入时间轴或点击设置`);
      const title=document.createElement('strong'),caption=document.createElement('span'),arrow=document.createElement('span');
      title.textContent=`${item.order}  ${item.name}`;caption.textContent=`到达待设 · ${item.duration==null?'停留待设':`停留 ${item.duration} 分钟`}`;arrow.className='pending-arrow';arrow.textContent='›';card.append(title,caption,arrow);pool.append(card);
    }
    poolHeight=28+untimed.length*58+(untimed.length?4:0);pool.style.height=poolHeight+'px';
    scroller.style.top=poolHeight+'px';if(oldHeight)scroller.scrollTop+=poolHeight-oldHeight;
  }
  function showArrivalEditor(id){
    selected=id;paint();const item=entry(id),modal=document.createElement('div');modal.className='demo-detail arrival-editor';modal.setAttribute('role','dialog');modal.setAttribute('aria-label',`设置${item.name}到达时间`);
    const content=document.createElement('section'),h=document.createElement('h3');h.textContent=`${item.order} ${item.name}`;
    const hint=document.createElement('p');hint.textContent='设好到达时间后，地点会进入当天时间轴。';
    const label=document.createElement('label');label.textContent='到达时间';const input=document.createElement('input');input.type='time';input.step='1800';input.required=true;input.setAttribute('aria-label','到达时间');label.append(input);
    const note=document.createElement('p');note.textContent=item.duration==null?'停留待设 · 保存后按 1 小时高度展示，可拖边缘设置。':`停留 ${item.duration} 分钟 · 保持已有时长。`;
    const save=document.createElement('button');save.className='primary';save.textContent='保存到达时间';save.disabled=true;input.oninput=()=>{save.disabled=!input.validity.valid;};
    save.onclick=()=>{if(!input.validity.valid)return;const [hour,minute]=input.value.split(':').map(Number),start=hour*60+minute;
      undo={id,before:{start:item.start,duration:item.duration},after:{start,duration:item.duration}};item.start=start;revision++;modal.remove();announce('到达时间已设置 · 可撤销');render();scroller.scrollTop=Math.max(0,px(start)-50);};
    const close=document.createElement('button');close.textContent='取消';close.onclick=()=>modal.remove();content.append(h,hint,label,note,save,close);modal.append(content);phone.append(modal);
    modal.addEventListener('pointerdown',e=>e.stopPropagation());modal.addEventListener('pointerup',e=>e.stopPropagation());modal.addEventListener('keydown',e=>{if(e.key==='Escape')modal.remove();e.stopPropagation();});
  }
  function showDetail(id){
    if(entry(id).start==null){showArrivalEditor(id);return;}
    selected=id;paint();const item=entry(id),modal=document.createElement('div');modal.className='demo-detail';modal.setAttribute('role','dialog');modal.setAttribute('aria-label','日程时间详情');
    const content=document.createElement('section');
    const h=document.createElement('h3');h.textContent=`${item.order} ${item.name}`;
    const dl=document.createElement('dl');
    for(const [label,value] of [['所属日期',`第 ${item.day} 天`],['到达时间',fmt(item.start)],['结束时间',item.duration==null?'未设':fmt(item.start+item.duration)],['停留时长',item.duration==null?'未设':`${item.duration} 分钟`]]){const dt=document.createElement('dt'),dd=document.createElement('dd');dt.textContent=label;dd.textContent=value;dl.append(dt,dd);}
    const edit=document.createElement('button');edit.className='primary';edit.textContent='在日历中调时';edit.onclick=()=>{modal.remove();select(id);announce('整条上沿、下沿的任意位置都可拖动；长按主体移动');};
    const close=document.createElement('button');close.textContent='关闭';close.onclick=()=>modal.remove();content.append(h,dl,edit,close);modal.append(content);phone.append(modal);modal.addEventListener('pointerdown',e=>e.stopPropagation());modal.addEventListener('pointerup',e=>e.stopPropagation());
  }
  function startDrag(id,mode,pointer){
    if(isWhole()||gesture)return;
    selected=id;const item=entry(id),placing=item.start==null;
    if(placing&&(item.duration===0||item.duration>1440)){showArrivalEditor(id);return;}
    if(placing)mode='place';
    const duration=displayDuration(item),start=item.start??clamp(Math.round(scroller.scrollTop/HOUR*60/STEP)*STEP+60,0,1440-duration);
    const base={start,duration};gesture={id,mode,before:{...item},base,draft:{...base},anchorY:pointer?.clientY||0,anchorScroll:scroller.scrollTop,keyboard:!pointer,dropValid:!pointer||!placing,grabOffset:pending?.grabOffset??26};
    host.classList.add('editing');
    ghost=document.createElement('div');ghost.className='calendar-ghost';const box=eventBox(item);
    Object.assign(ghost.style,{left:box.left+'%',width:box.width+'%',top:(12+px(start))+'px',height:px(duration)+'px'});if(!placing)grid.append(ghost);
    announce(placing?'拖入时间轴 · 松手设置到达':mode==='move'?'移动中 · 松手保存':mode==='start'?'调整开始 · 结束时间保持不变':'调整结束 · 到达时间保持不变');paint();
  }
  function applyDelta(delta){
    if(!gesture)return;const b=gesture.base,end=b.start+b.duration,change=Math.round(delta/STEP)*STEP;
    if(gesture.mode==='move'||gesture.mode==='place'){
      const steps=clamp(change/STEP,Math.ceil(-b.start/STEP),Math.floor((1440-end)/STEP));
      gesture.draft={start:b.start+steps*STEP,duration:b.duration};
    }else if(gesture.mode==='start'){
      const steps=clamp(change/STEP,Math.ceil(-b.start/STEP),Math.ceil(b.duration/STEP)-1);
      const start=b.start+steps*STEP;gesture.draft={start,duration:end-start};
    }else{
      const steps=clamp(change/STEP,Math.floor(-b.duration/STEP)+1,Math.floor((1440-end)/STEP));
      gesture.draft={start:b.start,duration:b.duration+steps*STEP};
    }
    paint();
  }
  function pointerUpdate(e){
    if(!gesture)return;lastPointer=e;
    const scale=phone.getBoundingClientRect().width/390;
    if(gesture.mode==='place'){
      const rect=scroller.getBoundingClientRect();
      gesture.dropValid=e.clientX>=rect.left+40*scale&&e.clientX<=rect.right&&e.clientY>=rect.top&&e.clientY<=rect.bottom;
      const raw=((e.clientY-rect.top)/scale+scroller.scrollTop-12-gesture.grabOffset)/HOUR*60;
      gesture.draft={start:clamp(Math.round(raw/STEP)*STEP,0,Math.floor((1440-gesture.base.duration)/STEP)*STEP),duration:gesture.base.duration};
      paint();return;
    }
    applyDelta(((e.clientY-gesture.anchorY)/scale+scroller.scrollTop-gesture.anchorScroll)/HOUR*60);
  }
  function autoScroll(){
    edgeFrame=null;if(!gesture||!lastPointer||gesture.keyboard)return;
    const rect=scroller.getBoundingClientRect(),scale=phone.getBoundingClientRect().width/390,y=lastPointer.clientY;
    let direction=0;if(y<rect.top+32*scale)direction=-1;else if(y>rect.bottom-32*scale)direction=1;
    const now=performance.now();
    if(direction!==edgeDirection){edgeDirection=direction;edgeSince=now;}
    if(direction&&now-edgeSince>300&&(gesture.mode!=='place'||gesture.dropValid)){scroller.scrollTop+=direction*Math.min(now-edgeTime,40)*0.07;pointerUpdate(lastPointer);}
    edgeTime=now;
    edgeFrame=requestAnimationFrame(autoScroll);
  }
  function cleanup(){
    if(edgeFrame)cancelAnimationFrame(edgeFrame);edgeFrame=null;lastPointer=null;edgeSince=0;edgeDirection=0;edgeTime=0;
    if(pending?.timer)clearTimeout(pending.timer);pending=null;
    ghost?.remove();ghost=null;host?.classList.remove('editing');
  }
  function commit(){
    if(!gesture)return;const g=gesture;gesture=null;cleanup();
    if(g.mode==='place'&&!g.dropValid){announce('未放入时间轴，保留在待安排');render();return;}
    const item=entry(g.id);if(g.mode==='place'||g.draft.start!==g.base.start||g.draft.duration!==g.base.duration){
      undo={id:g.id,before:{start:g.before.start,duration:g.before.duration},after:{...g.draft}};
      Object.assign(item,g.draft);revision++;announce('时间已更新 · 可撤销');
    }else announce('时间未改变');if(g.mode==='place')render();else paint();
  }
  function cancel(message=''){
    const was=!!gesture,placing=gesture?.mode==='place';gesture=null;cleanup();if(message)announce(message);if(was&&grid){if(placing)render();else paint();}
  }
  function undoChange(){
    if(!undo||gesture||isWhole())return;const item=entry(undo.id);
    if(item.start!==undo.after.start||item.duration!==undo.after.duration){undo=null;announce('时间已再次改变，无法覆盖新修改');inspect();return;}
    Object.assign(item,undo.before);selected=item.id;undo=null;revision++;announce('已撤销，时间已恢复');render();
  }
  function onDown(e){
    if(e.button!==0)return;e.stopPropagation();
    if(gesture){
      if(gesture.mode==='place'&&gesture.keyboard){gesture.keyboard=false;pending={pointerId:e.pointerId,moved:true};host.setPointerCapture(e.pointerId);pointerUpdate(e);edgeFrame=requestAnimationFrame(autoScroll);}
      return;
    }
    const card=e.target.closest('.calendar-event,.pending-card'),handle=e.target.closest('.resize-handle');
    const scale=phone.getBoundingClientRect().width/390;
    pending={id:card?.dataset.itemId,x:e.clientX,y:e.clientY,lastY:e.clientY,scroll:scroller.scrollTop,pointerId:e.pointerId,moved:false,scale,fromPool:card?.classList.contains('pending-card'),grabOffset:card?(e.clientY-card.getBoundingClientRect().top)/scale:0};
    host.setPointerCapture(e.pointerId);
    if(handle&&!isWhole()){e.preventDefault();startDrag(card.dataset.itemId,handle.dataset.edge,e);lastPointer=e;edgeFrame=requestAnimationFrame(autoScroll);}
    else if(card&&!isWhole())pending.timer=setTimeout(()=>{if(pending&&!pending.moved){startDrag(pending.id,'move',e);lastPointer=e;edgeFrame=requestAnimationFrame(autoScroll);}},LONG_PRESS);
  }
  function onMove(e){
    if(!pending||pending.pointerId!==e.pointerId)return;e.stopPropagation();
    if(gesture){e.preventDefault();pointerUpdate(e);return;}
    if(Math.hypot(e.clientX-pending.x,e.clientY-pending.y)>8*pending.scale){pending.moved=true;clearTimeout(pending.timer);}
    if(pending.moved&&!pending.fromPool){e.preventDefault();scroller.scrollTop=pending.scroll-(e.clientY-pending.y)/pending.scale;}
  }
  function onUp(e){
    if(!pending||pending.pointerId!==e.pointerId)return;e.stopPropagation();e.preventDefault();const p=pending;
    if(gesture){pointerUpdate(e);commit();return;}
    cleanup();
    if(p.id&&!p.moved){
      if(isWhole()){const item=entry(p.id);window.show(item.day===1?'day':item.day===2?'day2':'day3');selected=p.id;scroller.scrollTop=Math.max(0,px(item.start)-50);paint();}
      else showDetail(p.id);
    }else if(isWhole()&&Math.abs(e.clientX-p.x)>60*p.scale&&Math.abs(e.clientX-p.x)>Math.abs(e.clientY-p.y)){
      if(scene==='whole'&&e.clientX<p.x)window.show('tail');else if(scene==='tail'&&e.clientX>p.x)window.show('whole');
    }
  }
  function onKey(e){
    if(e.key==='Escape'&&gesture){e.preventDefault();cancel('已取消，时间未改变');return;}
    const card=e.target.closest('.calendar-event,.pending-card');if(!card||isWhole())return;
    if(![' ','Enter','Escape','ArrowUp','ArrowDown'].includes(e.key))return;e.preventDefault();e.stopPropagation();
    const mode=e.target.dataset.edge||'move',id=card.dataset.itemId;
    if(e.key==='Enter'&&!gesture&&entry(id).start==null){showArrivalEditor(id);return;}
    if(e.key==='Escape'){cancel('已取消，时间未改变');return;}
    if(e.key==='Enter'&&gesture){commit();return;}
    if((e.key===' '||e.key==='Enter')&&!gesture){startDrag(id,mode);return;}
    if(e.key==='ArrowUp'||e.key==='ArrowDown'){
      if(!gesture)startDrag(id,mode);if(!gesture)return;const currentDelta=gesture.mode==='start'?gesture.draft.start-gesture.base.start:gesture.mode==='end'?gesture.draft.duration-gesture.base.duration:gesture.draft.start-gesture.base.start;
      applyDelta(currentDelta+(e.key==='ArrowUp'?-STEP:STEP));
    }
  }
  function render(){
    if(!host)return;renderPool();grid.replaceChildren();
    if(isWhole()&&scene==='whole'){const divider=document.createElement('div');divider.className='calendar-column-divider';grid.append(divider);}
    for(let hour=0;hour<=24;hour++){
      const y=12+hour*HOUR,label=document.createElement('span'),line=document.createElement('span');
      label.className='hour-label';label.style.top=(y-6)+'px';label.textContent=fmt(hour*60);
      line.className='hour-line';line.style.top=y+'px';grid.append(label,line);
    }
    for(const item of visibleItems().filter(x=>x.start!=null))grid.append(createCard(item));
    paint();
  }
  function createCard(item){
      const card=document.createElement('div');card.className='calendar-event';card.dataset.itemId=item.id;card.tabIndex=0;card.setAttribute('role','button');
      card.style.setProperty('--event-bg',colors[item.day][0]);card.style.setProperty('--event-color',colors[item.day][1]);
      const title=document.createElement('span'),time=document.createElement('span');title.className='event-title';time.className='event-time';title.textContent=`${item.order}  ${item.name}`;card.append(title,time);
      if(!isWhole())for(const edge of ['start','end']){const h=document.createElement('button');h.className='resize-handle '+edge;h.dataset.edge=edge;h.setAttribute('aria-label',`调整${item.name}${edge==='start'?'开始':'结束'}时间`);card.append(h);}
      positionCard(card,item);return card;
  }
  function mount(key){
    if(key==='list')listWhole=isWhole();
    if(!isWhole()&&['day','day2','day3','half','drag'].includes(scene))listDay=dayForScene();
    if(scroller)scrollPositions[scene]=scroller.scrollTop;cancel();phone.querySelector('.demo-detail')?.remove();phone.querySelectorAll('.demo-list,.demo-list-title,.demo-list-rail').forEach(x=>x.remove());host?.remove();host=null;scroller=null;grid=null;pool=null;poolHeight=0;panel?.remove();panel=null;scene=key;
    if(key==='list'){
      const overlay=document.createElement('div');overlay.className='demo-list';
      const list=document.createElement('div');list.className='demo-list-items';
      for(const item of (listWhole?items:dayItems(listDay))){
        if(listWhole&&item.order===1){const heading=document.createElement('p');heading.className='demo-list-day-heading';heading.textContent=`第 ${item.day} 天 · 4 月 ${11+item.day} 日`;list.append(heading);}
        const row=document.createElement('div');row.className='demo-list-row';
        const at=document.createElement('span');at.className='demo-list-at';at.innerHTML=`<b>${item.order}</b>${fmt(item.start)}`;
        const text=document.createElement('span');text.className='demo-list-text';text.innerHTML=`<strong>${item.name}</strong><small>停留 ${item.duration} 分钟</small>`;
        const icon=document.createElement('span');icon.className='demo-list-icons';icon.textContent='≡　⋮';row.append(at,text,icon);list.append(row);
        if(item.travel){const route=document.createElement('p');route.className='demo-list-route';route.textContent=`${item.mode} · ${item.travel} 分钟`;list.append(route);}
      }
      overlay.append(list);phone.append(overlay);
      const title=document.createElement('div');title.className='demo-list-title';title.innerHTML=`<b>${listWhole?'全程 · 3 天':`第 ${listDay} 天 · ${dayItems(listDay).length} 站`}</b><small>${listWhole?'4 月 12 日 — 4 月 14 日':`4 月 ${11+listDay} 日`}</small>`;phone.append(title);
      const rail=document.createElement('div');rail.className='demo-list-rail';
      for(const day of [0,1,2,3]){const b=document.createElement('button');b.className=(listWhole?day===0:day===listDay)?'active':'';b.innerHTML=day===0?'全程':`第 ${day} 天<small>4/${11+day}</small>`;b.onclick=()=>window.show(day===0?'whole':day===1?'day':day===2?'day2':'day3');rail.append(b);}phone.append(rail);
      return;
    }
    if(!['day','day2','day3','half','whole','tail','drag','edge'].includes(key))return;
    if(key==='drag')selected='d1-1';else if(key==='edge')selected='edge-3';else if(selected&&!visibleItems().some(x=>x.id===selected))selected=null;
    host=document.createElement('div');host.className='live-calendar'+(isWhole()?' whole-calendar':'');
    const top=key==='half'?568:isWhole()?214:184,height=key==='half'?236:isWhole()?590:620;
    host.style.top=top/8.44+'%';host.style.height=height/8.44+'%';
    if(isWhole()){
      const headers=document.createElement('div');headers.className='column-headings';
      for(const [index,day] of (key==='tail'?[3]:[1,2]).entries()){
        const b=document.createElement('button');b.className='column-heading';b.style.left=(38+index*123)/282*100+'%';b.innerHTML=`第 ${day} 天<small>4/${11+day} 周${['日','一','二'][day-1]}</small>`;b.setAttribute('aria-label',`查看第${day}天`);b.onclick=()=>window.show(day===1?'day':day===2?'day2':'day3');headers.append(b);
      }host.append(headers);
    }
    scroller=document.createElement('div');scroller.className='calendar-scroll';scroller.setAttribute('aria-label',isWhole()?'全程时间日历，只读':'单日时间日历，可拖动调时');
    grid=document.createElement('div');grid.className='calendar-grid';grid.style.height='1280px';scroller.append(grid);host.append(scroller);
    bubble=document.createElement('div');bubble.className='time-bubble';bubble.hidden=true;host.append(bubble);phone.append(host);
    host.addEventListener('pointerdown',onDown);host.addEventListener('pointermove',onMove);host.addEventListener('pointerup',onUp);host.addEventListener('pointercancel',()=>cancel('已取消，时间未改变'));
    host.addEventListener('lostpointercapture',()=>{if(pending)cancel('已取消，时间未改变');});
    host.addEventListener('keydown',onKey);scroller.addEventListener('scroll',()=>{if(gesture)paint();});
    makePanel();render();scroller.scrollTop=scrollPositions[key]??(9*HOUR);inspect();
  }
  document.addEventListener('visibilitychange',()=>{if(document.hidden)cancel('已取消，时间未改变');});
  window.addEventListener('blur',()=>cancel('已取消，时间未改变'));
  window.CalendarDemo={mount,cancel,isDragging:()=>!!gesture,listTarget:()=>listWhole?'whole':listDay===2?'day2':listDay===3?'day3':'day'};
  mount(current);
})();
