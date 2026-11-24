@powershell -NoProfile -NonInteractive -ExecutionPolicy Bypass -Command "(Import-Clixml (Join-Path $env:USERPROFILE '.deploy\cafe24.xml')).GetNetworkCredential().Password"
