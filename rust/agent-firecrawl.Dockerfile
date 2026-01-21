ARG RUST_VERSION=1.89
FROM rust:${RUST_VERSION}-bullseye AS builder
WORKDIR /app

COPY agent-firecrawl agent-firecrawl

# Ssl required for building
RUN apt-get update
RUN apt-get install -y libssl-dev

RUN cargo install --path agent-firecrawl

FROM debian:bullseye-slim
WORKDIR /app

# Ssl also required for running...
RUN apt update && apt install -y libssl-dev ca-certificates

COPY --from=builder /usr/local/cargo/bin/agent-firecrawl /usr/local/bin/agent-firecrawl

CMD ["agent-firecrawl"]
