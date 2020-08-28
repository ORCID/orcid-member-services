JHIPSTER_URL=https://github.com/jhipster/jhipster-registry/releases/download/v6.1.2/jhipster-registry-6.1.2.jar
JHIPSTER_JAR=`basename $JHIPSTER_URL`

if [ ! -f $JHIPSTER_JAR ]; then
    if ! curl -f -L $JHIPSTER_URL --output $JHIPSTER_JAR
    then
        echo "Download $JHIPSTER_JAR - https://github.com/jhipster/jhipster-registry/releases - and place in this directory"
        exit 1
    fi
fi

java -jar $JHIPSTER_JAR --spring.security.user.password=admin --jhipster.security.authentication.jwt.secret=my-secret-key-which-should-be-changed-in-production-and-be-base64-encoded --spring.cloud.config.server.composite.0.type=native --spring.cloud.config.server.composite.0.search-locations=file:./central-config

