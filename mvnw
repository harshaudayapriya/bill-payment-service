#!/bin/sh
# Maven Wrapper script - downloads Maven if not found, then runs it
# Based on Apache Maven Wrapper 3.3.2

set -e

if [ -z "$JAVA_HOME" ]; then
  JAVACMD=java
else
  JAVACMD="$JAVA_HOME/bin/java"
fi

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(cd "$(dirname "$0")" && pwd)}"
MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"
MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-3.9.9"
MAVEN_CMD="$MAVEN_HOME/bin/mvn"

if [ ! -f "$MAVEN_CMD" ]; then
  echo "Downloading Apache Maven 3.9.9..."
  mkdir -p "$MAVEN_HOME"

  TMPFILE=$(mktemp /tmp/maven-XXXXXX.zip)

  if command -v curl > /dev/null 2>&1; then
    curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip" -o "$TMPFILE"
  elif command -v wget > /dev/null 2>&1; then
    wget -q "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip" -O "$TMPFILE"
  else
    echo "Error: curl or wget is required to download Maven" >&2
    exit 1
  fi

  unzip -q -o "$TMPFILE" -d "$HOME/.m2/wrapper/dists/"
  mv "$HOME/.m2/wrapper/dists/apache-maven-3.9.9" "$HOME/.m2/wrapper/dists/apache-maven-3.9.9-tmp" 2>/dev/null || true
  mv "$HOME/.m2/wrapper/dists/apache-maven-3.9.9-tmp" "$MAVEN_HOME" 2>/dev/null || true

  # Handle case where unzip creates a nested directory
  if [ ! -f "$MAVEN_CMD" ] && [ -d "$HOME/.m2/wrapper/dists/apache-maven-3.9.9/apache-maven-3.9.9" ]; then
    cp -r "$HOME/.m2/wrapper/dists/apache-maven-3.9.9/apache-maven-3.9.9/"* "$MAVEN_HOME/"
  fi

  rm -f "$TMPFILE"
  chmod +x "$MAVEN_CMD" 2>/dev/null || true
  echo "Maven 3.9.9 installed to $MAVEN_HOME"
fi

exec "$MAVEN_CMD" "$@"

