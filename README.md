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
- `port`: The port to listen on. Defaults to 25565
- `online_mode`: Whether usernames should be verified when connecting to the proxy
- `player_limit`: The maximum number of players allowed to connect to the proxy. Set to -1 to disable.

### Advanced options
- `reconnect_timeout`: How many milliseconds a player has to reconnect after being transferred
- `reconnect_threads`: How many threads should be used to await reconnections
- `auth_threads`: How many threads should be available for authentication requests
- `backend_timeout`: The maximum number of milliseconds for a backend server to connect
- `client_timeout`: The maximum number of milliseconds for a client to connect to the server

### Backends
The `backends` section of the configuration file is used to define the backend servers and their connection precedence.
The proxy can route players to different backends depending on a variety of factors. Each backend at minimum needs a
name and address. A minimal backend configuration would look something like the following:
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
- `redirect`: If this is set, the player will be sent a transfer packet with the backend's hostname and port, rather
than having the proxy handle the connection
- `requirement`: A requirement the connecting player will have to fulfill in order to connect to this backend. This is 
the primary method of routing connections. If the requirement fails, the server with the next-highest priority will be 
attempted. There are a number of requirement types, see below.

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
- `secure_chat`: Declares to the client whether the server uses secure chat or not
- `preview_chat`: Declares to the client whether the server uses chat previews or not
- `passthrough`: If set, the proxy will relay all status messages to the backend server with the given ID. All null 
fields in this entry will be filled with data from the backend server.
- `requirement`: A requirement the connecting player will have to fulfill in order to see this message. Used in the same
way as backends.
  - Note: Because server status requests only have access to the player's protocol version, IP address, and the hostname 
  and port used to connect to the server, not all requirement types can be used.

### Requirements
Backend servers and status entries use requirements to route connections. Each connection needs a type and a value. 
There are a number of requirement types, and the value is dependent on the type. A minimal requirement which checks the
hostname used to connect would look something like the following:
```json
"requirement": {
    "type": "hostname",
    "value": "hub.example.com"
}
```
Valid requirement types and their value syntax are as follows:
- `hostname`: Checks the hostname a player used to connect to the server. The value should be a string or list of strings
  defining valid hostnames
- `port`: Checks the port a player used to connect to the server. The value should be an integer or list of integers
  defining valid ports
- `username`: Checks the player's username. The value should be a string or list of strings defining valid usernames.
Not available in status requests.
- `uuid`: Checks the player's uuid. The value should be a string or list of strings defining valid uuids. Not available 
in status requests.
- `cookie`: Checks for a specified cookie saved on the client. The value should be an object containing a value `cookie`,
defining which cookie should be queried, and a value `values`, a list of strings defining valid cookie values. Not 
available in status requests.
- `locale`: Checks the player's locale information (i.e. en_us). The value should be a string or list of strings defining
valid locales. Not available in status requests.

*Note: If the highest priority backend server has no requirement, or a connecting player can fulfill its requirement 
without signing in, the proxy will **not** attempt to authenticate them. It is expected that backend servers are in
online mode. An option to always force authentication will be added in the future.*

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
The proxy's API is published in the repository at `https://maven.wallentines.org`
The proxy's API is published with the artifact ID `org.wallentines:midnightproxy-api`. The latest version is `0.1.0`

As of now, the plugin API is pretty limited. Plugins can pretty much only add console commands and requirement types.

## Differences From Typical Proxy Software
*i.e. What makes this different from other Minecraft proxies such as Velocity or BungeeCord?*

While it is accomplishing the same goal, this proxy operates in a fundamentally different way. This proxy is more 
similar to general-purpose reverse proxies such as HAProxy, but specifically tailored to Minecraft servers. 

A typical Minecraft proxy will establish an encrypted connection with the player, and another to the backend server, 
then route packets accordingly. Server switching is done by utilizing the configuration phase, or sending respawn 
packets. This system gives the proxy a lot of control over communication between client and server, and makes proxying
multiple servers trivial.

In MidnightProxy, as soon as a connection is routed properly, *all* traffic is passed through directly to the 
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
  - I plan to release a Fabric mod / Spigot plugin which facilitates server-switching through JWT cookies, and adds a
/server command to individual servers, in order to work around this issue.
- As of now, all connections to backend servers will look like they are coming from the proxy. This breaks IP-banning,
and prevents servers from enabling `prevent-proxy-connections` in the `server.properties` file.
  - In the future, I plan to add an `ip_address` requirement type to allow proxy-wide IP bans.
  - I also plan on adding an option to send HAProxy's PROXY v2 packets to backend servers to facilitate IP forwarding, 
  along with a companion Fabric mod (and *maybe* Spigot plugin) which allows servers to properly read those packets.