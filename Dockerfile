FROM quay.io/democracyworks/clojure-yourkit:lein-2.7.1

RUN mkdir -p /usr/src/batch-address-test-tool
WORKDIR /usr/src/batch-address-test-tool

COPY project.clj /usr/src/batch-address-test-tool/
RUN lein deps

COPY . /usr/src/batch-address-test-tool

RUN lein test
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" batch-address-test-tool-standalone.jar

CMD java -Xmx2g $YOURKIT_AGENT_OPTION -jar batch-address-test-tool-standalone.jar
