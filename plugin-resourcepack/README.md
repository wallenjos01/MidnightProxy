# MidnightProxy ResourcePack Plugin
A plugin for MidnightProxy which sends server resource packs to clients.

## Installation
Put the latest [Release](https://github.com/wallenjos01/MidnightProxy/releases/) into the proxy's `plugins` folder.

## Configuration
Resource packs are defined in the `packs` section of the plugin config.
The `routes` section of the config defines resource packs sent based on the backend server
The `global` section of the config lists resource packs sent to all players regardless of backend

### Examples
Sending a resource pack to all players
```json
{
  "packs": {
    "my_pack": {
      "uuid": "<pack-uuid>",
      "url": "<pack-zip-url>",
      "hash": "<pack-zip-sha1>"
    }
  },
  "global": [ "my_pack" ]
}
```

Sending a resource pack to a specific backend
```json
{
  "packs": {
    "my_pack": {
      "uuid": "<pack-uuid>",
      "url": "<pack-zip-url>",
      "hash": "<pack-zip-sha1>"
    }
  },
  "routes": {
    "lobby": [ "my_pack" ]
  }
}
```

Marking a pack as required
```json
{
  "packs": {
    "my_pack": {
      "uuid": "<pack-uuid>",
      "url": "<pack-zip-url>",
      "hash": "<pack-zip-sha1>",
      "required": true
    }
  },
  "global": [ "my_pack" ]
}
```

Using a custom prompt message for a required pack
```json
{
  "packs": {
    "my_pack": {
      "uuid": "<pack-uuid>",
      "url": "<pack-zip-url>",
      "hash": "<pack-zip-sha1>",
      "required": true,
      "message": "&bPlease apply my resource pack!"
    }
  },
  "global": [ "my_pack" ]
}
```

Using a custom kick message for a required pack
```json
{
  "packs": {
    "my_pack": {
      "uuid": "<pack-uuid>",
      "url": "<pack-zip-url>",
      "hash": "<pack-zip-sha1>",
      "required": true,
      "kick_message": "&cYou rejected my resource pack... :("
    }
  },
  "global": [ "my_pack" ]
}
```