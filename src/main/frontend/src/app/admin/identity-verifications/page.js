"use client";
import { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import AppShell from "../../components/app-shell";
import { apiJson } from "../../lib/api";
const statuses={WAITING_IDENTITY:"در انتظار تأیید هویت",REJECTED_IDENTITY:"رد هویت",NEEDS_REVIEW:"نیازمند بررسی",WAITING_ORGANIZATION:"صادرشده؛ در انتظار تعیین واحد",SUCCESS:"صادرشده",PAPER_CREATED:"صادرشده",WORKFLOW_START_FAILED:"صادرشده؛ خطای شروع گردش کار",RECEIVED:"دریافت‌شده"};
export default function IdentityVerifications() {
  const [items,setItems]=useState([]),[selected,setSelected]=useState(null),[form,setForm]=useState({}),[busy,setBusy]=useState(false),[error,setError]=useState("");
  const load=useCallback(async()=>{try {setItems(await apiJson("/api/v1/admin/identity-verifications"));} catch(e){setError(e.message);}},[]);
  useEffect(()=>{load();},[load]);
  async function decide(verified) {
    setBusy(true);setError("");
    try {await apiJson(`/api/v1/admin/identity-verifications/${selected.id}/decision`,{method:"POST",body:JSON.stringify({...form,verified})});setSelected(null);await load();}
    catch(e){setError(e.message);await load();} finally {setBusy(false);}
  }
  return <AppShell active="admin" title="شبیه‌ساز تأیید هویت">
    <p>درخواست تا دریافت پاسخ مثبت هویت در انتظار می‌ماند. نام تأییدشده را از پاسخ شبیه‌ساز وارد کنید.</p>
    {error && <p role="alert">{error}</p>}
    <button onClick={load} disabled={busy}>به‌روزرسانی</button>
    <table className="table"><thead><tr><th>شناسه درخواست</th><th>شناسه ملی</th><th>وضعیت</th><th>نتیجه</th></tr></thead><tbody>
    {items.map(item=><tr key={item.id}><td>{item.referenceId}</td><td>{item.identifier}</td><td>{statuses[item.status] || item.status}<div>{item.reason}</div></td><td>
      {item.status==="WAITING_IDENTITY" && <button disabled={busy} onClick={()=>{setSelected(item);setForm({});setError("");}}>پاسخ استعلام</button>}
      {item.executionPaperId && <Link href={`/execution-papers/${item.executionPaperId}`}>مشاهده برگ اجرا</Link>}
    </td></tr>)}</tbody></table>
    {selected && <section className="card" dir="rtl"><h2>پاسخ هویت {selected.identifier}</h2>
      {[["firstName","نام"],["lastName","نام خانوادگی"],["fatherName","نام پدر"],["birthDate","تاریخ تولد"],["companyName","نام شرکت (حقوقی)"],["registrationNumber","شماره ثبت شرکت"],["registrationDate","تاریخ ثبت شرکت"],["reason","علت رد"]].map(([key,label])=><label key={key} style={{display:"block",margin:"12px 0"}}>{label}<input value={form[key] || ""} onChange={e=>setForm({...form,[key]:e.target.value})}/></label>)}
      <button disabled={busy} onClick={()=>decide(true)}>تأیید هویت و ادامه صدور</button> <button disabled={busy} onClick={()=>decide(false)}>رد هویت و درخواست</button> <button disabled={busy} onClick={()=>setSelected(null)}>بستن</button>
    </section>}
  </AppShell>;
}
