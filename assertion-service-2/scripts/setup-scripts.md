# MONGO DB Scripts Setup Guide

## 1. Upload Scripts Folder

Run this from **Windows PowerShell / Linux**:

``` powershell
scp -r "$HOME\Orcid\orcid-member-services\assertion-service\scripts" <username>@mserv-qa-use2-a1.qa.int.orcid.org:/home/<username>/
```

``` unix
scp -r ~/Orcid/orcid-member-services/assertion-service/scripts <username>@mserv-qa-use2-a1.qa.int.orcid.org:/home/<username>/
```
------------------------------------------------------------------------


## 2. Connect to server & verify Upload on Server

``` bash
ssh <username>@mserv-qa-use2-a1.qa.int.orcid.org
```

``` bash
ls /home/<username>/scripts/query-fixes
```

------------------------------------------------------------------------

## 3. Copy Scripts Folder into Docker Container

``` bash
docker cp /home/<username>/scripts/query-fixes member_services-assertionservice-app-1:/app/scripts
```

------------------------------------------------------------------------

## 4. Access Docker Container

``` bash
docker exec -it member_services-assertionservice-app-1 /bin/bash
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

``` bash
cd /app/scripts/query-fixes
python3 cleanup.py
```

------------------------------------------------------------------------

**Note:**\
- Changes made inside a running container are temporary.
- If these scripts need to be available regularly, add Python and required dependencies to the Docker image instead of installing them manually each time.
- If the container name changes between environments, always verify it with `docker ps` before running `docker cp` or `docker exec`.
