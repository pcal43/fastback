Action                 | Use
---------------------- | ---
`local`                | Backs up locally only.  Like `/backup local`
`full`                 | Back up locally and upload.  Like `/backup full`
`full-gc`              | Do a full backup followed by a `prune` and `gc` to reclaim disk space. <br/> This might slow your game down if scheduled during autosaves.
`none`                 | Don't do anything
