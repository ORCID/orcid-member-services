# Assertion Service -- Scripts Setup Guide

## 1. Access Server

``` bash
ssh dpalafox@mserv-qa-use2-a1.qa.int.orcid.org
```

------------------------------------------------------------------------

## 2. Create `scripts` Folder

``` bash
mkdir scripts
```

------------------------------------------------------------------------

## 3. Upload Scripts Folder

Run this from **Windows PowerShell / Linux**:

``` powershell
scp -r C:\Users\dp852\Orcid\orcid-member-services\assertion-service\scripts\query-fixes dpalafox@mserv-qa-use2-a1.qa.int.orcid.org:/home/dpalafox/scripts/
```

------------------------------------------------------------------------

## 4. Connect to server & verify Upload on Server

``` bash
ls /home/dpalafox/scripts/query-fixes
```

------------------------------------------------------------------------

## 5. Copy Scripts Folder into Docker Container

``` bash
docker cp /home/dpalafox/scripts/query-fixes member_services-assertionservice-app-1:/app/scripts
```

------------------------------------------------------------------------

## 6. Access Docker Container

``` bash
docker exec -it member_services-assertionservice-app-1 /bin/bash
```

Verify scripts inside container:

``` bash
ls /app/scripts
```

------------------------------------------------------------------------

## 7. Install Python and Dependencies (Inside Container)

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

## 8. Run Script(s)

``` bash
cd /app/scripts
python3 test_connection.py
```

------------------------------------------------------------------------

**Note:**\
Changes inside a container are temporary. For permanent setup, add
Python and dependencies to the Dockerfile.
