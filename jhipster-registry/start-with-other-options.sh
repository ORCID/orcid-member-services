JHIPSTER_URL=https://github.com/jhipster/jhipster-registry/releases/download/v6.8.0/jhipster-registry-6.8.0.jar
JHIPSTER_JAR=`basename $JHIPSTER_URL`

if [ ! -f $JHIPSTER_JAR ]; then
    echo "Attempting jhipster registry jar download from $JHIPSTER_URL..."
    if ! curl -f -L $JHIPSTER_URL --output $JHIPSTER_JAR
    then
        echo "Failed to automatically download jhipster registry jar. Please download $JHIPSTER_JAR from https://github.com/jhipster/jhipster-registry/releases and place in this directory"
        exit 1
    fi
fi

java -jar $JHIPSTER_JAR --spring.cloud.loadbalancer.retry.enabled=false --spring.profiles.active=dev,uaa --spring.cloud.loadbalancer.ribbon.enabled=false --spring.security.oauth2.client.provider.uaa.token-uri=http://userservice/oauth/token --spring.security.oauth2.client.registration.uaa.client-id=internal --spring.security.oauth2.client.registration.uaa.client-secret=internal --eureka.instance.hostname=localhost --eureka.client.register-with-eureka=false ----eureka.client.fetch-registry=false --eureka.client.serviceUrl.defaultZone=http://admin:admin@localhost:8761/eureka --spring.security.user.password=admin --jhipster.security.authentication.jwt.base-64-secret=bXktc2VjcmV0LWtleS13aGljaC1zaG91bGQtYmUtY2hhbmdlZC1pbi1wcm9kdWN0aW9uLWFuZC1iZS1iYXNlNjQtZW5jb2RlZA== --spring.cloud.config.server.composite.0.type=native

