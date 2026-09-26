#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:3000}"
ENDPOINT="$BASE_URL/api/v1/debt-papers/issue"

for i in 2 3 4 5 6 7 ; do
  portal_id="514${i}62129${i}"
  curl -sS -X POST "$ENDPOINT" \
    -H 'Content-Type: application/json' \
    -d @- <<JSON
{
  "EblagLetterNo_tbz": "BL-1403-00013${i}",
  "EblagLetterDate_tbz": "2026-06-0${i}",
  "eblaghstatus_Tbz": "مشاهده شده",
  "EblagLetterrecievdDate_tbz": "2026-06-1${i}",
  "ID_REF_PORTAL": "${portal_id}",
  "bedehitype": "1",
  "EdareKolTitle_tbz": "اداره کل امور مالیاتی تهران",
  "OmurCode_tbz": "1204",
  "CODE_ACTIVITY_tbz": "${i}",
  "VahedCode_tbz": "6666",
  "EdareAddress_tbz": "تهران، خیابان ولیعصر، ساختمان مالیاتی",
  "EdareTel_tbz": "02188776655",
  "KelaseID_tbz": "5",
  "NationalID_tbz": "001244567${i}",
  "SenderVahedCode_tbz": "1203",
  "FName_tbz": "Mohammad${i}",
  "LName_tbz": "Ahmadi",
  "CodeMelli_tbz": "001134567${i}",
  "BirthDate_tbz": "1988-05-12",
  "ZipCode_tbz": "1587541236",
  "tbz_MoadiAddress": "Tehran, Vanak St, No. ${i}",
  "Economiccode_Tbz": "123456789${i}",
  "Tbz_Business_reputation": "Ahmadi Trading ${i}",
  "intacode_Tbz": "INTA-00${i}",
  "MoadiTel_tbz": "0912123456${i}",
  "SodurPlace_tbz": "Tehran",
  "SNO_tbz": "A-99887${i}",
  "JarimeDate_tbz": "2026-05-20",
  "GatiNo_tbz": "G-44556${i}",
  "GatiDate_tbz": "2026-05-10",
  "SenderKelaseID_tbz": "5",
  "ElatGatiat_tbz": "عدم پرداخت اقساط مالیاتی",
  "taxtypecode_Tbz": "VAT",
  "tbz_AmalKard": "عملکرد 1403",
  "tbz_Dore": "1403",
  "tbz_AslMaliat": "$((150000000 + i * 1000000))",
  "tbz_AslAvarez": "30000000",
  "JarimeSabet_tbz": "5000000",
  "JarimeDirkard_tbz": "2000000",
  "penaltycs_Tbz": "1000000",
  "totalpenalty_Tbz": "7000000",
  "penaltyoff_Tbz": "0",
  "Sum_tbz": "187000000",
  "pardakhtoasl_Tbz": "120000000",
  "pardakhtijarime_Tbz": "5000000",
  "tbz_Pardakhti": "125000000",
  "Etebar_tbz": "0",
  "MandeBedehi_tbz": "62000000"
}
JSON
  echo

done
