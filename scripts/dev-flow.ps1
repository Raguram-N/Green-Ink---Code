$ErrorActionPreference = "Stop"
$Base = "http://localhost:8787/api/v1"

Write-Host "1. Request OTP"
$otp = Invoke-RestMethod -Method Post -Uri "$Base/auth/otp/request" -ContentType "application/json" -Body '{"identifier":"9876543210"}'
Write-Host "Challenge:" $otp.challengeId "Dev OTP:" $otp.debugOtp

Write-Host "2. Verify OTP"
$verifyBody = @{ challengeId = $otp.challengeId; otp = $otp.debugOtp } | ConvertTo-Json
$auth = Invoke-RestMethod -Method Post -Uri "$Base/auth/otp/verify" -ContentType "application/json" -Body $verifyBody
$Headers = @{ Authorization = "Bearer $($auth.accessToken)" }

Write-Host "3. Confirm Premium is initially false"
Invoke-RestMethod -Method Get -Uri "$Base/me" -Headers $Headers | ConvertTo-Json -Depth 6

Write-Host "4. Create yearly demo order"
$order = Invoke-RestMethod -Method Post -Uri "$Base/billing/orders" -Headers $Headers -ContentType "application/json" -Body '{"planCode":"YEARLY"}'
$order | ConvertTo-Json

Write-Host "5. Verify demo payment"
$paymentBody = @{
    orderId = $order.orderId
    paymentId = "demo_pay_1"
    signature = "dev-valid-signature"
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$Base/billing/payments/verify" -Headers $Headers -ContentType "application/json" -Body $paymentBody | ConvertTo-Json

Write-Host "6. Open premium Notes fixture"
Invoke-RestMethod -Method Get -Uri "$Base/chapters/u1-c2/notes" -Headers $Headers | ConvertTo-Json -Depth 4

Write-Host "7. Mark free chapter Notes complete"
Invoke-RestMethod -Method Put -Uri "$Base/me/progress/chapters/u1-c1" -Headers $Headers -ContentType "application/json" -Body '{"notesCompleted":true}' | ConvertTo-Json

Write-Host "Done."
