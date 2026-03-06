#!/bin/sh

# Overwrite the compiled env.js with the real Docker environment variables
cat <<EOF > /usr/share/nginx/html/assets/env.js
(function(window) {
  window.__env = window.__env || {};
  window.__env.issuerUrl = '${APPLICATION_SECURITY_ISSUER_URL}';
})(this);
EOF

# Hand off control to the main container command (Nginx)
exec "$@"