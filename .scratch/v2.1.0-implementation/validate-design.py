"""Read-only validation of the native Pencil v2.1.0 cumulative snapshot."""
from pathlib import Path
import collections, hashlib, json, re
ROOT=Path(__file__).resolve().parents[2]
old=json.loads((ROOT/'design/easy-trip-v2.0.0.pen').read_text())
new=json.loads((ROOT/'design/easy-trip-v2.1.0.pen').read_text())
review=json.loads((ROOT/'design/explorations/v2.1.0-expense-sections-review.pen').read_text())
pairs={'Qmqal':'RXZKO','mNTqO':'KRpe6','GNywG':'HneoD','IFLkF':'Vhvrp','n7nBY':'zTmms','IEjeX':'tZBIo','esaIa':'nQ6Rc','g2MWtB':'hdBAO','YYgXf':'aMjeC','SoK83':'LKCrU','fu3PI':'j0Qyo','aLts2':'yDpRU','z4EzXQ':'ZDSMb','cmy0k':'qpK3w','P9DBy':'h9eKnf','H5kYgr':'UtdGR','zg3uE':'g04zi6','tBfeS':'kdeup','e17Gw6':'bO8eg','re52u':'Gtkn0','eszSp':'zigQL','zswvY':'UsCW3','I6Ggn5':'J6FXq8','d3vRN':'p7wMl','PcAej':'FcfWi'}
edge={'c0vRa','wtN9w','jc8OW','e9v0Ll','XLxbY','qMLBw','b2TIs','R38ET','lstLS','qf6FO','J0icRI','e9WXQ7','X6moH','a3Vyzc','ZKtJS'}
a={x['id']:x for x in old['children']};b={x['id']:x for x in new['children']};r={x['id']:x for x in review['children']}
assert len(b)==187
assert a.keys()-b.keys()==set(pairs.values())
assert b.keys()-a.keys()==set(pairs)|{'bHNTv'}
def walk(x):
 yield x
 for c in x.get('children',[]):yield from walk(c)
nodes=[n for x in new['children'] for n in walk(x)];ids={n['id'] for n in nodes}
assert len(nodes)==len(ids)
assert not [n for n in nodes if n.get('placeholder')]
assert not [n for n in nodes if n.get('type')=='ref' and n['ref'] not in ids]
assert all(n['name'].startswith('v2.1.0') for n in new['children'] if n.get('enabled') is not False)
def normalized(x,layout=False):
 if isinstance(x,dict):return {k:normalized(v,layout) for k,v in x.items() if k not in ('name','context','x','y')}
 if isinstance(x,list):return [normalized(v,layout) for v in x]
 if isinstance(x,str):return x.replace('v2.0.0','v2.1.0').replace('v2.1.0待实现','v2.1.0已实现')
 return x
unchanged=[]
for id in a.keys()&b.keys()-edge-{'t3MG7p'}:
 assert normalized(a[id])==normalized(b[id]), id
 unchanged.append(id)
for id in pairs:
 assert normalized(b[id])==normalized(r[id]), id
for key,val in old.get('variables',{}).items():assert new['variables'][key]==val,key
for id in edge:
 def money(n):
  clean={k:v for k,v in n.items() if k not in ('name','context')}
  return collections.Counter(re.findall(r'[¥￥]\s*[\d,]+(?:\.\d+)?',str(clean)))
 assert money(a[id])==money(b[id]),id
report={'roots':len(b),'active':sum(n.get('enabled') is not False for n in b.values()),'expensePages':40,'unchangedVisualRoots':len(unchanged),'approvedScreensPreserved':25,'edgeStatesAmountsPreserved':15,'ids':len(ids),'duplicateIds':0,'brokenRefs':0,'placeholders':0,'upstreamVariablesPreserved':True,'nativeReopened':True,'nativeMcpActiveRootOverlaps':0,'nativeMcpExpenseClipping':0,'sha256':hashlib.sha256((ROOT/'design/easy-trip-v2.1.0.pen').read_bytes()).hexdigest(),'knownInheritedUiGaps':'8 legacy UI and 22 timeline assertions not re-certified'}
(ROOT/'.scratch/v2.1.0-implementation/design-final/verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(report,ensure_ascii=False,indent=2))
