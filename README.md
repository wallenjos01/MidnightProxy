# MidnightProxy
A reverse proxy for Minecraft servers, utilizing the transfer packets introduced in Minecraft 1.20.5 (24w03a)
</br>
** *Note: this software is currently **alpha** quality!*
## Installation
Simply download and execute the latest jar from the [Releases](https://github.com/wallenjos01/MidnightProxy/releases/) 
tab. A `config.json` file will be created automatically. Stop the server by typing `stop` into the console, then start 
configuring.

## Configuration
### Basic Options
- `port`: The port to listen on. Defaults to 25565.
- `online_mode`: Whether usernames should be verified when connecting to the proxy. Defaults to true. 
- `player_limit`: The maximum number of players allowed to connect to the proxy. Defaults to 100. Set to -1 to disable.
- `icon_cache_dir`: The directory the proxy should look for status icons in. Defaults to `icons`.

### Advanced options
- `reconnect_timeout_sec`: How many seconds a player has to reconnect after being transferred. Defaults to 3.
- `backend_timeout_ms`: How many milliseconds the proxy should wait while attempting to connect to a backend server before assuming it is offline. Defaults to 5000.
- `client_timeout_ms`: How many milliseconds the proxy should wait between packets from a client before timing them out. Defaults to 15000.
- `auth_threads`: How many threads should be available for authentication requests. Defaults to 4.
- `force_authentication`: Whether the proxy should always attempt to authenticate connecting players, even if routing could be done without it. Defaults to false.
- `icon_cache_size`: How many icons should be kept in memory after being loaded. Defaults to 8.
- `prevent_proxy_connections`: Whether the server should reject players who attempt to connect from behind their own proxy. Only checked when doing authentication with Mojang. Defaults to false.
- `haproxy_protocol`: Whether the proxy expects HAProxy PROXY messages to be sent from joining clients. Defaults to false.
- `log_status_messages`: Whether the proxy should send a log message when it receives a status handshake (Server ping). Defaults to false.
- `reply_to_legacy_ping`: Whether the proxy should respond to legacy ping requests (pre-1.7). Defaults to true.

### Backends
The `backends` section of the configuration file is used to define the backend servers.
Each backend at minimum needs a name and address. A minimal backend configuration would look something like the following:
```json
"backends": { 
    "main": {
        "hostname": "localhost"
    }
}
```
Valid options for backends include:
- `port`: The port the backend server is listening on.
- `priority`: Backend servers with higher priority will be checked first.
- `redirect`: If this is set to `true`, the player will be sent a transfer packet with the backend's hostname and port, rather
than having the proxy handle the connection
- `haproxy`: If this is set to `true`, a HAProxy PROXY protocol message will be sent to backend servers when players connect to them.
This can allow for IP forwarding to backend servers, but only if the backend server is modified to support it.

### Routes
The `routes` section of the configuration file is used to define how the proxy should route connections.
Route precedence is determined by their index into the `routes` array. A minimal routes configuration would look something like the following:
```json
"routes": [
    {
        "backend": "main"    
    }
]
```
Valid options for routes include: 
- `backend`: The ID of the backend server to connect to
- `requirement`: The requirement for using this route
- `kick_on_fail`: Whether the proxy should kick clients when they fail the requirement
- `kick_message`: The language key to use when kicking a player (defaults to `error.generic_route_failed`)

The proxy can route players to different backends depending on a variety of factors, all defined via requirements.
To define a requirement, use the `requirement` key in a route configuration. If a player fulfills the requirement for the \
route with the highest precedence, they will be routed to that backend.

*Note: If the highest priority route has no requirement, or a connecting player can fulfill its requirement
without signing in, the proxy will **not** attempt to authenticate them. It is expected that backend servers are in
online mode. Set `force_authentication` to `true` if you want players to always be authenticated.*

### Status
The `status` section of the configuration file defines how the server should respond to status requests. Status requests
can be fulfilled by the proxy or passed through to backend servers, and can be routed in a similar fashion to logins. A
basic status entry would look something like the following:
```json
"status": [
    {
        "message": { "text": "A MidnightProxy Server" }
    }
]
```
A status entry which passes through to a backend server named *main* would look something like the following:
```json
"status": [
    {
        "passthrough": "main"
    }
]
```
Valid options for status entries include:
- `priority`: Status options with a higher priority will be checked first.
- `players_override`: Overrides the player count sent to the player. If omitted, the number of players on the proxy (or
passthrough backend) will be sent instead.
- `max_players_override`: Overrides the player limit sent to the player.
- `player_sample`: Sets the preview list of players which will be visible to the client when they hover over the player
count in the server list.
- `message`: The JSON message which appears in the server list
- `icon`: The name of the icon image file which should appear in the server list
- `secure_chat`: Declares to the client whether the server uses secure chat or not
- `preview_chat`: Declares to the client whether the server uses chat previews or not
- `passthrough`: If set, the proxy will relay all status messages to the backend server with the given ID. All null 
fields in this entry will be filled with data from the backend server.
- `requirement`: A requirement the connecting player will have to fulfill in order to see this message. Used in the same
way as backends.
  - Note: Because server status requests only have access to the player's protocol version, IP address, and the hostname 
  and port used to connect to the server, not all requirement types can be used.

## Requirements
Routes and status entries use requirements to route connections. Each connection needs a type and a value. 
There are a number of requirement types, and the configuration keys are dependent on the type. A minimal requirement which checks the
hostname used to connect would look something like the following:
```json
"requirement": {
    "type": "hostname",
    "value": "hub.example.com"
}
```

### Inversion
Requirements with `invert` set to true will have their check inverted. i.e. A check that will normally only pass when a condition
is met will instead pass in all cases *unless* that condition is met.

### Requirement Types
Valid requirement types and their configuration syntax are as follows:
- `hostname`: Checks the hostname a player used to connect to the server. It should have a `value` key, which is a string or list of strings
  defining valid hostnames
- `port`: Checks the port a player used to connect to the server. It should have a `value` key, which is an integer or list of integers
  defining valid ports
- `ip_address`: Checks the player's IP address. It should have a `value` key, which is a string or list of strings representing valid IP addresses.
- `username`: Checks the player's username. It should have a `value` key, which is a string or list of strings defining valid usernames.
Not available in status requests.
- `uuid`: Checks the player's uuid. It should have a `value` key, which is a string or list of strings defining valid uuids. Not available 
in status requests.
- `cookie`: Checks for a specified cookie saved on the client. It should have a `cookie` key,
defining which cookie should be queried, and a `value` key, which contains a list of strings defining valid cookie 
values. Not available in status requests.
- `locale`: Checks the player's locale information (i.e. en_us). It should have a `value` key, which is a string or list of strings defining
valid locales. Not available in status requests.
- `date`: Checks the current time and date on the server. It should have one or more of the following optional fields:
  - `time_zone`: The time zone to use (defaults to the system's time zone)
  - `second`: A range of seconds of the minute which are valid.
  - `minute`: A range of minutes of the hour which are valid.
  - `hour`: A range of hours of the day which are valid.
  - `day`: A range of days of the month which are valid.
  - `month`: A range of months of the year which are valid.
  - `year`: A range of years of the era which are valid.
- `composite`: Combines multiple requirements into one. It should have the following keys:
  - `count`: The number of requirements which need to be completed, or a range. (Set to "all" to require all 
  sub-requirements to be fulfilled)
  - `values`: A list of requirements which must be fulfilled

### Ranges
Date requirements fields and the composite requirement `count` field allow you to specify ranges of numbers in any
of the following formats:
- `N`: Exactly N
- `">N"`: more than N
- `"<N"`: Less than N
- `">=N"`: At least N
- `"<=N"`: At most N
- `"[N,M]"`: Between N and M (inclusive)
- `"(N,M)"`: Between N and M (exclusive)
- `"{N,M,O}"`: One of N, M, or O

### Outputs
Some requirement types (Such as [`mdp:jwt`](plugin-jwt/README.md)) may output values when successfully checked. These values can be accessed in a route's
`backend` field, by typing the name of the output between `%` characters.

### Examples
Requirement which routes players based on hostname:
```json
"routes": [
    {
        "backend": "hub",
        "requirement": {
            "type": "hostname",
            "value": ["hub.server.net"]
        }
    },
    {
        "backend": "survival",
        "requirement": {
            "type": "hostname",
            "value": ["survival.server.net"]
        }
    }
]
```
A requirement which denies players based on their username:
```json
"routes": [
    {
        "backend": "main",
        "requirement": {
            "type": "username",
            "invert": true,
            "value": ["Player1", "Player2"]
        }
    }
]
```
A status requirement which displays a different message and icon on Christmas day
```json
"status": [
    {
        "message": "&aMerry Christmas!",
        "icon": "christmas"
        "requirement": {
            "type": "date",
            "day": 25,
            "month": 12
        }
    }
]
```

## Lang
Language files are stored in the `lang` directory. Configurable messages include those that are sent to clients when 
routing fails.

## Plugins
The proxy will load plugins from the `plugins` folder on startup. A valid plugin is a jar file containing a `plugin.json`
file at its root with the following contents:
```json
{
  "name": "<The name of the plugin> (ExamplePlugin)",
  "version": "<The plugin's semantic version> (1.0.0)",
  "main": "<The plugin's main class> (com.example.plugin.Main)"
}
```
A plugin's main class must implement the `Plugin` interface, which requires it implement the method `void initialize(Proxy)`
The proxy's API is published in the repository at `https://maven.wallentines.org/releases`
The proxy's API is published with the artifact ID `org.wallentines:midnightproxy-api`. The latest version is `0.5.1`

Using the plugin API, plugins can:
- Add console commands. (See `org.wallentines.mdproxy.command.CommandExecutor`)
- Add requirement types. (See `org.wallentines.mdproxy.requirement.ConnectionCheckType`)
- Modify the way online players are counted. (See `org.wallentines.mdproxy.PlayerCountProvider`)
- Listen for events fired during the lifecycle of the proxy or individual connections
- Send and await plugin messages during the login and configuration phases.


## Differences From Typical Proxy Software
*i.e. What makes this different from other Minecraft proxies such as Velocity or BungeeCord?*

While it is accomplishing the same goal, this proxy operates in a fundamentally different way. This proxy is more 
similar to general-purpose reverse proxies such as HAProxy, but specifically tailored to Minecraft servers. 

A typical Minecraft proxy will establish an encrypted connection with the player, and another to the backend server, 
then route packets accordingly. Server switching is done by utilizing the configuration phase, or sending respawn 
packets. This system gives the proxy a lot of control over communication between client and server, and makes proxying
multiple servers trivial.

In MidnightProxy, as soon as a connection is routed properly, *all* data is passed through directly to the 
backend server. This has a number of benefits, including: end-to-end encryption between the player and the backend 
server, allowing the proxy to work with unmodified vanilla servers in online mode. Or allowing for a proper login phase 
for each server connection, so mods which utilize custom login query packets work properly out of the box. However, 
there are also some drawbacks:

### Limitations
- Because the connection is end-to-end encrypted, the proxy cannot send or intercept packets, or declare commands, so 
server-switching has to be initiated by the backend servers, instead of by the proxy. This is possible in vanilla as of 
version 24w04a, with the /transfer command, but is limited without additional modifications to the server. 
- The backend queue will be run through on each server-switch, as if the player connected for the first time, so each 
server will need some way to differentiate itself. Some ways to do this would be through hostnames, or by having a 
modified backend server set a cookie before transferring the player.
  - This can be accomplished by installing and configuring the [JWT plugin](plugin-jwt/README.md) on the proxy, and [ServerSwitcher](https://github.com/wallenjos01/serverswitcher) on each backend server. This only works on Fabric backends at the moment.
- By default, all connections to backend servers will look like they are coming from the proxy. This breaks IP-banning,
and prevents servers from enabling `prevent-proxy-connections` in the `server.properties` file. 
  - This can be fixed by enabling the `haproxy` option for backend servers, and installing [Fabric-HAProxy](https://github.com/wallenjos01/fabric-haproxy) on the backend servers. This only works on Fabric backends at the moment.
