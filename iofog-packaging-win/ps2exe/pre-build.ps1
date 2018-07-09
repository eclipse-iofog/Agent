echo "Downloading Docker.."

$url = "https://download.docker.com/win/stable/DockerToolbox.exe"
$outPath = "DockerToolbox.exe"

$Index = $url.LastIndexOf("/")
$file = $url.Substring($Index+1)
$newurl = $url.Substring(0,$index)
#Invoke-WebRequest -Uri $url -OutFile $outPath

$web = new-object System.Net.WebClient

Register-ObjectEvent -InputObject $web -EventName DownloadFileCompleted `
-SourceIdentifier Web.DownloadFileCompleted -Action {
    $Global:isDownloaded = $True
}

Register-ObjectEvent -InputObject $web -EventName DownloadProgressChanged `
-SourceIdentifier Web.DownloadProgressChanged -Action {
    $Global:Data = $event
}

$web.DownloadFileAsync($url, $outPath)

While (-Not $isDownloaded) {
    $percent = $Global:Data.SourceArgs.ProgressPercentage
    $totalBytes = $Global:Data.SourceArgs.TotalBytesToReceive
    $receivedBytes = $Global:Data.SourceArgs.BytesReceived
    If ($percent -ne $null) {
        Write-Progress -Activity ("Downloading {0} from {1}" -f $file, $newUrl) `
        -Status ("{0} bytes \ {1} bytes" -f $receivedBytes, $totalBytes)  -PercentComplete $percent
    }
}
Write-Progress -Activity ("Downloading {0} from {1}" -f $file, $newUrl) `
-Status ("{0} bytes \ {1} bytes" -f $receivedBytes, $totalBytes)  -Completed



Start-Process -Filepath $outPath