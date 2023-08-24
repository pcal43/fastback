
| Command                    | Use                                                                                   |
|----------------------------|---------------------------------------------------------------------------------------|
| `create-file-remote`       | Create a remote backup target on the file system.                                     |
| `delete`                   | Delete an individual snapshot.                                                        |
| `disable`                  | Disable backups on this world.                                                        |
| `enable`                   | Enable local backups backups on this world.                                           |
| `full`                     | Perform a local and remote backup immediately.                                        |
| `gc`                       | Run garbage collection to free up disk space.                                         |
| `help`                     | Get help on commands.                                                                 |
| `info`                     | Info about current backup state and settings.                                         |
| `list`                     | List backup snapshots for this world.                                                 |
| `local`                    | Perform a local backup immediately.                                                   |
| `prune`                    | Delete old snapshots according to the retention policy.                               |
| `remote-delete`            | Delete a remote snapshot.                                                             |
| `remote-list`              | List remote snapshots.                                                                |
| `remote-prune`             | Delete old snapshots from the remote backup according to the remote retention policy. |
| `remote-restore`           | Restore a remote snapshot.                                                            |
| `restore`                  | Restore a backup snapshot.                                                            |
| `set-autoback-action`      | Set an action to perform during auto-backups.                                         |
| `set-autoback-wait`        | Set the minimum number of minutes to wait between auto-backups.                       |
| `set-remote`               | Set the url for remote backups.                                                       |
| `set-remote-retention`     | Set snapshot retention policy for the remote backup.                                  |
| `set-retention`            | Set snapshot retention policy.                                                        |
| `set-shutdown-action`      | Set an action to perform on shutdown.                                                 |
| `set broadcast-enabled`    | Enables sending of a server-wide notice when a backup is starting.                    |
| `set broadcast-message`    | Customized server-wide notice message.                                                |
| `set lock-cleanup-enabled` | Automatic cleanup of orphaned `index.lock` files.  Be careful!                        |
| `set native-git-enabled`   | Enable native git support.  Recommended!                                              |
| `set restore-directory`    | Target directory for restored snapshots.  Useful for servers with limited tmp space.  |


