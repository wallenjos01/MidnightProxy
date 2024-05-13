#!/bin/bash
# vim:sw=4:ts=4:et

# Adapted from (https://github.com/nginxinc/docker-nginx/blob/master/entrypoint/docker-entrypoint.sh)

set -e

entrypoint_log() {
    if [ -z "${ENTRYPOINT_DEBUG_LOGS}" ]; then
        echo "$@"
    fi
}

# Run Startup Scripts
if [ "$1" = "/proxy/wrapper" ]; then
    if /usr/bin/find "/docker-entrypoint.d/start/" -mindepth 1 -maxdepth 1 -type f -print -quit 2>/dev/null | read v; then
        entrypoint_log "$0: Running startup scripts"

        find "/docker-entrypoint.d/start/" -follow -type f -print | sort -V | while read -r f; do
            case "$f" in
                *.envsh)
                    if [ -x "$f" ]; then
                        entrypoint_log "$0: Sourcing $f";
                        . "$f"
                    else
                        # warn on shell scripts without exec bit
                        entrypoint_log "$0: Ignoring $f, not executable";
                    fi
                    ;;
                *.sh)
                    if [ -x "$f" ]; then
                        entrypoint_log "$0: Running $f";
                        "$f"
                    else
                        # warn on shell scripts without exec bit
                        entrypoint_log "$0: Ignoring $f, not executable";
                    fi
                    ;;
                *) entrypoint_log "$0: Ignoring $f";;
            esac
        done
    else
        entrypoint_log "$0: No startup scripts found"
    fi
fi

# Set user ID
if [[ ! -z "${PUID}" ]]; then
  entrypoint_log "$0: Changing UID to ${PUID}";
	usermod -o -i $PUID proxy
fi

# Set group ID
if [[ ! -z "${PGID}" ]]; then
  entrypoint_log "$0: Changing GID to ${PGID}";
	groupmod -o -g $PGID proxy
fi

# Add more groups
if [[ ! -z "${ADD_GROUPS}" ]]; then

	groups=$(echo "${ADD_GROUPS}" | tr "," "\n")

	for g in "${groups[@]}"
	do
		$g_name=$(echo "$g" | cut -d':' -f1)
		$g_id=$(echo "$g" | cut -d';' -f2)

    entrypoint_log "$0: Adding group ${g_id}";
		groupadd -g $g_id $g_name
		usermod -aG $g_name proxy

	done
fi


# Start Server
if [ "$1" = "/proxy/wrapper" ]; then
  entrypoint_log "$0: Running wrapper as proxy user";
	sudo -u proxy PATH=$PATH "$@"
else
	exec "$@"
fi

# Run Stop Scripts
if [ "$1" = "/proxy/wrapper" ]; then
    if /usr/bin/find "/docker-entrypoint.d/stop/" -mindepth 1 -maxdepth 1 -type f -print -quit 2>/dev/null | read v; then
        entrypoint_log "$0: Running shutdown scripts"

        find "/docker-entrypoint.d/stop/" -follow -type f -print | sort -V | while read -r f; do
            case "$f" in
                *.envsh)
                    if [ -x "$f" ]; then
                        entrypoint_log "$0: Sourcing $f";
                        . "$f"
                    else
                        # warn on shell scripts without exec bit
                        entrypoint_log "$0: Ignoring $f, not executable";
                    fi
                    ;;
                *.sh)
                    if [ -x "$f" ]; then
                        entrypoint_log "$0: Running $f";
                        "$f"
                    else
                        # warn on shell scripts without exec bit
                        entrypoint_log "$0: Ignoring $f, not executable";
                    fi
                    ;;
                *) entrypoint_log "$0: Ignoring $f";;
            esac
        done
    else
        entrypoint_log "$0: No shutdown scripts found"
    fi
fi