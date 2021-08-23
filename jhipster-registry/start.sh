JHIPSTER_URL=https://github.com/jhipster/jhipster-registry/releases/download/v6.1.2/jhipster-registry-6.1.2.jar
JHIPSTER_JAR=`basename $JHIPSTER_URL`

if [ ! -f $JHIPSTER_JAR ]; then
    echo "Attempting jhipster registry jar download from $JHIPSTER_URL..."
    if ! curl -f -L $JHIPSTER_URL --output $JHIPSTER_JAR
    then
        echo "Failed to automatically download jhipster registry jar. Please download $JHIPSTER_JAR from https://github.com/jhipster/jhipster-registry/releases and place in this directory"
        exit 1
    fi
fi

java -jar $JHIPSTER_JAR --spring.profiles.active=dev,uaa --spring.security.user.password=admin --jhipster.security.authentication.jwt.base-64-secret=bXktc2VjcmV0LWtleS13aGljaC1zaG91bGQtYmUtY2hhbmdlZC1pbi1wcm9kdWN0aW9uLWFuZC1iZS1iYXNlNjQtZW5jb2RlZA== --spring.cloud.config.server.composite.0.type=native

