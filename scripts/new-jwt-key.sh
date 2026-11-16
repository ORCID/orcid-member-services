#!/usr/bin/env bash

# script to create a jwt key passed in as JWT_PUBLIC_KEY JWT_PRIVATE_KEY
label="${1:-qa}"

openssl genrsa -out "mserv-${label}-jwt.key" 2048
openssl rsa -in "mserv-${label}-jwt.key" -pubout -out "mserv-${label}-jwt.pem"
