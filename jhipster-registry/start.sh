if [ ! -f jhipster-registry-6.1.2.jar ]; then
    echo "Download jhipster-registry-6.1.2.jar - https://github.com/jhipster/jhipster-registry/releases - and place in this directory"
fi

java -jar jhipster-registry-6.1.2.jar --spring.security.user.password=admin --jhipster.security.authentication.jwt.secret=my-secret-key-which-should-be-changed-in-production-and-be-base64-encoded --spring.cloud.config.server.composite.0.type=native --spring.cloud.config.server.composite.0.search-locations=file:./central-config

