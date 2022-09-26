---
layout: default
title: Commands
nav_order: 80
---

# Commands

Type `/backup` followed by one of these subcommands:

Command                | Use
---------------------- | ---
`enable`               | Enable local backups backups on this world.
`disable`              | Disable backups on this world.
`local`                | Perform a local backup immediately.
`full`                 | Perform a local and remote backup immediately.
`info`                 | Info about current backup state and settings.
`restore`              | Restore a backup snapshot.
`create-file-remote`   | Create a remote backup target on the file system.
`set-remote`           | Set the url for remote backups.
`set-shutdown-action`  | Enable or disable backups on shutdown.
`set-retention`        | Set snapshot retention policy.
`prune`                | Delete old snapshots according to the retention policy.
`purge`                | Delete an individual snapshot.
`gc`                   | Run garbage collection to free up disk space.
`list`                 | List backup snapshots for this world.
`help`                 | Get help on commands.