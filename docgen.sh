#!/bin/sh


echo '''
Command                | Use
---------------------- | ---
''' > docs/commands-list.md

cat ./src/main/resources/assets/fastback/lang/en_us.json | \
jq -r 'to_entries[] |select(.key|match("commands.fastback.help*")) | ([ (["`", .key, "`"] | join("")), .value] | join(" | ")) ' \
>> docs/commands-list.md       
