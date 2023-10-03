FROM node:20.7.0-bullseye-slim

# Create app directory
WORKDIR /app

RUN apt-get update

# Install some useful utils for debug/test
RUN apt -y install procps iproute2 net-tools curl iputils-ping

RUN rm -rf /var/lib/apt/lists/*

# Install app dependencies
# A wildcard is used to ensure both package.json AND package-lock.json are copied
# where available (npm@5+)
COPY package*.json ./

COPY container-files/* /var/tmp/  

RUN chmod +x /var/tmp/*

RUN npm install \
    npm build

# If you are building your code for production
# RUN npm install --only=production

# Bundle app source
COPY . .

EXPOSE 4200

ENTRYPOINT /var/tmp/entrypoint.sh
