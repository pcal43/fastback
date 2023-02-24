Action                 | Use
---------------------- | ---
`daily`                | Daily: Keep the last snapshot from each day, plus all snapshots from the last `n` days
`fixed`                | Fixed: Keep only the `n` most-recent snapshots.
`gfs`                  | GFS: Keep every backup today + latest daily backup in the last week + latest weekly backup in the last month + latest backup of each month
`all`                  | Retain all snapshots; never prune

