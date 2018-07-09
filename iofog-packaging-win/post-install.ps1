echo "Starting post-install process..."

echo "Configuring Iofog PATH.."
$iofogExePath = @(where.exe /R c:\\ "iofog.exe")[0]
$iofogPath = Split-Path -Path $iofogExePath
echo $iofogPath

cd $iofogPath
Start-Process ($iofogPath + "\vm-install.exe")

if (!([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]"Administrator"))
{
    Start-Process powershell.exe "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`"" -Verb RunAs
}

$success = "The command completed successfully."
$result = net localgroup iofog /add 2>&1
if ( $result.contains($success))
{
    echo "Added ioFog group."
}
else
{
    echo "Iofog group already exists."
}

$daemonName = "iofogd"
# TODO change c: to any drive


$iofogdExePath = $iofogPath + "\iofog_service.exe"

echo $iofogdExePath

$scSuccess = "SUCCESS"
$scResponse = sc.exe create $daemonName binpath= $iofogdExePath start= auto 2>&1
if ( $scResponse.contains($scSuccess) ) {
    reg add "HKLM\SYSTEM\CurrentControlSet\Services\iofogd\Parameters" /v Application /d "$iofogdExePath"
    echo "Daemon created."
}

# TODO start daemon
#sc.exe start $daemonName
#echo "Daemon started."

$iofogRootPath = Split-Path -Path $iofogPath
icacls $iofogRootPath /t /grant "*S-1-1-0:(OI)(CI)F" # Everyone=S-1-1-0 SID, for compatibility

#Read-Host -Prompt "Press Enter to exit"