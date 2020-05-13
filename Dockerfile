# syntax=docker/dockerfile:experimental
FROM hseeberger/scala-sbt:8u252_1.3.10_2.13.2 as builder

WORKDIR /scala

COPY . /scala
RUN --mount=type=cache,target=/root/.cache \
    --mount=type=cache,target=/root/.ivy2 \
    --mount=type=cache,target=/scala/project/project \
    --mount=type=cache,target=/scala/project/target \
    --mount=type=cache,target=/scala/target \
    sbt pack  \
    && mkdir -p dist \
    && cp -R /scala/target/pack/bin dist/bin \
    && cp -R /scala/target/pack/lib dist/lib


FROM openjdk:8u252

WORKDIR /opt/docker

COPY --from=builder /scala/dist/bin /opt/docker/bin
COPY --from=builder /scala/dist/lib /opt/docker/lib

ENTRYPOINT ["/opt/docker/bin/akkavis"]
