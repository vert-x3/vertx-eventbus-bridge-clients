#!/bin/sh
set -e
cwd=$(cd `dirname $0` && pwd)
cd $cwd

dist=0
publish=0
apikey=""

while (( "$#" )); do
  case "$1" in
  "--dist"*)
    dist=1
    shift
    ;;
  "--publish"*)
    publish=1
    shift
    ;;
  "--apikey"*)
    apikey="$2"
    shift
    shift
    ;;
  *)
    ;;
  esac
done

echo -e "Build the project"
dotnet restore
dotnet build --no-restore -c Release

if [ $dist -eq 1 -o $publish -eq 1 ]; then
  echo -e "Package the project"
  dotnet pack --include-symbols -p:SymbolPackageFormat=snupkg --no-build -c Release vertx-eventbus.csproj -o .
fi

if [ $publish -eq 1 ]; then
  echo -e "Publish the package to Nuget"
  dotnet nuget push *.nupkg -s https://api.nuget.org/v3/index.json -k $apikey --interactive --skip-duplicate
fi
