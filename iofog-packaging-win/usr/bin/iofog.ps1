param (
    [string]$argument,
    [string]$argument2,
    [string]$argument3,
    [string]$argument4,
    [string]$argument5,
    [string]$argument6,
    [string]$argument7
)

$iofogPath = $ENV:IOFOG_PATH
$JAR_FILE_NAME= $iofogPath + "\iofog.jar"

# TODO move daemon start to sc.exe service
$DAEMON_NAME= $iofogPath + "iofogd.jar"

$dockerEnv = [Environment]::GetEnvironmentVariable("DOCKER_TOOLBOX_INSTALL_PATH", "User")
$vmEnv = [Environment]::GetEnvironmentVariable("VBOX_MSI_INSTALL_PATH", "User")
$path = [Environment]::GetEnvironmentVariable("PATH", "User")
$iofogEnv = $path -like "*iofog*"

$isInitialized = $dockerEnv -and $vmEnv -and $iofogEnv
if (!$isInitialized)
{
    [Environment]::SetEnvironmentVariable("PATH", "$ENV:PATH;$iofogPath", "User")
    $ENV:PATH = "$ENV:PATH;$iofogPath"

    echo "Configuring Docker.."
    $dockerExePath = @(where.exe /R c:\\ "docker-machine.exe")[0]
    $dockerPath = Split-Path -Path $dockerExePath
    [Environment]::SetEnvironmentVariable("DOCKER_TOOLBOX_INSTALL_PATH", "$dockerPath", "User")
    $ENV:DOCKER_TOOLBOX_INSTALL_PATH = $dockerPath
    echo $dockerPath

    [Environment]::SetEnvironmentVariable("PATH", "$ENV:PATH;$dockerPath", "User")
    $ENV:PATH = "$ENV:PATH;$dockerPath"

    $vboxExePath = @(where.exe "VBoxManage.exe")[0]
    if (!$vboxExePath)
    {
        echo "Configuring VirtualBox.."
        $vboxExePath = @(where.exe /R c:\\ "VBoxManage.exe")[0]
    }

    $vboxPath = Split-Path -Path $vboxExePath
    $vboxPath = $vboxPath += "\"
    [Environment]::SetEnvironmentVariable("VBOX_MSI_INSTALL_PATH", "$vboxPath", "User")
    $ENV:VBOX_MSI_INSTALL_PATH = $vboxPath

    echo $vboxPath
    echo "Configured Virtual Box."

    $bashExePath = @(where.exe "bash.exe")[0]
    if (!$bashExePath)
    {
        echo "Configuring bash PATH.."
        $bashExePath = @(where.exe /R c:\\ "bash.exe")[0]
    }

    $bashPath = Split-Path -Path $bashExePath
    [Environment]::SetEnvironmentVariable("PATH", "$ENV:PATH;$bashExePath", "User")
    $ENV:PATH = "$ENV:PATH;$bashExePath"

    echo $bashPath
    echo "Added Bash to PATH."

    $iofogRootPath = Split-Path -Path $iofogPath
    $con = Get-Content "$iofogPath\config.xml"
    $con | % { $_.Replace("%IOFOG_PATH", $iofogRootPath) } | Set-Content "$iofogPath\config.xml"

    Start-Process -WindowStyle Hidden iofog_service.exe


    $login = Read-Host "Please enter Oro login"
    $password = Read-Host "Please enter password" # TODO PASSWORD      -assecurestring "Please enter password: "
    #$password = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto([System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($password))

    # Wait for daemon start
    Start-Sleep -s 5

    $loginSuccess = "customerId"
    $loginResponse = @(java -jar "$iofogPath\iofog.jar" login -u $login -p $password)
    echo $loginResponse
    if ( $loginResponse[0].contains($loginSuccess))
    {
        $macAddress = @(Get-WmiObject win32_networkadapterconfiguration | select -ExpandProperty macaddress)[0]
        $customerId = ($loginResponse)[0].split()[1]
        echo $macAddress
        echo $customerId

        $customerSuccess = "token"

        $wifiDataPath = $ENV:WIFI_DATA_PATH
        if (!$wifiDataPath)
        {
            echo "Getting wifi-data application path.."

            $wifiExePath = @(where.exe /R c:\\ "scanner.bat")[0]
            $wifiDataPath = Split-Path -Path $wifiExePath
        }

        # converting path to Windows format for volumemapping, e.g. /c/Users/Asus/Documents/commercialsuite/oro-wifi-parser
        $wifiPath = $wifiDataPath.substring(0,1).ToLower() + $wifiDataPath.substring(1)
        $wifiPath = "/" + $wifiPath
        $wifiPath = $wifiPath.Replace('\', '/')
        $wifiPath = $wifiPath.Replace(':', '')
        echo $wifiPath
        $customerResponse = @(java -jar "$iofogPath\iofog.jar" customer -m $macAddress -c $customerId -p $wifiPath)
        if ( $customerResponse[0].contains($customerSuccess))
        {
            $token = $customerResponse[0].split()[1]
            $uuid = $customerResponse[1].split()[1]

            echo $token
            echo $uuid

            $con = Get-Content "$iofogPath\config.xml"
            $con | % { $_.Replace("<access_token/>", "<access_token>$token</access_token>") } | Set-Content "$iofogPath\config.xml"

            $con = Get-Content "$iofogPath\config.xml"
            $con | % { $_.Replace("<instance_id/>", "<instance_id>$uuid</instance_id>") } | Set-Content "$iofogPath\config.xml"

            # restart daemon
            echo "Stopping daemon.."
            cd $iofogPath
            java -jar "$iofogPath\iofogd.jar" stop -Wait
        }
    }

    # Launch Docker Machine
    Start-Process $bashExePath -WindowStyle Hidden -ArgumentList "start.sh" -RedirectStandardOutput "machineOutput.txt"

    Start-Sleep -s 3

    echo "Launching Docker VirtualBox Machine.."

    $machineOutput = cat "machineOutput.txt"
    $latestLine = cat "machineOutput.txt" -Tail 1

    echo $machineOutput

    $machineSuccess = "Start interactive shell"
    $machineFail = "Looks like something went wrong in step"
    $machineFail2 = "You can further specify your shell with either"
    while(!$machineOutput.contains($machineSuccess) -and !$machineOutput.contains($machineFail) -and !$machineOutput.contains($machineFail2))
    {
        $newLine = cat "machineOutput.txt" -Tail 1
        if (!$newLine.equals($latestLine))
        {
            echo $newLine
            $latestLine = $newLine
        }
        Start-Sleep -s 1
        $machineOutput = cat "machineOutput.txt"
    }

    Start-Sleep -s 3

    $isMachineFailed = $machineOutput.contains($machineFail) -or $machineOutput.contains($machineFail2)
    if ($isMachineFailed)
    {
        # stop Docker machine
        taskkill /im bash.exe /f

        # restart Docker machine

        Start-Process $bashExePath -ArgumentList "start.sh" -RedirectStandardOutput "machineOutput2.txt"

        Start-Sleep -s 3

        $machineOutput = cat "machineOutput2.txt"
        $latestLine = cat "machineOutput2.txt" -Tail 1

        echo $machineOutput

        while(!$machineOutput.contains($machineSuccess) -and !$machineOutput.contains($machineFail) -and !$machineOutput.contains($machineFail2))
        {
            $newLine = cat "machineOutput2.txt" -Tail 1
            if (!$newLine.equals($latestLine))
            {
                echo $newLine
                $latestLine = $newLine
            }
            Start-Sleep -s 1
            $machineOutput = cat "machineOutput2.txt"
        }

    }

    $ENV:DOCKER_CERT_PATH = [Environment]::GetEnvironmentVariable("DOCKER_CERT_PATH", "User")
    $ENV:DOCKER_HOST = [Environment]::GetEnvironmentVariable("DOCKER_HOST", "User")
    $ENV:DOCKER_MACHINE_NAME = [Environment]::GetEnvironmentVariable("DOCKER_MACHINE_NAME", "User")
    $ENV:DOCKER_TLS_VERIFY = [Environment]::GetEnvironmentVariable("DOCKER_TLS_VERIFY", "User")

    echo $ENV:DOCKER_CERT_PATH
    echo $ENV:DOCKER_HOST
    echo $ENV:DOCKER_MACHINE_NAME
    echo $ENV:DOCKER_TLS_VERIFY

    #    rm machineOutput.txt
    #    rm machineOutput2.txt

    # restarting daemon
    echo "Restarting Daemon.."

    Start-Process -WindowStyle Hidden iofog_service.exe
    Start-Sleep -s 7
    echo "Daemon restarted."

    echo "Configuring wifi-data app.."

    $wifiExePath = @(where.exe /R c:\\ "scanner.bat")[0]
    $wifiDataPath = Split-Path -Path $wifiExePath

    echo $wifiDataPath
    cd $wifiDataPath
    Start-Process powershell -WindowStyle Hidden -ArgumentList "$wifiExePath"

    # TODO VM PORTS? if necessary

    # TODO remove non-required files later & fix shortcut self-repair mode
#    rm "$iofogPath\VirtualBox-5.2.8.exe"
#    rm "$iofogPath\JavaSetup8u171.exe"
#    rm "$iofogRootPath\post-install.exe"
#    rm "$iofogRootPath\post-uninstall.ps1"

    Read-Host "Install finished, press any key to continue."
    exit
}
else  # usual Iofog logic
{
    # Launch Docker if not running
    $dockerStatus = docker-machine status
    if ($dockerStatus -and !$dockerStatus.equals("Running")) {
        $dockerPath = $ENV:DOCKER_TOOLBOX_INSTALL_PATH
        Start-Process powershell -ArgumentList "Set-Location '$dockerPath'; ./start.sh;"
        echo "Starting Docker Machine, wait.."
        Start-Sleep -s 30
    }

    java -jar $JAR_FILE_NAME $argument $argument2 $argument3 $argument4 $argument5 $argument6 $argument7
}