#!/bin/sh
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

echo -e "Build and test the project"

echo -e "Run unit tests"
python -m unittest -v test/unittesting/*.py

echo -e "Run Integration tests"
python -m unittest -v test/systemtesting/test_eventbus.py

if [ $dist -eq 1 -o $publish -eq 1 ]; then
  echo -e "Package the project"
  python setup.py sdist
fi

if [ $publish -eq 1 ]; then
  echo -e "Publish the package to Pypi.org"
  twine upload dist/*
fi
