param (
    [string]$arg1
)

$SERVICE_NAME="iofogd"

$JAR_FILE_NAME= $ENV:IOFOG_PATH + "iofogd.jar"
echo $JAR_FILE_NAME

cd $ENV:IOFOG_PATH
if ($arg1)
{
        java -jar "iofogd.jar" $arg1
}
else
{
    # start if called like "iofog_service" with no params
    java -jar "iofogd.jar" start
}
#if ($start) {
#	echo "Starting iofog service..."
#    echo $JAR_FILE_NAME
#    Start-Process java -ArgumentList '-jar', '$JAR_FILE_NAME', 'start'
#}
#
#if ($stop) {
#    Start-Process java -ArgumentList '-jar', '$JAR_FILE_NAME', 'stop'
#}

#while($true) {
#    echo "test"
#    Start-Sleep –Seconds 1
#}