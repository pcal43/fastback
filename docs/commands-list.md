
| Command                           | Use                                                                                      |
|-----------------------------------|------------------------------------------------------------------------------------------|
| `init`                            | Initialize fastback for the current world.  Run this first.                              |
| `help`                            | Get help on commands.                                                                    |
| `local`                           | Perform a local backup immediately.                                                      |
| `full`                            | Perform a local backup followed by a remote push (if configured).                        |
| `restore`                         | Restore a backup snapshot.                                                               |
| `delete`                          | Delete an individual snapshot.                                                           |
| `info`                            | Info about current backup state and settings.                                            |
| `list`                            | List backup snapshots for this world.                                                    |
| `push`    _NEW_!                  | Push a snapshot to the remote.                                                           |
| `prune`                           | Delete old snapshots according to the retention policy.                                  |
| `gc`                              | Run garbage collection to free up disk space.                                            |
| `create-file-remote`              | Create a remote backup target on the file system.                                        |
| `remote-delete`                   | Delete a remote snapshot.                                                                |
| `remote-list`                     | List remote snapshots.                                                                   |
| `remote-prune`                    | Delete old snapshots from the remote backup according to the remote retention policy.    |
| `remote-restore`                  | Restore a remote snapshot.                                                               |
| `set retention-policy`            | Set retention policy for local snapshots.                                                |
| `set remote-url`                  | Set the url for remote backups.                                                          |
| `set shutdown-action`             | Set an action to perform on shutdown.                                                    |
| `set autoback-action`             | Set an action to perform during auto-backups.                                            |
| `set autoback-wait`               | Set the minimum number of minutes to wait between auto-backups.                          |
| `set restore-directory`           | Target directory for restored snapshots.  Useful for servers with limited tmp space.     |
| `set remote-retention-policy`     | Set retention policy for remote snapshots.                                               |
| `set mods-backup-enabled` _NEW_!  | Whether to also backup mod jars and config files (in `.fastback/mods-backup`)            |
| `set broadcast-enabled` _NEW_!    | Whether to send a server-wide notice when a backup is starting.                          |
| `set broadcast-message`_NEW_!     | Customized server-wide notice message.                                                   |
| `set lock-cleanup-enabled` _NEW_! | Automatic cleanup of orphaned `index.lock` files.  Be careful!                           |
| `set force-debug-enabled` _NEW_!  | Enable verbose debugging output to the console.  Useful if you're running into problems. |


