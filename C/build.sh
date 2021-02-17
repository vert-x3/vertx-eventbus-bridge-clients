#!/bin/sh
set -e

cwd=$(cd `dirname $0` && pwd)
cd $cwd

BUILD_OPTS_PRE="--rm -v $(pwd):/workdir:Z"
BUILD_OPTS_SUF="-it"
dist=0
goal="make"

while (( "$#" )); do
  case "$1" in
  "--notty"*)
    BUILD_OPTS_SUF="-i"
    ;;
  "--dist"*)
    dist=1
    ;;
  "--clean"*)
    goal="make clean"
    rm -rf $cwd/dist/
    ;;
  *)
    ;;
  esac
  shift
done

BUILD_OPTS_SUF="${BUILD_OPTS_SUF} multiarch/crossbuild"
for triple in x86_64-linux-gnu i686-w64-mingw32 x86_64-w64-mingw32 x86_64-apple-darwin; do
  echo -e "Build target triple: ${triple}"
  docker run ${BUILD_OPTS_PRE} -e CROSS_TRIPLE=${triple} ${BUILD_OPTS_SUF} $goal
done

if [ $dist -eq 1 ]; then
  echo -e "Packaging dist"
  rm -rf $cwd/dist/ && mkdir -p $cwd/dist/lib
  cp VERSION dist/
  cp README.md dist/
  cp ../LICENSE dist/
  cp -r include dist/
  for distro in `ls target`; do
    mkdir -p $cwd/dist/lib/$distro
    cp target/$distro/libeventbus.a dist/lib/$distro/libeventbus.a
  done
  ver="$(cat VERSION)"
  tar -czf vertx-eventbus-$ver.tar.gz dist/
fi

echo -e "Build Completed !!"