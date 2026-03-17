# MONGO DB Scripts Setup Guide

## 1. Upload Scripts Folder

Run this from **Windows PowerShell / Linux**:

``` powershell
scp -r "$HOME\Orcid\orcid-member-services\assertion-service-2\scripts" <username>@<server>:/home/<username>/
```

``` unix
scp -r ~/Orcid/orcid-member-services/assertion-service-2/scripts <username>@<server>:/home/<username>/
```
------------------------------------------------------------------------


## 2. Connect to server & verify Upload on Server

``` bash
ssh <username>@<server>
```

``` bash
ls /home/<username>/scripts
```

------------------------------------------------------------------------

## 3. Copy Scripts Folder into Docker Container

``` bash
docker cp /home/<username>/scripts <assertion-docker>:/app/scripts
```

------------------------------------------------------------------------

## 4. Access Docker Container

``` bash
docker exec -it <assertion-docker> /bin/bash
```

Verify scripts inside container:

``` bash
ls /app/scripts
```

------------------------------------------------------------------------

## 5. Install Python and Dependencies (Inside Container)

### Update Packages

``` bash
apt update
```

### Install Python & Pip

``` bash
apt install -y python3 python3-pip
```

### Install Mongo Driver

``` bash
pip3 install pymongo
```

------------------------------------------------------------------------

## 6. Run Script(s)

### Test Connection

``` bash
cd /app/scripts
python3 test_connection.py
```
### Delete Documents

``` bash
cd /app/scripts/test-data
python3 delete_documents.py
```

------------------------------------------------------------------------

**Note:**\
- Changes made inside a running container are temporary.
- If these scripts need to be available regularly, add Python and required dependencies to the Docker image instead of installing them manually each time.
- If the container name changes between environments, always verify it with `docker ps` before running `docker cp` or `docker exec`.
