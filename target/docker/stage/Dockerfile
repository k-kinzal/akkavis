FROM registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift as stage0
WORKDIR /opt/docker
COPY opt /opt
USER root
RUN ["chmod", "-R", "u=rX,g=rX", "/opt/docker"]
RUN ["chmod", "u+x,g+x", "/opt/docker/bin/akka-cluster-sharding-visualizer"]
RUN ["chmod", "u+x,g+x", "/opt/docker/bin/runner"]

FROM registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift
USER root
RUN id -u demiourgos728 2> /dev/null || (( getent group 0 || groupadd -g 0 root ) && useradd --system --create-home --uid 1001 --gid 0 demiourgos728 )
WORKDIR /opt/docker
COPY --from=stage0 --chown=demiourgos728:root /opt/docker /opt/docker
EXPOSE 2551 8558 8080
USER 1001:0
ENTRYPOINT ["/opt/docker/bin/akka-cluster-sharding-visualizer"]
CMD []
