$iofogPath = Get-Location | select -ExpandProperty Path

if (!$iofogPath)
{
    echo "Checking if VirtualBox, Docker Toolbox & Java are installed.."
    $iofogExePath = @(where.exe /R c:\\ "iofog.exe")[0]

    $iofogPath = Split-Path -Path $iofogExePath
}

# install Docker Toolbox if needed
try
{
    docker-machine version
}
catch
{
    echo "Installing Docker Toolbox.."
    Start-Process ($iofogPath + "\DockerToolbox.exe") -Wait
}

# install VirtualBox if needed
$VMInstalled = (Get-ItemProperty HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\* |  where-object {$_.DisplayName -ne $null} | select -ExpandProperty DisplayName)
$VBinstall = "Oracle VM VirtualBox 5.2.8"
if (!$VMInstalled.contains($VBinstall)) {
    echo "Installing VirtualBox.."
    echo $iofogPath
    Start-Process ($iofogPath + "\VirtualBox-5.2.8.exe") -Wait
}

# install Java if needed
try
{
    java -version
}
catch
{
    echo "Installing Java.."
    Start-Process ($iofogPath + "\JavaSetup8u171.exe") -Wait
}

#Read-Host "VM installed."