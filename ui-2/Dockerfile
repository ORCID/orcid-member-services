FROM node:22-bookworm-slim AS ui-build-stage

WORKDIR /app

RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y python3 make gcc g++ vim && \
    rm -rf /var/lib/apt/lists/*

COPY package*.json ./

RUN npm config list && \
    npm install

COPY . .

RUN npm run build

FROM nginx:1.26-bookworm

RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y procps vim && \
    rm -rf /var/lib/apt/lists/*

COPY container-files/etc/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf

COPY --from=ui-build-stage /app/dist/ /usr/share/nginx/html/

EXPOSE 8090

COPY entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]

CMD ["nginx", "-g", "daemon off;"]