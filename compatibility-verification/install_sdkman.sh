#!/bin/sh

#curl -s "https://get.sdkman.io" | bash

source "$HOME/.sdkman/bin/sdkman-init.sh"

for candidate in  $( cat compatibility-verification/failed.txt  | grep -v '#' |
       sed -r -e 's/.*\|(.*)/\1/' ) ; do
  echo "##########################################"
  echo "###${candidate}###"
  sdk install java $candidate </dev/null
  sdk use java $candidate </dev/null
  mvn clean
  mvn package
  D=$?
  a=$( java -version  2>&1  | head -n 1 )

  echo  $(date):$candidate:${a}:${D}  >>results.txt
done