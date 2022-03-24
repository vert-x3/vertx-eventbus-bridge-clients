#!/bin/env bash
set -e
cwd=$(cd `dirname $0` && pwd)
cd $cwd

dist=0
publish=0

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
  *)
    ;;
  esac
done

echo -e "Build the project"
npm install

if [ $dist -eq 1 -o $publish -eq 1 ]; then
  echo -e "Package the project"
  npm pack
fi

if [ $publish -eq 1 ]; then
  echo -e "Publish the package to NPMJS"
  npm publish . --access public
fi
