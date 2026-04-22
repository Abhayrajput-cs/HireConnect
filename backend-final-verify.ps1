$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http

function New-Client([bool]$allowRedirect = $true) {
    $handler = [System.Net.Http.HttpClientHandler]::new()
    $handler.AllowAutoRedirect = $allowRedirect
    $handler.CookieContainer = [System.Net.CookieContainer]::new()
    $client = [System.Net.Http.HttpClient]::new($handler)
    $client.Timeout = [TimeSpan]::FromSeconds(30)
    return $client
}

function Send-Request {
    param(
        [System.Net.Http.HttpClient]$Client,
        [string]$Method,
        [string]$Url,
        $Body = $null,
        [string]$ContentType = 'application/json',
        [hashtable]$Headers = @{}
    )
    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::new($Method), $Url)
    foreach ($key in $Headers.Keys) {
        if (-not $request.Headers.TryAddWithoutValidation($key, [string]$Headers[$key])) {
            if ($null -ne $Body) {
                if ($ContentType -eq 'application/x-www-form-urlencoded' -and $Body -is [hashtable]) {
                    $pairs = New-Object 'System.Collections.Generic.List[System.Collections.Generic.KeyValuePair[string,string]]'
                    foreach ($bk in $Body.Keys) {
                        $pairs.Add([System.Collections.Generic.KeyValuePair[string,string]]::new([string]$bk, [string]$Body[$bk]))
                    }
                    $request.Content = [System.Net.Http.FormUrlEncodedContent]::new($pairs)
                } elseif ($Body -is [string]) {
                    $request.Content = [System.Net.Http.StringContent]::new($Body, [System.Text.Encoding]::UTF8, $ContentType)
                } else {
                    $json = $Body | ConvertTo-Json -Depth 10
                    $request.Content = [System.Net.Http.StringContent]::new($json, [System.Text.Encoding]::UTF8, $ContentType)
                }
            }
            $request.Content.Headers.TryAddWithoutValidation($key, [string]$Headers[$key]) | Out-Null
        }
    }
    if ($null -ne $Body -and $null -eq $request.Content) {
        if ($ContentType -eq 'application/x-www-form-urlencoded' -and $Body -is [hashtable]) {
            $pairs = New-Object 'System.Collections.Generic.List[System.Collections.Generic.KeyValuePair[string,string]]'
            foreach ($bk in $Body.Keys) {
                $pairs.Add([System.Collections.Generic.KeyValuePair[string,string]]::new([string]$bk, [string]$Body[$bk]))
            }
            $request.Content = [System.Net.Http.FormUrlEncodedContent]::new($pairs)
        } elseif ($Body -is [string]) {
            $request.Content = [System.Net.Http.StringContent]::new($Body, [System.Text.Encoding]::UTF8, $ContentType)
        } else {
            $json = $Body | ConvertTo-Json -Depth 10
            $request.Content = [System.Net.Http.StringContent]::new($json, [System.Text.Encoding]::UTF8, $ContentType)
        }
    }
    $response = $Client.SendAsync($request).GetAwaiter().GetResult()
    $content = if ($response.Content) { $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() } else { '' }
    [pscustomobject]@{
        Status = [int]$response.StatusCode
        Body = $content
        Headers = $response.Headers
    }
}

function Parse-Json([string]$Text) {
    if ([string]::IsNullOrWhiteSpace($Text)) { return $null }
    return $Text | ConvertFrom-Json
}

$checks = New-Object System.Collections.Generic.List[object]
function Check([bool]$Condition, [string]$Name, [string]$Detail) {
    $checks.Add([pscustomobject]@{ name = $Name; ok = $Condition; detail = $Detail }) | Out-Null
    if (-not $Condition) {
        throw "FAILED: $Name :: $Detail"
    }
}

$rest = New-Client
$noRedirect = New-Client $false
$candidateWeb = New-Client
$recruiterWeb = New-Client
$adminWeb = New-Client

$base = @{
    eureka = 'http://localhost:8761'
    gateway = 'http://localhost:8080'
    auth = 'http://localhost:8081'
    profile = 'http://localhost:8084'
    job = 'http://localhost:8085'
    application = 'http://localhost:8086'
    interview = 'http://localhost:8087'
    notification = 'http://localhost:8088'
    web = 'http://localhost:8090'
}

foreach ($entry in @(
    @{ name='eureka'; url="$($base.eureka)/actuator/health" },
    @{ name='gateway'; url="$($base.gateway)/actuator/health" },
    @{ name='auth'; url="$($base.auth)/actuator/health" },
    @{ name='profile'; url="$($base.profile)/actuator/health" },
    @{ name='job'; url="$($base.job)/actuator/health" },
    @{ name='application'; url="$($base.application)/actuator/health" },
    @{ name='interview'; url="$($base.interview)/actuator/health" },
    @{ name='notification'; url="$($base.notification)/actuator/health" },
    @{ name='web'; url="$($base.web)/actuator/health" }
)) {
    $resp = Send-Request -Client $rest -Method 'GET' -Url $entry.url
    $json = Parse-Json $resp.Body
    Check ($resp.Status -eq 200 -and $json.status -eq 'UP') "health:$($entry.name)" $resp.Body
}

$eurekaPage = Send-Request -Client $rest -Method 'GET' -Url $base.eureka
Check ($eurekaPage.Status -eq 200 -and $eurekaPage.Body -match 'AUTH-SERVICE' -and $eurekaPage.Body -match 'JOB-SERVICE' -and $eurekaPage.Body -match 'HIRECONNECT-WEB') 'eureka:registry' 'expected services listed in registry page'

$stamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$candidateEmail = "candidate.$stamp@example.com"
$recruiterEmail = "recruiter.$stamp@example.com"
$tempCandidateEmail = "candidate.temp.$stamp@example.com"
$adminEmail = 'admin@hireconnect.local'
$candidatePassword = 'Secure123'
$recruiterPassword = 'Secure123'
$adminPassword = 'Admin@1234'
$candidateMobile = [int64](9000000000 + ($stamp % 999999999))
$recruiterMobile = [int64](8000000000 + ($stamp % 999999999))
$tempCandidateMobile = [int64](7000000000 + ($stamp % 999999999))

$registerCandidate = Send-Request -Client $rest -Method 'POST' -Url "$($base.auth)/auth/register" -Body @{ email=$candidateEmail; password=$candidatePassword; role='CANDIDATE' }
$registerCandidateJson = Parse-Json $registerCandidate.Body
Check ($registerCandidate.Status -eq 201 -and $registerCandidateJson.user.role -eq 'CANDIDATE') 'auth:register-candidate' $registerCandidate.Body

$registerRecruiter = Send-Request -Client $rest -Method 'POST' -Url "$($base.auth)/auth/register" -Body @{ email=$recruiterEmail; password=$recruiterPassword; role='RECRUITER' }
$registerRecruiterJson = Parse-Json $registerRecruiter.Body
Check ($registerRecruiter.Status -eq 201 -and $registerRecruiterJson.user.role -eq 'RECRUITER') 'auth:register-recruiter' $registerRecruiter.Body

$duplicateCandidate = Send-Request -Client $rest -Method 'POST' -Url "$($base.auth)/auth/register" -Body @{ email=$candidateEmail; password=$candidatePassword; role='CANDIDATE' }
Check ($duplicateCandidate.Status -eq 409) 'auth:duplicate-register' $duplicateCandidate.Body

$candidateLoginResp = Send-Request -Client $rest -Method 'POST' -Url "$($base.auth)/auth/login" -Body @{ email=$candidateEmail; password=$candidatePassword }
$candidateLogin = Parse-Json $candidateLoginResp.Body
Check ($candidateLoginResp.Status -eq 200 -and -not [string]::IsNullOrWhiteSpace($candidateLogin.accessToken)) 'auth:login-candidate' $candidateLoginResp.Body

$recruiterLoginResp = Send-Request -Client $rest -Method 'POST' -Url "$($base.auth)/auth/login" -Body @{ email=$recruiterEmail; password=$recruiterPassword }
$recruiterLogin = Parse-Json $recruiterLoginResp.Body
Check ($recruiterLoginResp.Status -eq 200 -and -not [string]::IsNullOrWhiteSpace($recruiterLogin.accessToken)) 'auth:login-recruiter' $recruiterLoginResp.Body

$adminLoginResp = Send-Request -Client $rest -Method 'POST' -Url "$($base.auth)/auth/login" -Body @{ email=$adminEmail; password=$adminPassword }
$adminLogin = Parse-Json $adminLoginResp.Body
Check ($adminLoginResp.Status -eq 200 -and $adminLogin.user.role -eq 'ADMIN') 'auth:login-admin' $adminLoginResp.Body

$candidateHeaders = @{ Authorization = "Bearer $($candidateLogin.accessToken)" }
$recruiterHeaders = @{ Authorization = "Bearer $($recruiterLogin.accessToken)" }
$adminHeaders = @{ Authorization = "Bearer $($adminLogin.accessToken)" }

$authMe = Parse-Json (Send-Request -Client $rest -Method 'GET' -Url "$($base.auth)/auth/me" -Headers $candidateHeaders).Body
Check ($authMe.email -eq $candidateEmail) 'auth:me' ($authMe | ConvertTo-Json -Depth 5)

$refreshResp = Send-Request -Client $rest -Method 'POST' -Url "$($base.auth)/auth/refresh" -Body @{ refreshToken = $candidateLogin.refreshToken }
$refreshJson = Parse-Json $refreshResp.Body
Check ($refreshResp.Status -eq 200 -and -not [string]::IsNullOrWhiteSpace($refreshJson.accessToken)) 'auth:refresh' $refreshResp.Body

$validateResp = Send-Request -Client $rest -Method 'POST' -Url "$($base.auth)/auth/validate" -Body @{ token = $candidateLogin.accessToken }
$validateJson = Parse-Json $validateResp.Body
Check ($validateResp.Status -eq 200 -and $validateJson.valid -eq $true) 'auth:validate' $validateResp.Body

$invalidLogin = Send-Request -Client $rest -Method 'POST' -Url "$($base.auth)/auth/login" -Body @{ email=$candidateEmail; password='Wrong123' }
Check ($invalidLogin.Status -eq 401) 'auth:invalid-login' $invalidLogin.Body

$oauthStart = Send-Request -Client $noRedirect -Method 'GET' -Url "$($base.auth)/oauth2/authorization/github?role=RECRUITER"
Check ($oauthStart.Status -eq 302) 'auth:github-oauth-start' 'expected redirect to GitHub'

foreach ($entry in @(
    @{ name='profile'; url="$($base.profile)/api/v1/profiles" },
    @{ name='job'; url="$($base.job)/api/v1/jobs" },
    @{ name='application'; url="$($base.application)/api/v1/applications/job/1" },
    @{ name='interview'; url="$($base.interview)/api/v1/interviews/status/SCHEDULED" },
    @{ name='notification'; url="$($base.notification)/api/v1/notifications/user/1" }
)) {
    $resp = Send-Request -Client $rest -Method 'GET' -Url $entry.url
    Check ($resp.Status -eq 401) "security:unauthorized-$($entry.name)" $resp.Body
}

$candidateProfileResp = Send-Request -Client $rest -Method 'POST' -Url "$($base.profile)/api/v1/profiles/candidates" -Headers $candidateHeaders -Body @{
    fullName = 'Final Candidate'
    email = $candidateEmail
    mobile = $candidateMobile
    dob = '1999-01-01'
    gender = 'MALE'
    skills = @('Java','Spring Boot','MySQL')
    experience = 3
    resumeUrl = 'https://files.example.com/resume/final-candidate.pdf'
    addresses = @(@{ houseNo='12A'; street='MG Road'; city='Pune'; state='Maharashtra'; pincode=411001 })
}
$candidateProfile = Parse-Json $candidateProfileResp.Body
Check ($candidateProfileResp.Status -eq 201 -and $candidateProfile.role -eq 'CANDIDATE') 'profile:create-candidate' $candidateProfileResp.Body

$recruiterProfileResp = Send-Request -Client $rest -Method 'POST' -Url "$($base.profile)/api/v1/profiles/recruiters" -Headers $recruiterHeaders -Body @{
    fullName = 'Final Recruiter'
    email = $recruiterEmail
    mobile = $recruiterMobile
    companyName = 'HireConnect Labs'
    companySize = '51-200'
    industry = 'Software'
    website = 'https://hireconnect.example.com'
    addresses = @(@{ houseNo='8'; street='Cyber City'; city='Gurgaon'; state='Haryana'; pincode=122002 })
}
$recruiterProfile = Parse-Json $recruiterProfileResp.Body
Check ($recruiterProfileResp.Status -eq 201 -and $recruiterProfile.role -eq 'RECRUITER') 'profile:create-recruiter' $recruiterProfileResp.Body

$candidateProfileId = [int]$candidateProfile.profileId
$recruiterProfileId = [int]$recruiterProfile.profileId

$profileById = Parse-Json (Send-Request -Client $rest -Method 'GET' -Url "$($base.profile)/api/v1/profiles/$candidateProfileId" -Headers $candidateHeaders).Body
Check ($profileById.email -eq $candidateEmail) 'profile:get-by-id' ($profileById | ConvertTo-Json -Depth 6)

$profileByEmail = Parse-Json (Send-Request -Client $rest -Method 'GET' -Url "$($base.profile)/api/v1/profiles/email/$candidateEmail" -Headers $candidateHeaders).Body
Check ($profileByEmail.profileId -eq $candidateProfileId) 'profile:get-by-email' ($profileByEmail | ConvertTo-Json -Depth 6)

$profileByMobile = Parse-Json (Send-Request -Client $rest -Method 'GET' -Url "$($base.profile)/api/v1/profiles/mobile/$candidateMobile" -Headers $candidateHeaders).Body
Check ($profileByMobile.profileId -eq $candidateProfileId) 'profile:get-by-mobile' ($profileByMobile | ConvertTo-Json -Depth 6)

$recruitersByRole = Parse-Json (Send-Request -Client $rest -Method 'GET' -Url "$($base.profile)/api/v1/profiles/role/RECRUITER" -Headers $recruiterHeaders).Body
Check (($recruitersByRole | Measure-Object).Count -ge 1) 'profile:get-by-role' 'expected recruiter list'

$updatedProfileResp = Send-Request -Client $rest -Method 'PUT' -Url "$($base.profile)/api/v1/profiles/$candidateProfileId" -Headers $candidateHeaders -Body @{ fullName='Final Candidate Updated'; experience=4; skills=@('Java','Spring Boot','AWS') }
$updatedProfile = Parse-Json $updatedProfileResp.Body
Check ($updatedProfileResp.Status -eq 200 -and $updatedProfile.experience -eq 4) 'profile:update' $updatedProfileResp.Body

$tempCandidateRegister = Parse-Json (Send-Request -Client $rest -Method 'POST' -Url "$($base.auth)/auth/register" -Body @{ email=$tempCandidateEmail; password=$candidatePassword; role='CANDIDATE' }).Body
$tempCandidateHeaders = @{ Authorization = "Bearer $($tempCandidateRegister.accessToken)" }
$tempCandidateProfileResp = Send-Request -Client $rest -Method 'POST' -Url "$($base.profile)/api/v1/profiles/candidates" -Headers $tempCandidateHeaders -Body @{
    fullName = 'Temp Candidate'
    email = $tempCandidateEmail
    mobile = $tempCandidateMobile
    dob = '2000-01-01'
    gender = 'FEMALE'
    skills = @('Testing')
    experience = 1
    resumeUrl = 'https://files.example.com/resume/temp-candidate.pdf'
    addresses = @(@{ houseNo='1'; street='Test Street'; city='Pune'; state='Maharashtra'; pincode=411002 })
}
$tempCandidateProfile = Parse-Json $tempCandidateProfileResp.Body
$tempDelete = Send-Request -Client $rest -Method 'DELETE' -Url "$($base.profile)/api/v1/profiles/$($tempCandidateProfile.profileId)" -Headers $tempCandidateHeaders
Check ($tempDelete.Status -eq 204) 'profile:delete' $tempDelete.Body

$jobResp = Send-Request -Client $rest -Method 'POST' -Url "$($base.job)/api/v1/jobs" -Headers $recruiterHeaders -Body @{
    title = "Backend Engineer $stamp"
    category = 'Engineering'
    type = 'Full-time'
    location = 'Pune'
    salaryMin = 1200000
    salaryMax = 1800000
    description = 'Build backend services'
    skills = @('Java','Spring Boot','MySQL')
    experienceRequired = 3
    postedBy = $recruiterProfileId
    status = 'OPEN'
    postedAt = (Get-Date).ToString('yyyy-MM-dd')
}
$job = Parse-Json $jobResp.Body
Check ($jobResp.Status -eq 201 -and $job.postedBy -eq $recruiterProfileId) 'job:create' $jobResp.Body
$jobId = [int]$job.jobId

foreach ($entry in @(
    @{ name='all'; url="$($base.job)/api/v1/jobs"; expect='"jobId"' },
    @{ name='id'; url="$($base.job)/api/v1/jobs/$jobId"; expect='Backend Engineer' },
    @{ name='title'; url="$($base.job)/api/v1/jobs/title/Backend%20Engineer%20$stamp"; expect='Backend Engineer' },
    @{ name='category'; url="$($base.job)/api/v1/jobs/category/Engineering"; expect='Backend Engineer' },
    @{ name='location'; url="$($base.job)/api/v1/jobs/location/Pune"; expect='Backend Engineer' },
    @{ name='status'; url="$($base.job)/api/v1/jobs/status/OPEN"; expect='Backend Engineer' },
    @{ name='recruiter'; url="$($base.job)/api/v1/jobs/recruiter/$recruiterProfileId"; expect='Backend Engineer' },
    @{ name='search'; url="$($base.job)/api/v1/jobs?category=Engineering&location=Pune&status=OPEN&postedBy=$recruiterProfileId"; expect='Backend Engineer' }
)) {
    $resp = Send-Request -Client $rest -Method 'GET' -Url $entry.url -Headers $candidateHeaders
    Check ($resp.Status -eq 200 -and $resp.Body -match [regex]::Escape($entry.expect)) "job:$($entry.name)" $resp.Body
}

$jobUpdateResp = Send-Request -Client $rest -Method 'PUT' -Url "$($base.job)/api/v1/jobs/$jobId" -Headers $recruiterHeaders -Body @{ location='Bengaluru'; salaryMax=1900000 }
$jobUpdate = Parse-Json $jobUpdateResp.Body
Check ($jobUpdate.location -eq 'Bengaluru' -and $jobUpdate.salaryMax -eq 1900000) 'job:update' $jobUpdateResp.Body

$job2Resp = Send-Request -Client $rest -Method 'POST' -Url "$($base.job)/api/v1/jobs" -Headers $recruiterHeaders -Body @{
    title = "QA Engineer $stamp"
    category = 'Quality'
    type = 'Full-time'
    location = 'Mumbai'
    salaryMin = 800000
    salaryMax = 1100000
    description = 'Test platform quality'
    skills = @('Testing','Selenium')
    experienceRequired = 2
    postedBy = $recruiterProfileId
    status = 'OPEN'
    postedAt = (Get-Date).ToString('yyyy-MM-dd')
}
$job2 = Parse-Json $job2Resp.Body
$job2Id = [int]$job2.jobId
Check ($job2Resp.Status -eq 201) 'job:create-secondary' $job2Resp.Body

$view1 = Send-Request -Client $rest -Method 'POST' -Url "$($base.notification)/api/v1/analytics/jobs/$jobId/views" -Headers $candidateHeaders -Body @{}
$view2 = Send-Request -Client $rest -Method 'POST' -Url "$($base.notification)/api/v1/analytics/jobs/$jobId/views" -Headers $candidateHeaders -Body @{}
Check ($view1.Status -eq 200 -and $view2.Status -eq 200) 'analytics:record-job-views' 'two views recorded'

$appResp = Send-Request -Client $rest -Method 'POST' -Url "$($base.application)/api/v1/applications" -Headers $candidateHeaders -Body @{
    jobId = $jobId
    candidateId = $candidateProfileId
    coverLetter = 'Interested in this role'
    resumeUrl = 'https://files.example.com/resume/final-candidate.pdf'
}
$app = Parse-Json $appResp.Body
Check ($appResp.Status -eq 201 -and $app.status -eq 'APPLIED') 'application:create' $appResp.Body
$applicationId = [int]$app.applicationId

$app2Resp = Send-Request -Client $rest -Method 'POST' -Url "$($base.application)/api/v1/applications" -Headers $candidateHeaders -Body @{
    jobId = $job2Id
    candidateId = $candidateProfileId
    coverLetter = 'Second application'
    resumeUrl = 'https://files.example.com/resume/final-candidate.pdf'
}
$app2 = Parse-Json $app2Resp.Body
Check ($app2Resp.Status -eq 201) 'application:create-secondary' $app2Resp.Body
$app2WithdrawResp = Send-Request -Client $rest -Method 'PATCH' -Url "$($base.application)/api/v1/applications/$($app2.applicationId)/withdraw" -Headers $candidateHeaders
$app2Withdraw = Parse-Json $app2WithdrawResp.Body
Check ($app2WithdrawResp.Status -eq 200 -and $app2Withdraw.status -eq 'WITHDRAWN') 'application:withdraw' $app2WithdrawResp.Body

foreach ($entry in @(
    @{ name='id'; url="$($base.application)/api/v1/applications/$applicationId"; expect='APPLIED' },
    @{ name='candidate'; url="$($base.application)/api/v1/applications/candidate/$candidateProfileId"; expect='APPLIED' },
    @{ name='job'; url="$($base.application)/api/v1/applications/job/$jobId"; expect='APPLIED' },
    @{ name='count'; url="$($base.application)/api/v1/applications/job/$jobId/count"; expect='1' },
    @{ name='status-filter'; url="$($base.application)/api/v1/applications?status=APPLIED"; expect='APPLIED' },
    @{ name='date-range'; url="$($base.application)/api/v1/applications?appliedFrom=$((Get-Date).ToString('yyyy-MM-dd'))&appliedTo=$((Get-Date).ToString('yyyy-MM-dd'))"; expect='APPLIED' }
)) {
    $resp = Send-Request -Client $rest -Method 'GET' -Url $entry.url -Headers $recruiterHeaders
    Check ($resp.Status -eq 200 -and $resp.Body -match [regex]::Escape($entry.expect)) "application:$($entry.name)" $resp.Body
}

$shortlistResp = Send-Request -Client $rest -Method 'PATCH' -Url "$($base.application)/api/v1/applications/$applicationId/status" -Headers $recruiterHeaders -Body @{ status = 'SHORTLISTED' }
$shortlist = Parse-Json $shortlistResp.Body
Check ($shortlistResp.Status -eq 200 -and $shortlist.status -eq 'SHORTLISTED') 'application:shortlist' $shortlistResp.Body

$scheduledAt = (Get-Date).AddDays(3).ToString('yyyy-MM-ddTHH:mm:ss')
$interviewResp = Send-Request -Client $rest -Method 'POST' -Url "$($base.interview)/api/v1/interviews" -Headers $recruiterHeaders -Body @{
    applicationId = $applicationId
    scheduledAt = $scheduledAt
    mode = 'ONLINE'
    meetLink = 'https://meet.example.com/interview-final'
    location = ''
    notes = 'Round 1'
}
$interview = Parse-Json $interviewResp.Body
Check ($interviewResp.Status -eq 201 -and $interview.status -eq 'SCHEDULED') 'interview:create' $interviewResp.Body
$interviewId = [int]$interview.interviewId

Start-Sleep -Seconds 4

$interviewById = Parse-Json (Send-Request -Client $rest -Method 'GET' -Url "$($base.interview)/api/v1/interviews/$interviewId" -Headers $candidateHeaders).Body
Check ($interviewById.interviewId -eq $interviewId) 'interview:get-by-id' ($interviewById | ConvertTo-Json -Depth 6)

$interviewByAppResp = Send-Request -Client $rest -Method 'GET' -Url "$($base.interview)/api/v1/interviews/application/$applicationId" -Headers $candidateHeaders
Check ($interviewByAppResp.Status -eq 200 -and $interviewByAppResp.Body -match [regex]::Escape('SCHEDULED')) 'interview:get-by-application' $interviewByAppResp.Body

$rangeFrom = (Get-Date).AddDays(2).ToString('yyyy-MM-ddTHH:mm:ss')
$rangeTo = (Get-Date).AddDays(4).ToString('yyyy-MM-ddTHH:mm:ss')
$interviewRangeResp = Send-Request -Client $rest -Method 'GET' -Url "$($base.interview)/api/v1/interviews?scheduledFrom=$rangeFrom&scheduledTo=$rangeTo" -Headers $recruiterHeaders
Check ($interviewRangeResp.Status -eq 200 -and $interviewRangeResp.Body -match [regex]::Escape([string]$interviewId)) 'interview:get-by-range' $interviewRangeResp.Body

$confirmResp = Send-Request -Client $rest -Method 'PATCH' -Url "$($base.interview)/api/v1/interviews/$interviewId/confirm" -Headers $candidateHeaders
Check ($confirmResp.Status -eq 200 -and $confirmResp.Body -match 'confirmed') 'interview:confirm' $confirmResp.Body

$newScheduledAt = (Get-Date).AddDays(4).ToString('yyyy-MM-ddTHH:mm:ss')
$rescheduleResp = Send-Request -Client $rest -Method 'PATCH' -Url "$($base.interview)/api/v1/interviews/$interviewId/reschedule" -Headers $candidateHeaders -Body @{
    scheduledAt = $newScheduledAt
    meetLink = 'https://meet.example.com/interview-final-2'
    location = ''
    notes = 'Need a later slot'
}
$rescheduled = Parse-Json $rescheduleResp.Body
Check ($rescheduleResp.Status -eq 200 -and $rescheduled.status -eq 'RESCHEDULE_REQUESTED') 'interview:reschedule' $rescheduleResp.Body

$statusResp = Send-Request -Client $rest -Method 'GET' -Url "$($base.interview)/api/v1/interviews/status/RESCHEDULE_REQUESTED" -Headers $recruiterHeaders
Check ($statusResp.Status -eq 200 -and $statusResp.Body -match [regex]::Escape([string]$interviewId)) 'interview:get-by-status' $statusResp.Body

$cancelResp = Send-Request -Client $rest -Method 'DELETE' -Url "$($base.interview)/api/v1/interviews/$interviewId" -Headers $recruiterHeaders
Check ($cancelResp.Status -eq 204) 'interview:cancel' $cancelResp.Body

Start-Sleep -Seconds 6

$manualEventResp = Send-Request -Client $rest -Method 'POST' -Url "$($base.notification)/api/v1/notifications/events" -Headers $adminHeaders -Body @{
    eventType = 'MANUAL_ALERT'
    notificationType = 'SYSTEM'
    message = 'Backend final verification alert'
    recipientUserIds = @($candidateProfileId)
    recipientEmails = @($candidateEmail)
    occurredAt = (Get-Date).ToString('yyyy-MM-ddTHH:mm:ss')
}
Check ($manualEventResp.Status -eq 202) 'notification:manual-event' $manualEventResp.Body

Start-Sleep -Seconds 4

$candidateNotificationsResp = Send-Request -Client $rest -Method 'GET' -Url "$($base.notification)/api/v1/notifications/user/$candidateProfileId" -Headers $candidateHeaders
$candidateNotifications = Parse-Json $candidateNotificationsResp.Body
Check ($candidateNotificationsResp.Status -eq 200 -and ($candidateNotifications | Measure-Object).Count -ge 1) 'notification:get-by-user' $candidateNotificationsResp.Body

$recruiterNotificationsResp = Send-Request -Client $rest -Method 'GET' -Url "$($base.notification)/api/v1/notifications/user/$recruiterProfileId" -Headers $recruiterHeaders
$recruiterNotifications = Parse-Json $recruiterNotificationsResp.Body
Check ($recruiterNotificationsResp.Status -eq 200 -and ($recruiterNotifications | Measure-Object).Count -ge 1) 'notification:get-by-user-recruiter' $recruiterNotificationsResp.Body

$candidateUnreadResp = Send-Request -Client $rest -Method 'GET' -Url "$($base.notification)/api/v1/notifications/user/$candidateProfileId/unread-count" -Headers $candidateHeaders
$candidateUnread = [int]$candidateUnreadResp.Body
Check ($candidateUnreadResp.Status -eq 200 -and $candidateUnread -ge 1) 'notification:unread-count' $candidateUnreadResp.Body

$firstCandidateNotificationId = [int]$candidateNotifications[0].notificationId
$markReadResp = Send-Request -Client $rest -Method 'PATCH' -Url "$($base.notification)/api/v1/notifications/$firstCandidateNotificationId/read" -Headers $candidateHeaders
Check ($markReadResp.Status -eq 204) 'notification:mark-read' $markReadResp.Body

$markAllRecruiterResp = Send-Request -Client $rest -Method 'PATCH' -Url "$($base.notification)/api/v1/notifications/user/$recruiterProfileId/read-all" -Headers $recruiterHeaders
Check ($markAllRecruiterResp.Status -eq 204) 'notification:mark-all-read' $markAllRecruiterResp.Body

$recruiterUnreadAfterResp = Send-Request -Client $rest -Method 'GET' -Url "$($base.notification)/api/v1/notifications/user/$recruiterProfileId/unread-count" -Headers $recruiterHeaders
Check ($recruiterUnreadAfterResp.Status -eq 200 -and [int]$recruiterUnreadAfterResp.Body -eq 0) 'notification:unread-zero-after-mark-all' $recruiterUnreadAfterResp.Body

$deleteNotificationResp = Send-Request -Client $rest -Method 'DELETE' -Url "$($base.notification)/api/v1/notifications/$firstCandidateNotificationId" -Headers $candidateHeaders
Check ($deleteNotificationResp.Status -eq 204) 'notification:delete' $deleteNotificationResp.Body

$jobViewCountResp = Send-Request -Client $rest -Method 'GET' -Url "$($base.notification)/api/v1/analytics/jobs/$jobId/view-count" -Headers $candidateHeaders
Check ($jobViewCountResp.Status -eq 200 -and [int]$jobViewCountResp.Body -ge 2) 'analytics:view-count' $jobViewCountResp.Body

$appCountResp = Send-Request -Client $rest -Method 'GET' -Url "$($base.notification)/api/v1/analytics/jobs/$jobId/application-count" -Headers $recruiterHeaders
Check ($appCountResp.Status -eq 200 -and [int]$appCountResp.Body -eq 1) 'analytics:application-count' $appCountResp.Body

$ratioResp = Send-Request -Client $rest -Method 'GET' -Url "$($base.notification)/api/v1/analytics/jobs/$jobId/view-to-apply-ratio" -Headers $candidateHeaders
Check ($ratioResp.Status -eq 200 -and [double]$ratioResp.Body -ge 1.0) 'analytics:view-to-apply-ratio' $ratioResp.Body

$recruiterStatsResp = Send-Request -Client $rest -Method 'GET' -Url "$($base.notification)/api/v1/analytics/recruiter/$recruiterProfileId" -Headers $recruiterHeaders
$recruiterStats = Parse-Json $recruiterStatsResp.Body
Check ($recruiterStatsResp.Status -eq 200 -and $recruiterStats.totalApplications -ge 1) 'analytics:recruiter-stats' $recruiterStatsResp.Body

$timeToHireResp = Send-Request -Client $rest -Method 'GET' -Url "$($base.notification)/api/v1/analytics/recruiter/$recruiterProfileId/time-to-hire" -Headers $recruiterHeaders
Check ($timeToHireResp.Status -eq 200) 'analytics:time-to-hire' $timeToHireResp.Body

$platformStatsResp = Send-Request -Client $rest -Method 'GET' -Url "$($base.notification)/api/v1/analytics/admin" -Headers $adminHeaders
$platformStats = Parse-Json $platformStatsResp.Body
Check ($platformStatsResp.Status -eq 200 -and $platformStats.totalJobs -ge 1) 'analytics:admin-platform-stats' $platformStatsResp.Body

$topCategoriesResp = Send-Request -Client $rest -Method 'GET' -Url "$($base.notification)/api/v1/analytics/categories/top" -Headers $recruiterHeaders
Check ($topCategoriesResp.Status -eq 200 -and $topCategoriesResp.Body -match 'Engineering') 'analytics:top-categories' $topCategoriesResp.Body

$gatewayChecks = @(
    @{ name='auth-me'; url="$($base.gateway)/api/auth/me"; headers=$candidateHeaders; expect=$candidateEmail },
    @{ name='profile'; url="$($base.gateway)/api/v1/profiles/$candidateProfileId"; headers=$candidateHeaders; expect=$candidateEmail },
    @{ name='job'; url="$($base.gateway)/api/v1/jobs/$jobId"; headers=$candidateHeaders; expect='Backend Engineer' },
    @{ name='job-legacy'; url="$($base.gateway)/api/jobs/$jobId"; headers=$candidateHeaders; expect='Backend Engineer' },
    @{ name='application'; url="$($base.gateway)/api/v1/applications/$applicationId"; headers=$candidateHeaders; expect='INTERVIEW_SCHEDULED' },
    @{ name='interview'; url="$($base.gateway)/api/v1/interviews/$interviewId"; headers=$candidateHeaders; expect='CANCELED' },
    @{ name='notification'; url="$($base.gateway)/api/v1/notifications/user/$candidateProfileId/unread-count"; headers=$candidateHeaders; expect='' },
    @{ name='analytics'; url="$($base.gateway)/api/v1/analytics/recruiter/$recruiterProfileId"; headers=$recruiterHeaders; expect='totalJobs' },
    @{ name='web-login'; url="$($base.gateway)/web/login"; headers=@{}; expect='Login' }
)
foreach ($entry in $gatewayChecks) {
    $resp = Send-Request -Client $rest -Method 'GET' -Url $entry.url -Headers $entry.headers
    $condition = $resp.Status -eq 200
    if ($entry.expect) { $condition = $condition -and ($resp.Body -match [regex]::Escape($entry.expect)) }
    Check $condition "gateway:$($entry.name)" $resp.Body
}

$loginPage = Send-Request -Client $candidateWeb -Method 'GET' -Url "$($base.web)/login"
Check ($loginPage.Status -eq 200 -and $loginPage.Body -match 'Login') 'web:candidate-login-page' $loginPage.Body

$candidateWebLogin = Send-Request -Client $candidateWeb -Method 'POST' -Url "$($base.web)/login" -ContentType 'application/x-www-form-urlencoded' -Body @{ email=$candidateEmail; password=$candidatePassword }
Check ($candidateWebLogin.Status -eq 200 -and $candidateWebLogin.Body -match 'Candidate Profile') 'web:candidate-login' $candidateWebLogin.Body

foreach ($entry in @(
    @{ name='profile'; url="$($base.web)/candidate/profile"; expect='Candidate Profile' },
    @{ name='jobs'; url="$($base.web)/candidate/jobs"; expect='Search Jobs' },
    @{ name='applications'; url="$($base.web)/candidate/applications"; expect='Your Applications' },
    @{ name='interviews'; url="$($base.web)/candidate/interviews"; expect='Your Interviews' },
    @{ name='notifications'; url="$($base.web)/candidate/notifications"; expect='Your Notifications' }
)) {
    $resp = Send-Request -Client $candidateWeb -Method 'GET' -Url $entry.url
    Check ($resp.Status -eq 200 -and $resp.Body -match $entry.expect) "web:candidate-$($entry.name)" $resp.Body
}

$bookmarkWeb = Send-Request -Client $candidateWeb -Method 'POST' -Url "$($base.web)/candidate/jobs/$jobId/bookmark"
Check ($bookmarkWeb.Status -eq 200 -and $bookmarkWeb.Body -match 'Search Jobs') 'web:candidate-bookmark' $bookmarkWeb.Body

$walletWeb = Send-Request -Client $candidateWeb -Method 'POST' -Url "$($base.web)/candidate/wallet" -ContentType 'application/x-www-form-urlencoded' -Body @{ amount='500.00' }
Check ($walletWeb.Status -eq 200 -and $walletWeb.Body -match 'Search Jobs') 'web:candidate-wallet' $walletWeb.Body

$recruiterLoginPage = Send-Request -Client $recruiterWeb -Method 'GET' -Url "$($base.web)/login"
Check ($recruiterLoginPage.Status -eq 200) 'web:recruiter-login-page' $recruiterLoginPage.Body
$recruiterWebLogin = Send-Request -Client $recruiterWeb -Method 'POST' -Url "$($base.web)/login" -ContentType 'application/x-www-form-urlencoded' -Body @{ email=$recruiterEmail; password=$recruiterPassword }
Check ($recruiterWebLogin.Status -eq 200 -and $recruiterWebLogin.Body -match 'Recruiter Dashboard') 'web:recruiter-login' $recruiterWebLogin.Body

foreach ($entry in @(
    @{ name='dashboard'; url="$($base.web)/recruiter/dashboard"; expect='Recruiter Dashboard' },
    @{ name='applications'; url="$($base.web)/recruiter/jobs/$jobId/applications"; expect='Applications for' },
    @{ name='analytics'; url="$($base.web)/recruiter/analytics"; expect='Recruiter Analytics' }
)) {
    $resp = Send-Request -Client $recruiterWeb -Method 'GET' -Url $entry.url
    Check ($resp.Status -eq 200 -and $resp.Body -match $entry.expect) "web:recruiter-$($entry.name)" $resp.Body
}

$adminLoginPage = Send-Request -Client $adminWeb -Method 'GET' -Url "$($base.web)/login"
Check ($adminLoginPage.Status -eq 200) 'web:admin-login-page' $adminLoginPage.Body
$adminWebLogin = Send-Request -Client $adminWeb -Method 'POST' -Url "$($base.web)/login" -ContentType 'application/x-www-form-urlencoded' -Body @{ email=$adminEmail; password=$adminPassword }
Check ($adminWebLogin.Status -eq 200 -and $adminWebLogin.Body -match 'Admin Dashboard') 'web:admin-login' $adminWebLogin.Body

foreach ($entry in @(
    @{ name='dashboard'; url="$($base.web)/admin/dashboard"; expect='Admin Dashboard' },
    @{ name='users'; url="$($base.web)/admin/users"; expect='Manage Users' },
    @{ name='jobs'; url="$($base.web)/admin/jobs"; expect='All Jobs' },
    @{ name='analytics'; url="$($base.web)/admin/analytics"; expect='Platform Analytics' }
)) {
    $resp = Send-Request -Client $adminWeb -Method 'GET' -Url $entry.url
    Check ($resp.Status -eq 200 -and $resp.Body -match $entry.expect) "web:admin-$($entry.name)" $resp.Body
}

$suspendResp = Send-Request -Client $adminWeb -Method 'POST' -Url "$($base.web)/admin/users/$candidateProfileId/suspend" -ContentType 'application/x-www-form-urlencoded' -Body @{ reason='Final verification' }
Check ($suspendResp.Status -eq 200 -and $suspendResp.Body -match 'Manage Users' -and $suspendResp.Body -match 'Final verification') 'web:admin-suspend-user' $suspendResp.Body

$exportResp = Send-Request -Client $adminWeb -Method 'GET' -Url "$($base.web)/admin/reports/export"
Check ($exportResp.Status -eq 200 -and $exportResp.Body -match 'metric,value') 'web:admin-export-report' $exportResp.Body

foreach ($entry in @(
    @{ name='recruiter-subscription-removed'; url="$($base.web)/recruiter/subscription" },
    @{ name='recruiter-invoices-removed'; url="$($base.web)/recruiter/invoices" },
    @{ name='admin-subscriptions-removed'; url="$($base.web)/admin/subscriptions" },
    @{ name='admin-invoices-removed'; url="$($base.web)/admin/invoices" }
)) {
    $resp = Send-Request -Client $rest -Method 'GET' -Url $entry.url
    Check ($resp.Status -eq 404) "web:$($entry.name)" $resp.Body
}

$deleteJobResp = Send-Request -Client $rest -Method 'DELETE' -Url "$($base.job)/api/v1/jobs/$job2Id" -Headers $recruiterHeaders
Check ($deleteJobResp.Status -eq 204) 'job:delete-secondary' $deleteJobResp.Body

$logoutResp = Send-Request -Client $rest -Method 'POST' -Url "$($base.auth)/auth/logout" -Headers $candidateHeaders
Check ($logoutResp.Status -eq 200) 'auth:logout' $logoutResp.Body
$logoutValidate = Parse-Json (Send-Request -Client $rest -Method 'POST' -Url "$($base.auth)/auth/validate" -Body @{ token = $candidateLogin.accessToken }).Body
$logoutInvalidated = $logoutValidate.valid -eq $false
$checks.Add([pscustomobject]@{ name = 'auth:logout-invalidates-token'; ok = $logoutInvalidated; detail = ($logoutValidate | ConvertTo-Json -Depth 5) }) | Out-Null

"VERIFICATION_RESULTS"
$checks | ForEach-Object { "{0}`t{1}`t{2}" -f ($(if ($_.ok) {'PASS'} else {'FAIL'}), $_.name, $_.detail) }
"SUMMARY"
"passCount=$((@($checks | Where-Object ok).Count))"
"failCount=$((@($checks | Where-Object { -not $_.ok }).Count))"
