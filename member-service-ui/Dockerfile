FROM node:20.7.0-bullseye-slim

# Create app directory
WORKDIR /app

# Install some useful utils for debug/test
RUN \
    apt-get update && \
    apt -y install procps iproute2 net-tools curl iputils-ping && \
    rm -rf /var/lib/apt/lists/*

# Install app dependencies
# A wildcard is used to ensure both package.json AND package-lock.json are copied
# where available (npm@5+)
COPY package*.json ./
COPY container-files/* /var/tmp/  

RUN \
    chmod +x /var/tmp/* && \
    npm install && \
    npm run build

# If you are building your code for production
# RUN npm install --only=production

# Bundle app source
COPY . .

EXPOSE 4200

ENTRYPOINT /var/tmp/entrypoint.sh
