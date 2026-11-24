# 프론트(Nuxt) + 백엔드(Spring)를 ROOT.war 하나로 묶어 카페24(yyuna03.cafe24.com)에 배포한다.
#   .\scripts\deploy.ps1 -Setup         서버 비밀번호를 한 번 입력해 이 PC에 암호화 저장
#   .\scripts\deploy.ps1                 바뀐 파일이 있으면 배포
#   .\scripts\deploy.ps1 -Force          바뀐 파일이 없어도 배포
#   .\scripts\deploy.ps1 -MarkDeployed   배포하지 않고 지금 상태를 "배포됨"으로 기록
#   .\scripts\deploy.ps1 -Hook           Claude Code Stop 훅용 (결과를 JSON systemMessage로 출력)
param(
  [switch]$Setup,
  [switch]$Force,
  [switch]$MarkDeployed,
  [switch]$Hook
)

# 네이티브 명령(nuxt, gradle, ssh)의 stderr 경고로 멈추지 않도록 Continue, 실패는 종료 코드로 판단
$ErrorActionPreference = 'Continue'

$FeRoot = Split-Path $PSScriptRoot -Parent
$BeRoot = 'D:\toyy\domain_be'
$SshHost = 'yyuna03@yyuna03.cafe24.com'
$SiteUrl = 'http://yyuna03.cafe24.com'
$StateDir = Join-Path $env:USERPROFILE '.deploy'
$CredFile = Join-Path $StateDir 'cafe24.xml'
$StampFile = Join-Path $StateDir 'last-deployed.txt'
$LockFile = Join-Path $StateDir 'deploy.lock'
$LogFile = Join-Path $StateDir 'deploy.log'

New-Item -ItemType Directory -Force $StateDir | Out-Null

function Log($msg) {
  Add-Content -Path $LogFile -Encoding UTF8 -Value "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $msg"
  if (-not $Hook) { Write-Host $msg }
}

# 훅이면 JSON 한 줄만 stdout으로 내보낸다 (그 외 출력은 전부 로그 파일로)
function Finish($msg) {
  Log $msg
  if ($Hook) { @{ systemMessage = $msg } | ConvertTo-Json -Compress }
  exit 0
}

if ($Setup) {
  $pw = Read-Host "$SshHost 비밀번호 (입력한 글자는 보이지 않아요)" -AsSecureString
  New-Object System.Management.Automation.PSCredential('yyuna03', $pw) | Export-Clixml -Path $CredFile
  Write-Host "저장했어요: $CredFile (이 PC의 이 Windows 계정에서만 풀 수 있어요)"
  exit 0
}

# 배포에 영향을 주는 파일들의 이름·크기·수정시각으로 지문을 만든다
function Get-Fingerprint {
  $files = @()
  $files += Get-ChildItem -Recurse -File (Join-Path $FeRoot 'app'), (Join-Path $FeRoot 'public')
  $files += Get-Item (Join-Path $FeRoot 'nuxt.config.ts'), (Join-Path $FeRoot 'package.json'), (Join-Path $FeRoot 'pnpm-lock.yaml')
  $files += Get-ChildItem -Recurse -File (Join-Path $BeRoot 'src') | Where-Object { $_.FullName -notmatch '\\resources\\static\\' }
  $files += Get-Item (Join-Path $BeRoot 'build.gradle'), (Join-Path $BeRoot 'settings.gradle')
  $text = ($files | Sort-Object FullName | ForEach-Object { "$($_.FullName)|$($_.Length)|$($_.LastWriteTimeUtc.Ticks)" }) -join "`n"
  $sha = [Security.Cryptography.SHA256]::Create()
  [BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($text))) -replace '-', ''
}

$fingerprint = Get-Fingerprint
$last = if (Test-Path $StampFile) { (Get-Content $StampFile -Raw).Trim() } else { '' }

if ($MarkDeployed) {
  Set-Content -Path $StampFile -Value $fingerprint -Encoding ASCII
  Finish '지금 상태를 배포된 것으로 기록했어요.'
}

if (-not $Force -and $fingerprint -eq $last) {
  if ($Hook) { exit 0 }  # 바뀐 게 없으면 훅은 조용히 끝낸다
  Finish '바뀐 파일이 없어서 배포하지 않았어요.'
}

# %USERPROFILE%\.deploy\paused 파일이 있으면 자동 배포를 쉰다 (DB 변경이 먼저 필요한 작업 중 등)
if ($Hook -and (Test-Path (Join-Path $StateDir 'paused'))) {
  Finish '자동 배포 일시 정지 중이라 배포하지 않았어요. (다시 켜려면 %USERPROFILE%\.deploy\paused 파일 삭제)'
}

if (-not (Test-Path $CredFile)) {
  Finish '자동 배포 대기: 서버 비밀번호가 저장되어 있지 않아요. 터미널에서 .\scripts\deploy.ps1 -Setup 을 한 번 실행해 주세요.'
}

# 동시에 두 번 배포되지 않게 잠금 (30분 넘은 잠금은 비정상 종료로 보고 무시)
if ((Test-Path $LockFile) -and ((Get-Item $LockFile).LastWriteTime -gt (Get-Date).AddMinutes(-30))) {
  Finish '이미 다른 배포가 진행 중이라 이번 배포는 건너뛰었어요.'
}
Set-Content -Path $LockFile -Value $PID

try {
  $env:Path = "C:\Program Files\nodejs;$env:APPDATA\npm;$env:Path"
  $env:NUXT_TELEMETRY_DISABLED = '1'
  $env:JAVA_HOME = Join-Path $env:USERPROFILE '.jdks\ms-21.0.12.1'
  Push-Location $FeRoot
  Log '=== 배포 시작'

  # 1. 검사: 에러가 있으면 배포하지 않는다
  & .\node_modules\.bin\nuxt.cmd prepare *>> $LogFile
  & .\node_modules\.bin\vue-tsc.cmd --noEmit -p .nuxt/tsconfig.app.json *>> $LogFile
  if ($LASTEXITCODE -ne 0) { Finish "자동 배포 중단: 타입 검사 에러가 있어요. 로그: $LogFile" }
  & .\node_modules\.bin\eslint.cmd app --quiet *>> $LogFile
  if ($LASTEXITCODE -ne 0) { Finish "자동 배포 중단: ESLint 에러가 있어요. 로그: $LogFile" }

  # 2. 프론트 빌드 → 백엔드 static 폴더로 복사
  & .\node_modules\.bin\nuxt.cmd generate *>> $LogFile
  if ($LASTEXITCODE -ne 0) { Finish "자동 배포 중단: 프론트 빌드 실패. 로그: $LogFile" }
  robocopy (Resolve-Path .output\public).Path (Join-Path $BeRoot 'src\main\resources\static') /MIR /NFL /NDL /NJH /NJS /NP *>> $LogFile
  if ($LASTEXITCODE -ge 8) { Finish "자동 배포 중단: static 복사 실패. 로그: $LogFile" }

  # 3. ROOT.war 생성
  Push-Location $BeRoot
  & .\gradlew.bat bootWar --console=plain '-Djavax.net.ssl.trustStoreType=Windows-ROOT' *>> $LogFile
  $gradleExit = $LASTEXITCODE
  Pop-Location
  if ($gradleExit -ne 0) { Finish "자동 배포 중단: WAR 빌드 실패. 로그: $LogFile" }
  $war = Get-Item (Join-Path $BeRoot 'build\libs\ROOT.war')

  # 4. 업로드 → 톰캣 정지 → 교체 → 시작 (비밀번호는 askpass가 암호화 파일에서 꺼내 ssh에 넘긴다)
  $env:SSH_ASKPASS = Join-Path $PSScriptRoot 'deploy-askpass.cmd'
  $env:SSH_ASKPASS_REQUIRE = 'force'
  $env:DISPLAY = 'none'
  $opt = @('-o', 'NumberOfPasswordPrompts=1', '-o', 'ConnectTimeout=20', '-o', 'PreferredAuthentications=password')

  scp @opt $war.FullName "${SshHost}:~/ROOT.war.new" *>> $LogFile
  if ($LASTEXITCODE -ne 0) { Finish "자동 배포 중단: 업로드 실패 (비밀번호가 바뀌었으면 -Setup 을 다시 실행). 로그: $LogFile" }

  # 서버 셸에는 stat/sleep이 없어서 ls|grep으로 크기를 확인한다. 크기가 다르면 아무것도 바꾸지 않는다
  $remote = "ls -l ~/ROOT.war.new | grep -qw $($war.Length) || { echo SIZE_MISMATCH; exit 1; }; " +
            '~/tomcat/bin/shutdown.sh >/dev/null 2>&1; killtomcat; ' +
            # 용량 제한이 있어 복사(cp) 대신 이동(mv)으로 이전 버전을 한 개만 남긴다
            'rm -f ~/ROOT.war.prev; mv ~/tomcat/webapps/ROOT.war ~/ROOT.war.prev; mv ~/ROOT.war.new ~/tomcat/webapps/ROOT.war; rm -rf ~/tomcat/webapps/ROOT; ' +
            '. ~/.bash_profile; ~/tomcat/bin/startup.sh </dev/null >/dev/null 2>&1; echo DEPLOYED'
  $out = ssh @opt $SshHost $remote 2>> $LogFile
  Log "ssh: $out"
  if (-not ($out -match 'DEPLOYED')) { Finish "자동 배포 실패: 서버 교체 단계에서 멈췄어요 ($out). 사이트는 이전 버전이에요. 로그: $LogFile" }

  # 5. 확인: 새 index.html이 뜰 때까지 최대 2분 대기 (통신사가 끼워 넣는 짧은 페이지는 건너뜀)
  $local = (Get-Content .output\public\index.html -Raw -Encoding UTF8).Trim()
  $ok = $false
  for ($i = 1; $i -le 24 -and -not $ok; $i++) {
    Start-Sleep -Seconds 5
    try {
      $r = Invoke-WebRequest "$SiteUrl/index.html?v=$(Get-Random)" -UseBasicParsing -TimeoutSec 15
      $live = [Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()).Trim()
      if ($live -eq $local) { $ok = $true }
    } catch { }
  }
  if (-not $ok) { Finish "자동 배포 확인 실패: 서버 교체는 했지만 2분 안에 새 버전이 확인되지 않았어요. 사이트를 확인해 주세요. 되돌리기용 이전 버전: ~/ROOT.war.prev" }

  Set-Content -Path $StampFile -Value $fingerprint -Encoding ASCII
  Finish "자동 배포 완료: $SiteUrl 에 새 버전이 올라갔어요. (Ctrl+F5로 확인)"
} finally {
  Pop-Location -ErrorAction SilentlyContinue
  Remove-Item $LockFile -ErrorAction SilentlyContinue
}
