FROM node:20.7.0-bullseye-slim AS ui-build-stage

# Create app directory
WORKDIR /app

# Install some useful utils for debug/test
RUN \
    apt-get update && \
    apt -y install python3 make gcc g++ && \
    rm -rf /var/lib/apt/lists/*

# Install app dependencies
# A wildcard is used to ensure both package.json AND package-lock.json are copied
# where available (npm@5+)
COPY package*.json ./

# If you are building your code for production
# RUN npm install --only=production
RUN \
    npm config list && \
    npm config delete proxy && \
    npm install

# Bundle app source
COPY . .

RUN npm run build

FROM nginx

RUN \
    apt-get update && \
    apt -y install procps vim && \
    rm -rf /var/lib/apt/lists/*

COPY container-files/etc/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf

# Copy files from the ui-build-stage into this new nginx stage
COPY --from=ui-build-stage /app/dist/ /usr/share/nginx/html/

EXPOSE 8090

# Use the underlying ENTRYPOINT and CMD provided by the base NGINX image
#ENTRYPOINT /var/tmp/entrypoint.sh
