#!/bin/bash

WORKSPACE=.
LUBAN_DLL=$WORKSPACE/Tools/Luban/Luban.dll
CONF_ROOT=.

dotnet $LUBAN_DLL \
    -t server \
    -c java-json \
    -d json \
    --conf $CONF_ROOT/luban.conf \
    --customTemplateDir "$WORKSPACE/customTemplates" \
    -x outputCodeDir="$WORKSPACE/../../java/com/mumu/game/luban/config" \
    -x outputDataDir="$WORKSPACE/output/json"

if [ $? -ne 0 ]; then
    echo "Luban generation failed!"
    exit 1
fi

echo "All tasks completed!"
