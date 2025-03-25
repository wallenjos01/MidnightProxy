# MidnightProxy JWT Plugin
A plugin for MidnightProxy which allows the usage of Json Web Token cookies for routing.

## Installation
Put the latest [Release](https://github.com/wallenjos01/MidnightProxy/releases/) into the proxy's `plugins` folder.

## Generating Keys
To use the plugin, you first need to generate one or more agreed-upon keys, which can be given to backend servers for
signing or encrypting tokens. To do this, run the following command in the console:
```
jwt genKey <name> -t <type> -l <length>
```
- The `<name>` argument can be whatever you like
- The `<type>` argument should be one of the following: (Defaults to `hmac`)
  - `hmac`: Used for generating signed (but not encrypted) JWTs
  - `aes`: Used for generating encrypted JWTs with AES key encoding
  - `rsa`: Used for generating encrypted JWTs with RSA asymmetric key encoding
- The `<length>` argument is a length (in octets) of the generated key. (Defaults to `32`)
  - The length is not used when generating an RSA key
  - If the key type is AES, the length will determine the key encryption algorithm which will be used, it should be one
of the following:
    - `16`: for the `A128KW` algorithm
    - `24`: for the `A192KW` algorithm
    - `32`: for the `A256KW` algorithm

The generated key will appear in the `config/jwt/keystore` folder by default.

Then, the `routes` section of the configuration can be updated to work with JWTs.

## JWT Requirements
Routing with JWT is done using requirements. Specifically, the JWT plugin registers a new requirement type with the id
`mdp:jwt`. This requirement type accepts the following fields:
- `cookie`: (Required) The ID of cookie which contains the JWT
- `expect_claims`: A mapping of claims to expected values. If any of the values are mismatched, the requirement will fail. 
Placeholders may be used for values. (See below)
- `output_claims`: A list of claims which will be output to the connection context after the requirement is passed.
- `key`: The name of the key to use to verify/decrypt the cookie. (Defaults to `default`)
- `key_type`: The type of key to use to verify/decrypt the cookie. If unset, will default to the key type specified in the
token's JOSE header.
- `single_use_claim`: If set, the specified claim should be set to a UUID which is unique to this cookie. That UUID will 
be checked against a registry of recently accessed cookies during validation. If a cookie is re-used before expiration, 
the check will fail.
- `require_auth`: If set to true, the proxy will require the player to be authenticated before requesting the cookie.
- `require_encryption`: If set to true, the check will fail if the token is not encrypted.

### Placeholders
The values in the `expect_claims` section can be set to placeholders. These placeholder will be resolved while the requirement
is being checked. Placeholders are formatted as a string between `%` characters. Valid placeholders include:
- `%client_username%`: The username declared by the client. (Maybe forged if checked before authentication) 
- `%client_uuid%`: The username declared by the client. (Maybe forged if checked before authentication) 
- `%client_protocol%`: The client's declared protocol version.
- `%client_hostname%`: The hostname the client used to connect.
- `%client_port%`: The port the client used to connect.
- `%client_locale%`: The player's declared locale. (Not available before authentication)

### Outputs
The values in the `output_claims` array will be stored in the connection context if the check is passed. These can be
accessed in a route's `backend` value. The claims are stored with the format `jwt.<claim>`. These claims must be *present*
in the JWT in order for the check to pass, but the claims' values are not validated in any way.

### Examples
Using a JWT to connect a player to a backend:
```json
{
  "routes": [
    {
      "requirement": {
        "type": "mdp:jwt",
        "key": "my_key",
        "cookie": "my_cookie",
        "expect_claims": {
          "username": "<client_username>",
          "uuid": "<client_uuid>"
        },
        "output_claims": [
          "backend"
        ],
        "single_use_claim": "token_id",
        "require_auth": false
      },
      "backend": "<jwt_backend>"
    },
    {
      "backend": "lobby"
    }
  ]
}
```

Using a JWT to connect a player to an ephemeral backend:
```json
{
  "routes": [
    {
      "requirement": {
        "type": "mdp:jwt",
        "key": "my_key",
        "cookie": "my_cookie",
        "expect_claims": {
          "username": "<client_username>",
          "uuid": "<client_uuid>"
        },
        "output_claims": [
          "backend_host",
          "backend_port"
        ],
        "single_use_claim": "token_id",
        "require_auth": false
      },
      "backend": {
        "hostname": "<jwt_backend_hostname>",
        "port": "<jwt_backend_port>"
      }
    },
    {
      "backend": "lobby"
    }
  ]
}
```

## Setting the Cookies
As this is a general-purpose plugin, theoretically any backend server could use a plugin/mod to set a JWT cookie.

[ServerSwitcher](https://github.com/wallenjos01/ServerSwitcher) is an official companion mod to MidnightProxy, and is intended
to be used to facilitate routing between servers without requiring multiple hostnames or ports.
More details can be found at the bottom of the README for [ServerSwitcher](https://github.com/wallenjos01/ServerSwitcher).