#!/usr/bin/env bash
set -eu

cd "$(dirname "$0")/.."

if [ ! -x node_modules/.bin/appium ]; then
    echo "The pinned local Appium CLI is missing. Run npm ci from $PWD." >&2
    exit 1
fi

unset APPIUM_HOME

port="${PORT:-4723}"
echo "Starting the repository-pinned Appium server at http://127.0.0.1:$port"
exec npx --no-install appium --address 127.0.0.1 --port "$port"
