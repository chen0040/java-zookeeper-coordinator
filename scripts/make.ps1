
$profile="local"

$currentPath = $pwd

cd $PSScriptRoot/..

if($args.length -gt 0) {
    $profile=$args[0]
}

Invoke-Expression -Command:"mvn -f pom.xml clean package -P$profile -U"

$proj="java-zookeeper-coordinator"
$source=$PSScriptRoot + "/../target/" + $proj + ".jar"
$dest=$PSScriptRoot + "/../bin/" + $proj + ".jar"
copy $source $dest

cd $currentPath
