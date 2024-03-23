# MidnightProxy Whitelist Plugin
A plugin for MidnightProxy which allows the usage of Whitelists for routing.

## Installation
Put the latest [Release](https://github.com/wallenjos01/MidnightProxy/releases/) into the proxy's `plugins` folder.

## Whitelists
Whitelists are defined in a `lists` section in config/whitelist/config.json
A sample config.json may look something like the following:
```json
{
  "lists": {
    "default": {
      "uuids": [ "<insert uuids here>" ],
      "names": [ "<insert usernames here>" ]
    }
  }
}
```
It is recommended to use uuids over usernames, so that players do not become un-whitelisted after changing their 
username.

## Whitelist requirements
Routing with Whitelists is done using a requirement type with the id `mdp:whitelist`. This requirement type accepts the 
following fields:
- `whitelist` (required): The whitelist to check
- `require_auth`: If set to true, the proxy will require the player to be authenticated before checking if they are on 
the whitelist (defaults to true).


### Example
```json
{
  "routes": [
    {
      "requirement": {
        "type": "mdp:whitelist",
        "require_auth": true,
        "whitelist": "default"
      },
      "kick_on_fail": true,
      "kick_message": "kick.not_whitelisted"
    },
    {
      "backend": "lobby"
    }
  ]
}
```