FROM node:22-bookworm-slim AS ui-build-stage

WORKDIR /app

RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y python3 make gcc g++ && \
    rm -rf /var/lib/apt/lists/*

COPY package*.json ./

RUN npm config list && \
    npm install

COPY . .

RUN npm run build

FROM ubuntu:noble

# upgrade from noble-security only
RUN mkdir -p /tmp/apt-security \
 && awk -v RS= -v ORS='\n\n' '/noble-security/' /etc/apt/sources.list.d/ubuntu.sources \
      > /tmp/apt-security/security.sources \
 && apt-get -o Dir::Etc::SourceList=/dev/null -o Dir::Etc::SourceParts=/tmp/apt-security update \
 && apt-get -o Dir::Etc::SourceList=/dev/null -o Dir::Etc::SourceParts=/tmp/apt-security upgrade -y \
 && rm -rf /tmp/apt-security /var/lib/apt/lists/*

RUN apt-get update \
 && apt-get install -y --no-install-recommends nginx \
 && rm -rf /var/lib/apt/lists/* \
 && rm -f /etc/nginx/sites-enabled/default \
 && ln -sf /dev/stdout /var/log/nginx/access.log \
 && ln -sf /dev/stderr /var/log/nginx/error.log \
 && sed -i -e '/^user /d' -e 's|^pid .*|pid /run/nginx/nginx.pid;|' /etc/nginx/nginx.conf \
 && mkdir -p /run/nginx \
 && chown -R www-data:www-data /var/lib/nginx /var/log/nginx /run/nginx

COPY container-files/etc/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf

COPY --chown=www-data:www-data --from=ui-build-stage /app/dist/ /usr/share/nginx/html/

COPY --chmod=755 entrypoint.sh /entrypoint.sh

USER www-data

EXPOSE 8090
STOPSIGNAL SIGQUIT
ENTRYPOINT ["/entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
