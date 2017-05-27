#!/usr/bin/env bash

set -e

if [ -z ${PROJECT+x} ]; then
    echo "Set PROJECT to deploy."
    exit 1
fi
   
export ROOT=`git rev-parse --show-toplevel`

cd ${ROOT?}
lein clean
lein cljsbuild once min

cd ${ROOT?}/gae
gcloud --quiet --project ${PROJECT?} app deploy
