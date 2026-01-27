ARG RUST_VERSION=1.89
FROM rust:${RUST_VERSION}-trixie AS builder
WORKDIR /app

COPY agent-elevenlabs agent-elevenlabs

# Ssl required for building
RUN apt-get update
RUN apt-get install -y libssl-dev

RUN cargo install --path agent-elevenlabs

FROM ghcr.io/astral-sh/uv:trixie-slim
WORKDIR /app

# Ssl also required for running...
RUN apt update && apt install -y libssl-dev curl ca-certificates

COPY --from=builder /usr/local/cargo/bin/agent-elevenlabs /usr/local/bin/agent-elevenlabs

CMD ["agent-elevenlabs"]
