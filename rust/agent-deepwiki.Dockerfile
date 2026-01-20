ARG RUST_VERSION=1.89
FROM rust:${RUST_VERSION}-bullseye AS builder
WORKDIR /app

COPY agent-deepwiki agent-deepwiki

# Ssl required for building
RUN apt-get update
RUN apt-get install -y libssl-dev

RUN cargo install --path agent-deepwiki

FROM debian:bullseye-slim
WORKDIR /app

# Ssl also required for running...
RUN apt update && apt install -y libssl-dev ca-certificates

COPY --from=builder /usr/local/cargo/bin/agent-deepwiki /usr/local/bin/agent-deepwiki

CMD ["agent-deepwiki"]
