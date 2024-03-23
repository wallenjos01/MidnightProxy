# MidnightProxy Last Server Plugin
A plugin for MidnightProxy which allows the proxy to route players to the last server they were on

## Installation
Put the latest [Release](https://github.com/wallenjos01/MidnightProxy/releases/) into the proxy's `plugins` folder.

## Last server requirements
Routing is done using a requirement type with the id `mdp:last_server`. The requirement will fail if the player has not
joined the server before, or if the plugin does not have the player's last server stored. The plugin will always store
the last server each player was on when they disconnect.

This requirement type accepts the 
following fields:
- `require_auth`: If set to true, the proxy will require the player to be authenticated before checking their previous server (defaults to true).


### Outputs
The last server requirement will output the last server the player was on as `last_server.backend`. This can be accessed 
as a placeholder in the route's `backend` value.

### Example
```json
{
  "routes": [
    {
      "requirement": {
        "type": "mdp:last_server",
        "require_auth": true
      },
      "backend": "%last_server.backend%"
    }
  ]
}
```