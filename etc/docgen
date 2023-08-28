#!/bin/sh

#
# Always run this in the root of the repo
#
cd $(git rev-parse --show-toplevel)

#
# generate commands-list.md
#
echo '''
| Command                | Use |
| ---------------------- | --- |''' > docs/commands-list.md
cat ./src/main/resources/assets/fastback/lang/en_us.json | \
jq -r 'to_entries[] |select(.key|match("fastback.help.command.*")) | ([ (["| `", (.key|split(".")[3]), "`"] | join("")), .value, ""] | join("|")) ' \
>> docs/commands-list.md


#
# generate permissions-list.md
#
echo '''
* `fastback.command`''' > docs/permissions-list.md
cat ./src/main/resources/assets/fastback/lang/en_us.json | \
jq -r 'to_entries[] |select(.key|match("fastback.help.command.*")) | (["* `fastback.command.", (.key|split(".")[3]), "`"]|join(""))' \
>> docs/permissions-list.md

