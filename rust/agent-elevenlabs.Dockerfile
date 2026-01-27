ARG RUST_VERSION=1.89
FROM rust:${RUST_VERSION}-bullseye AS builder
WORKDIR /app

COPY agent-replicate agent-replicate

# Ssl required for building
RUN apt-get update
RUN apt-get install -y libssl-dev

RUN cargo install --path agent-replicate

FROM debian:bullseye-slim
WORKDIR /app

COPY --from=builder /app/agent-replicate/mcp mcp

ARG NODE_VERSION=22
ENV NVM_DIR=/root/.nvm

# Ssl also required for running...
RUN apt update && apt install -y libssl-dev curl ca-certificates

# nvm/npm/node https://github.com/nvm-sh/nvm?tab=readme-ov-file#installing-in-docker
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.3/install.sh | bash
RUN bash -c "source $NVM_DIR/nvm.sh && nvm install $NODE_VERSION && npm --prefix mcp install"

RUN echo "#!/bin/bash" >> run.sh && \
    echo "source $NVM_DIR/nvm.sh" >> run.sh && \
    echo "node /app/mcp/node_modules/replicate-mcp/index.js" >> run.sh && \
    chmod +x run.sh

COPY --from=builder /usr/local/cargo/bin/agent-replicate /usr/local/bin/agent-replicate

CMD ["agent-replicate"]
