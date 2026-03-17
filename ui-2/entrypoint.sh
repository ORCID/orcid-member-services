#!/bin/sh

# Overwrite the compiled env.js with the real Docker environment variables
# The Angular i18n build outputs to locale subdirectories (en/, es/, etc.)
ENV_CONTENT="(function(window) {
  window.__env = window.__env || {};
  window.__env.issuerUrl = '${APPLICATION_SECURITY_ISSUER_URL}';
})(this);"

for dir in /usr/share/nginx/html/*/assets; do
  if [ -d "$dir" ]; then
    echo "$ENV_CONTENT" > "$dir/env.js"
  fi
done

# Also write to the root assets dir if it exists
if [ -d /usr/share/nginx/html/assets ]; then
  echo "$ENV_CONTENT" > /usr/share/nginx/html/assets/env.js
fi

# Hand off control to the main container command (Nginx)
exec "$@"
